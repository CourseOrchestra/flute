package ru.curs.flute;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;

import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

/**
 * Менеджер, запускающий потоки выполнения, обрабатывающие задание.
 */
public class TaskManager {

	private static final TaskManager THE_MANAGER = new TaskManager();
	private static boolean stop;

	private int processCount = 0;

	private Connection mainConn;
	private PreparedStatement selectNextStmt;
	private PreparedStatement markNextStmt;

	private void initMainConn() throws EFluteCritical {
		try {
			if (mainConn == null || mainConn.isClosed()) {
				mainConn = ConnectionPool.get();
				selectNextStmt = mainConn
						.prepareStatement(String
								.format("SELECT TOP 1 ID, SCRIPT, PARAMETERS FROM %s WHERE STATUS = 0 ORDER BY ID",
										AppSettings.getTableName()));
				markNextStmt = mainConn.prepareStatement(String.format(
						"UPDATE %s SET STATUS = 1 WHERE ID = ?",
						AppSettings.getTableName()));
			}
		} catch (SQLException e) {
			throw new EFluteCritical(
					"Error during main connection initialization: "
							+ e.getMessage());
		}
	}

	private TaskParams getNextTask() throws EFluteCritical {
		initMainConn();
		try {
			ResultSet rs = selectNextStmt.executeQuery();
			if (rs.next()) {
				int id = rs.getInt(1);
				String template = rs.getString(2);
				SQLXML xml = rs.getSQLXML(3);
				Document doc = xml == null ? null : (Document) xml.getSource(
						DOMSource.class).getNode();
				markNextStmt.setInt(1, id);
				markNextStmt.execute();
				if (!mainConn.getAutoCommit())
					mainConn.commit();
				return new TaskParams(id, template, doc);
			} else
				return null;
		} catch (SQLException e) {
			throw new EFluteCritical(
					"Error during getting the next task data: "
							+ e.getMessage());
		}
	}

	private PythonProcessor getNextProcessor() {
		if (processCount < AppSettings.getThreadNumber()) {
			PythonProcessor p = new PythonProcessor() {
				@Override
				protected void finish() {
					synchronized (TaskManager.this) {
						processCount--;
						TaskManager.this.notifyAll();
					}
				}
			};
			processCount++;
			return p;
		} else
			return null;
	}

	/**
	 * Выполняет в бесконечном цикле опрос заданий и раздачу их
	 * потокам-исполнителям.
	 */
	private synchronized void internalExecute() throws EFluteCritical {
		while (true) {
			// Вынимаем следующего исполнителя
			PythonProcessor p = getNextProcessor();
			while (p == null) {
				try {
					wait();
				} catch (InterruptedException e) {
					// Do nothing
				}
				if (stop)
					return;
				p = getNextProcessor();
			}

			// Вынимаем следующее задание
			TaskParams currentTask = getNextTask();
			// Если задания нет, выдерживаем паузу и снова пытаемся вынуть.
			while (currentTask == null) {
				try {
					Thread.sleep(AppSettings.getQueryPeriod());
				} catch (InterruptedException e) {
					// Do nothing
				}
				if (stop)
					return;
				currentTask = getNextTask();
			}
			if (stop)
				return;
			p.setTask(currentTask);
			p.start();
		}
	}

	/**
	 * Выполняет в бесконечном цикле опрос заданий и раздачу их
	 * потокам-исполнителям.
	 */
	static void execute() throws EFluteCritical {
		stop = false;
		THE_MANAGER.internalExecute();
	}

	/**
	 * Останавливает сервис.
	 */
	static void stop() {
		/*
		 * Вообще говоря, это всё надо переписать поаккуратнее, чтобы тут
		 * дождаться, пока все кончат работу и т. д. Но сейчас с этим нет
		 * времени возиться, поэтому просто ждём 6 сек и прикрываем лавочку.
		 */

		// Блокируем раздачу новых заданий
		stop = true;
		// Ждём 6 сек.
		try {
			Thread.sleep(6000);
		} catch (InterruptedException e) {
		}
	}
}
