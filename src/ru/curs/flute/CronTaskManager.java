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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import it.sauronsoftware.cron4j.Scheduler;
import it.sauronsoftware.cron4j.SchedulingPattern;
import it.sauronsoftware.cron4j.Task;
import it.sauronsoftware.cron4j.TaskExecutionContext;
import ru.curs.celesta.Celesta;
import ru.curs.celesta.CelestaException;
import ru.curs.celesta.ConnectionPool;

/**
 * Параметры запланированного задания.
 */
class CronTaskData {
	private final int id;
	private String schedule;
	private String script;
	private String scheduledId;

	public CronTaskData(int id, String schedule, String script) {
		super();
		this.id = id;
		this.schedule = schedule;
		this.script = script;
	}

	public int getId() {
		return id;
	}

	public String getScheduledId() {
		return scheduledId;
	}

	public void setScheduledId(String scheduledId) {
		this.scheduledId = scheduledId;
	}

	public String getSchedule() {
		return schedule;
	}

	public String getScript() {
		return script;
	}

	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	public void setScript(String script) {
		this.script = script;
	}
}

/**
 * Запланированное задание.
 */
class FluteCronTask extends Task {
	private final CronTaskData d;

	FluteCronTask(CronTaskData d) {
		this.d = d;
	}

	@Override
	public void execute(TaskExecutionContext executionContext) throws RuntimeException {
		try {
			String sesId = String.format("FLUTE%08X", (new Random()).nextInt());
			Celesta.getInstance().login(sesId, AppSettings.getFluteUserId());
			Celesta.getInstance().runPython(sesId, d.getScript(), d.getId(), executionContext);
			Celesta.getInstance().logout(sesId, false);
		} catch (CelestaException e) {
			throw new RuntimeException(String.format("Celesta error: %s", e.getMessage()));
		}
	}
}

/**
 * Менеджер, запускающий потоки выполнения, обрабатывающие задание.
 */
public final class CronTaskManager {

	static final int SHUTDOWN_TIME = 6000;
	private static final CronTaskManager THE_MANAGER = new CronTaskManager();
	private static final HashMap<Integer, CronTaskData> SCHEDULE = new HashMap<>();
	private static final Scheduler SCHEDULER = new Scheduler();

	private static boolean stop;

	private Connection mainConn;
	private PreparedStatement selectSchedule;

	private CronTaskManager() {

	}

	private void initMainConn() throws EFluteCritical {
		try {
			if (mainConn == null || mainConn.isClosed()) {
				mainConn = ConnectionPool.get();
				String sql = "SELECT ID, CRONSCHEDULE, SCRIPT FROM %s WHERE DISABLED = 0 ORDER BY ID";
				selectSchedule = mainConn.prepareStatement(String.format(sql, AppSettings.getCronTableName()));
			}
		} catch (SQLException | CelestaException e) {
			throw new EFluteCritical("Error during main connection initialization: " + e.getMessage());
		}
	}

	private void refreshSchedule() throws EFluteCritical {
		initMainConn();
		try {
			HashSet<Integer> actual = new HashSet<>(SCHEDULE.keySet());
			ResultSet rs = selectSchedule.executeQuery();
			while (rs.next()) {
				int taskid = rs.getInt(1);
				actual.remove(taskid);
				CronTaskData ctd = new CronTaskData(taskid, rs.getString(2), rs.getString(3));
				CronTaskData oldTd = SCHEDULE.get(ctd.getId());
				if (oldTd != null) {
					if (!oldTd.getSchedule().equals(ctd.getSchedule())) {
						// MODIFIED SCHEDULE: reschedule!
						SCHEDULER.reschedule(oldTd.getScheduledId(), new SchedulingPattern(ctd.getSchedule()));
						oldTd.setSchedule(ctd.getSchedule());
					}
					if (!oldTd.getScript().equals(ctd.getScript())) {
						oldTd.setScript(ctd.getScript());
					}
					// Еlse do nothing!

				} else {
					// NEW TASK: schedule!
					Task t = new FluteCronTask(ctd);
					String id = SCHEDULER.schedule(new SchedulingPattern(ctd.getSchedule()), t);
					ctd.setScheduledId(id);
					SCHEDULE.put(taskid, ctd);
				}
			}
			// Deschedule!
			for (int i : actual) {
				CronTaskData oldTd = SCHEDULE.remove(i);
				SCHEDULER.deschedule(oldTd.getScheduledId());
			}
		} catch (SQLException e) {
			throw new EFluteCritical("Error during getting tasks schedule: " + e.getMessage());
		}
		if (!SCHEDULER.isStarted())
			SCHEDULER.start();
	}

	/**
	 * Выполняет в бесконечном цикле опрос заданий и раздачу их
	 * потокам-исполнителям.
	 */
	private synchronized void internalExecute() throws EFluteCritical {
		while (true) {
			// Обновляем расписание
			refreshSchedule();
			// Выдерживаем паузу.
			if (stop) {
				if (SCHEDULER.isStarted())
					SCHEDULER.stop();
				return;
			}
			try {
				Thread.sleep(AppSettings.getQueryPeriod());
			} catch (InterruptedException e) {// CHECKSTYLE:OFF
				// Do nothing
				// CHECKSTYLE:ON
			}
			if (stop) {
				if (SCHEDULER.isStarted())
					SCHEDULER.stop();
				return;
			}
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
