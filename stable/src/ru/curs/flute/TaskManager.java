/*
   (с) 2013 ООО "КУРС-ИТ"  

   Этот файл — часть КУРС:Flute.
   
   КУРС:Flute — свободная программа: вы можете перераспространять ее и/или изменять
   ее на условиях Стандартной общественной лицензии GNU в том виде, в каком
   она была опубликована Фондом свободного программного обеспечения; либо
   версии 3 лицензии, либо (по вашему выбору) любой более поздней версии.

   Эта программа распространяется в надежде, что она будет полезной,
   но БЕЗО ВСЯКИХ ГАРАНТИЙ; даже без неявной гарантии ТОВАРНОГО ВИДА
   или ПРИГОДНОСТИ ДЛЯ ОПРЕДЕЛЕННЫХ ЦЕЛЕЙ. Подробнее см. в Стандартной
   общественной лицензии GNU.

   Вы должны были получить копию Стандартной общественной лицензии GNU
   вместе с этой программой. Если это не так, см. http://www.gnu.org/licenses/.

   
   Copyright 2013, COURSE-IT Ltd.

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see http://www.gnu.org/licenses/.

 */
package ru.curs.flute;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;

import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import ru.curs.celesta.CelestaException;
import ru.curs.celesta.ConnectionPool;

/**
 * Менеджер, запускающий потоки выполнения, обрабатывающие задание.
 */
public final class TaskManager {

	static final int SHUTDOWN_TIME = 6000;
	private static final TaskManager THE_MANAGER = new TaskManager();
	private static boolean stop;

	private int processCount = 0;

	private Connection mainConn;
	private PreparedStatement selectNextStmt;
	private PreparedStatement markNextStmt;

	private TaskManager() {

	}

	private void initMainConn() throws EFluteCritical {
		String sql;
		switch (ru.curs.celesta.AppSettings.getDBType()) {
		case MSSQL:
			sql = "SELECT TOP 1 ID, SCRIPT, PARAMETERS FROM %s WHERE STATUS = 0 ORDER BY ID";
			break;
		case POSTGRES:
		case MYSQL:
			sql = "SELECT ID, SCRIPT, PARAMETERS FROM %s WHERE STATUS = 0 ORDER BY ID LIMIT 1";
			break;
		case ORACLE:
			sql = "with a as (SELECT ID, SCRIPT, PARAMETERS FROM %s WHERE STATUS = 0 ORDER BY ID) "
					+ "select a.* from a where rownum <= 1";
			break;
		default:
			throw new EFluteCritical("Cannot recognize database type.");
		}

		try {
			if (mainConn == null || mainConn.isClosed()) {
				mainConn = ConnectionPool.get();
				selectNextStmt = mainConn.prepareStatement(String.format(sql,
						AppSettings.getTableName()));
				markNextStmt = mainConn.prepareStatement(String.format(
						"UPDATE %s SET STATUS = 1 WHERE ID = ?",
						AppSettings.getTableName()));
			}
		} catch (SQLException | CelestaException e) {
			throw new EFluteCritical(
					"Error during main connection initialization: "
							+ e.getMessage());
		}
	}

	private TaskParams getNextTask() throws EFluteCritical {
		initMainConn();
		try {
			ResultSet rs = selectNextStmt.executeQuery();
			try {
				if (rs.next()) {
					int id = rs.getInt(1);
					String template = rs.getString(2);
					Document doc = null;
					String str = null;
					try {
						SQLXML xml = rs.getSQLXML(3);
						doc = xml == null ? null : (Document) xml.getSource(
								DOMSource.class).getNode();
					} catch (SQLException e) {
						str = rs.getString(3);
					}
					markNextStmt.setInt(1, id);
					markNextStmt.execute();
					return new TaskParams(id, template, doc, str);
				} else {
					return null;
				}
			} finally {
				rs.close();
				ConnectionPool.commit(mainConn);
			}
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
		} else {
			return null;
		}
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
				} catch (InterruptedException e) {// CHECKSTYLE:OFF
					// Do nothing
					// CHECKSTYLE:ON
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
				} catch (InterruptedException e) {// CHECKSTYLE:OFF
					// Do nothing
					// CHECKSTYLE:ON
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
	 * 
	 * @throws EFluteCritical
	 *             сбой
	 */
	public static void execute() throws EFluteCritical {
		stop = false;
		THE_MANAGER.mainConn = null;
		THE_MANAGER.internalExecute();
	}

	/**
	 * Останавливает сервис.
	 */
	public static void stop() {
		/*
		 * Вообще говоря, это всё надо переписать поаккуратнее, чтобы тут
		 * дождаться, пока все кончат работу и т. д. Но сейчас с этим нет
		 * времени возиться, поэтому просто ждём 6 сек и прикрываем лавочку.
		 */

		// Блокируем раздачу новых заданий
		stop = true;

	}
}
