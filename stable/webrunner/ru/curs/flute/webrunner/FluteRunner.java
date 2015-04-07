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
package ru.curs.flute.webrunner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import ru.curs.celesta.Celesta;
import ru.curs.celesta.CelestaException;
import ru.curs.flute.AppSettings;
import ru.curs.flute.EFluteCritical;
import ru.curs.flute.Main;
import ru.curs.flute.TaskManager;

/**
 * Синглетон, держащий поток выполнения с Флейтой.
 */
public final class FluteRunner {
	/**
	 * Экземпляр-синглетон выполняльщика Флейты.
	 */
	public static final FluteRunner RUNNER = new FluteRunner();
	private final ConsoleStream msgBuffer = new ConsoleStream();
	private final BufferedWriter pw;
	private FluteThread thread = new FluteThread();
	private Logger fluteLogger = null;

	private FluteRunner() {
		OutputStreamWriter osw;
		try {
			osw = new OutputStreamWriter(msgBuffer, "utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			osw = null;
		}
		pw = new BufferedWriter(osw);
	}

	private void log(String msg) {
		try {
			pw.append(msg);
			pw.newLine();
			pw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Запускает Флейту, если она не была запущена.
	 */
	public synchronized void start() {
		switch (thread.getState()) {
		case NEW:
			thread.start();
			break;
		case TERMINATED:
			thread = new FluteThread();
			thread.start();
			break;
		default:
			break;
		}
	}

	/**
	 * Останавливает FluteRunner.
	 */
	public synchronized void stop() {
		TaskManager.stop();
	}

	/**
	 * Возвращает содержимое консоли.
	 */
	public synchronized String getConsole() {
		boolean started;
		switch (thread.getState()) {
		case NEW:
		case TERMINATED:
			started = false;
			break;
		default:
			started = true;
			break;
		}
		try {
			return (started ? "STATUS: STARTED" : "STATUS: STOPPED") + "\n"
					+ msgBuffer.asString("utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
	}

	private static String getMyPath() {
		String path = Main.class.getProtectionDomain().getCodeSource()
				.getLocation().getPath();
		File f = new File(path.replace("%20", " "));
		return f.getParent() + File.separator;
	}

	/**
	 * Высвобождает задания, выполняющиеся в текущий момент, в момент
	 * завершения.
	 */
	private static void releaseTasks() {
		try {
			Connection conn = ru.curs.celesta.ConnectionPool.get();
			PreparedStatement stmt = conn.prepareStatement(String.format(
					"UPDATE %s SET STATUS = 0 WHERE STATUS = 1",
					AppSettings.getTableName()));
			stmt.execute();
			conn.close();
		} catch (SQLException | CelestaException e) {
			// Ошибка возникла при финализации
			e.printStackTrace();
		}
	}

	/**
	 * Поток выполнения Флейты.
	 */
	private class FluteThread extends Thread {

		@Override
		public void run() {
			if (fluteLogger == null) {
				String path = getMyPath();
				// ДО ЭТОГО МОМЕНТА У НАС НЕТ ЛОГГЕРА
				// Сперва разбираемся с настроечным файлом: читаем его.
				File f = new File(path + "flute.properties");

				log("");
				if (!f.exists()) {
					log("File " + f + " cannot be found.");
					return;
				}

				Properties p = null;
				try {
					p = ru.curs.flute.AppSettings.init(f);
				} catch (EFluteCritical e) {
					log("The following problems occured while reading file "
							+ f + ":");
					log(e.getMessage());
					return;
				}

				// С ЭТОГО МОМЕНТА У НАС ЕСТЬ ЛОГГЕР
				fluteLogger = AppSettings.getLogger();
				final StreamHandler sh = new StreamHandler(msgBuffer,
						new SimpleFormatter());
				fluteLogger.addHandler(new Handler() {
					@Override
					public void publish(LogRecord record) {
						sh.publish(record);
						sh.flush();
					}

					@Override
					public void flush() {
						sh.flush();
					}

					@Override
					public void close() {
						sh.close();
					}

				});

				fluteLogger
						.log(Level.INFO,
								"Flute initilized successfully, initializing Celesta...");
				try {
					Celesta.initialize(p);
				} catch (CelestaException e) {
					log("The following problems occured while initializing Celesta:");
					log(e.getMessage());
					return;
				}

				fluteLogger
						.log(Level.INFO, "Celesta initialized successfully.");

				// Затем инициализируем код, призванный высвобождать задания при
				// закрытии приложения.
				Runtime.getRuntime().addShutdownHook(new Thread() {
					@Override
					public void run() {
						releaseTasks();
					}
				});
			} else {
				fluteLogger
						.log(Level.INFO,
								"Everything was already initialized, restarting task execution loop.");
			}

			while (true) {
				try {
					fluteLogger.log(Level.INFO, "Task execution loop started.");
					// Если до сих пор ничего критического не произошло,
					// запускаем в
					// бесконечном цикле опрос и раздачу заданий.
					TaskManager.execute();
					// Если выполнение завершилось штатно -- выходим
					fluteLogger.log(Level.INFO, "Task execution loop stopped.");
					break;
				} catch (EFluteCritical e) {
					fluteLogger.log(Level.SEVERE,
							"The following critical problem stopped the normal execution:\n"
									+ e.getMessage());
					if (!AppSettings.neverStop()) {
						AppSettings
								.getLogger()
								.log(Level.SEVERE,
										"never.stop=false, so flute service is now stopping and exiting.");
						return;
					} else {
						AppSettings
								.getLogger()
								.log(Level.INFO,
										String.format(
												"never.stop=true, so flute will try to start again in %d ms.",
												10 * AppSettings
														.getQueryPeriod()));
						// Выдерживаем хорошую паузу
						try {
							Thread.sleep(10 * AppSettings.getQueryPeriod());
						} catch (InterruptedException e1) {
							fluteLogger.log(Level.INFO,
									"Thread sleep interrupted.");
						}
						fluteLogger.log(Level.INFO,
								"Trying to revive the service...");
					}
				}
			}
		}

	}
}
