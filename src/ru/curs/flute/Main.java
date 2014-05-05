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

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;

import ru.curs.celesta.Celesta;
import ru.curs.celesta.CelestaException;

/**
 * Запускаемый из консоли или из Apache Commons Service Runner класс приложения.
 * 
 */
public final class Main {

	/**
	 * Точка запуска приложения из консоли.
	 * 
	 * @param args
	 *            аргументы.
	 */
	public static void main(String[] args) {
		String cmd = "start";
		if (args.length > 0)
			cmd = args[0];

		if ("start".equals(cmd)) {
			startService();
		} else {
			stopService();
		}
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

	private static String getMyPath() {
		String path = Main.class.getProtectionDomain().getCodeSource()
				.getLocation().getPath();
		File f = new File(path.replace("%20", " "));
		if (f.getAbsolutePath().toLowerCase().endsWith(".jar")) {
			return f.getParent() + File.separator;
		} else {
			return f.getAbsolutePath() + File.separator;
		}
	}

	private static void startService() {
		String path = getMyPath();
		// ДО ЭТОГО МОМЕНТА У НАС НЕТ ЛОГГЕРА
		// Сперва разбираемся с настроечным файлом: читаем его.
		File f = new File(path + "flute.properties");
		System.out.println();
		if (!f.exists()) {
			System.out.println("File " + f + " cannot be found.");
			System.exit(1);
		}

		Properties p = null;
		try {
			p = AppSettings.init(f);
		} catch (EFluteCritical e) {
			System.out
					.println("The following problems occured while reading file "
							+ f + ":");
			System.out.print(e.getMessage());
			System.exit(1);
		}

		try {
			Celesta.initialize(p);
		} catch (CelestaException e) {
			System.out
					.println("The following problems occured while initializing Celesta:");
			System.out.print(e.getMessage());
			System.exit(1);
		}

		// С ЭТОГО МОМЕНТА У НАС ЕСТЬ ЛОГГЕР
		AppSettings.getLogger().log(Level.INFO,
				"Celesta initialized successfully");
		
		// Затем инициализируем код, призванный высвобождать задания при
		// закрытии приложения.
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				releaseTasks();
			}
		});

		while (true) {
			try {
				// Если до сих пор ничего критического не произошло, запускаем в
				// бесконечном цикле опрос и раздачу заданий.
				TaskManager.execute();
				// Если выполнение завершилось штатно -- выходим
				break;
			} catch (EFluteCritical e) {
				AppSettings.getLogger().log(
						Level.SEVERE,
						"The following critical problem stopped the normal execution:\n"
								+ e.getMessage());
				if (!AppSettings.neverStop()) {
					AppSettings
							.getLogger()
							.log(Level.SEVERE,
									"never.stop=false, so flute service is now stopping and exiting.");
					System.exit(1);
				} else {
					AppSettings
							.getLogger()
							.log(Level.INFO,
									String.format(
											"never.stop=true, so flute will try to start again in %d ms.",
											10 * AppSettings.getQueryPeriod()));
					// Выдерживаем хорошую паузу
					try {
						Thread.sleep(10 * AppSettings.getQueryPeriod());
					} catch (InterruptedException e1) {
						AppSettings.getLogger().log(Level.INFO,
								"Thread sleep interrupted.");
					}
					AppSettings.getLogger().log(Level.INFO,
							"Trying to revive the service...");
				}

			}
		}
	}

	private static void stopService() {
		TaskManager.stop();
		System.out.println("Service stopped on request.");
	}

	/**
	 * init-метод Apache Commons Daemon: Here open configuration files, create a
	 * trace file, create ServerSockets, Threads.
	 * 
	 * @param arguments
	 *            параметры (в нашем случае игнорируются).
	 */
	public void init(String[] arguments) {

	}

	/**
	 * start-метод Apache Commons Daemon: Start the Thread, accept incoming
	 * connections.
	 */
	public void start() {
		System.err.println("Flute starting...");
		startService();
		System.err.println("Flute started.");
	}

	/**
	 * stop-метод Apache Commons Daemon: Inform the Thread to terminate the
	 * run(), close the ServerSockets.
	 */
	public void stop() {
		System.err.println("Flute stopping...");
		stopService();
		System.err.println("Flute stopped");
	}

	/**
	 * destroy-метод Apache Commons Daemon: Destroy any object created in
	 * init().
	 */
	public void destroy() {

	}
}
