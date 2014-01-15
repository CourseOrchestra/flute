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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import org.python.core.codecs;
import org.python.util.PythonInterpreter;

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

		if ("start".equals(cmd))
			startService();
		else
			stopService();
	}

	/**
	 * Высвобождает задания, выполняющиеся в текущий момент, в момент
	 * завершения.
	 */
	private static void releaseTasks() {
		try {
			Connection conn = ConnectionPool.get();
			PreparedStatement stmt = conn.prepareStatement(String.format(
					"UPDATE %s SET STATUS = 0 WHERE STATUS = 1",
					AppSettings.getTableName()));
			stmt.execute();
		} catch (SQLException | EFluteCritical e) {
			// Ошибка возникла при финализации
			e.printStackTrace();
		}
	}

	private static String getMyPath() {
		String path = Main.class.getProtectionDomain().getCodeSource()
				.getLocation().getPath();
		File f = new File(path.replace("%20", " "));
		if (f.getAbsolutePath().toLowerCase().endsWith(".jar"))
			return f.getParent() + File.separator;
		else
			return f.getAbsolutePath() + File.separator;
	}

	private static void initCL(String path) {
		
		File lib = new File(path + "lib");
		
		// Construct the "class path" for this class loader
		Set<URL> set = new LinkedHashSet<URL>();

		if (lib.isDirectory() && lib.exists() && lib.canRead()) {
			String filenames[] = lib.list();
			for (String filename : filenames) {
				if (!filename.toLowerCase().endsWith(".jar"))
					continue;
				File file = new File(lib, filename);
				URL url;
				try {
					url = file.toURI().toURL();
					set.add(url);
				} catch (MalformedURLException e) {
					// This can't happen
					e.printStackTrace();
				}
			}
		}
		// Construct the class loader itself
		final URL[] array = set.toArray(new URL[set.size()]);
		ClassLoader classLoader = AccessController
				.doPrivileged(new PrivilegedAction<URLClassLoader>() {
					@Override
					public URLClassLoader run() {
						return new URLClassLoader(array);
					}
				});

		Thread.currentThread().setContextClassLoader(classLoader);

		String libfolder = lib.toString();
		Properties postProperties = new Properties();
		postProperties.setProperty("python.packages.directories",
				"java.ext.dirs,flute.lib");
		postProperties.setProperty("flute.lib", libfolder);
		postProperties.setProperty("python.path", path + "pylib" + ";" + path + "scripts");

		PythonInterpreter.initialize(System.getProperties(), postProperties,
				null);
		codecs.setDefaultEncoding("UTF-8");
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
		try {
			AppSettings.init(f);
		} catch (EFluteCritical e) {
			System.out
					.println("The following problems occured while reading file "
							+ f + ":");
			System.out.print(e.getMessage());
			System.exit(1);
		}

		// Инициализируем класслоадер с библиотеками
		initCL(path);

		// С ЭТОГО МОМЕНТА У НАС ЕСТЬ ЛОГГЕР
		// Затем готовим драйвер подключения к БД...
		try {
			Class.forName(AppSettings.getDbClassName());
		} catch (ClassNotFoundException e) {
			AppSettings.getLogger().log(
					Level.SEVERE,
					"Class " + AppSettings.getDbClassName()
							+ " (JDBC driver) not found.");
			System.exit(1);
		}

		// Затем инициализируем код, призванный высвобождать задания при
		// закрытии приложения.
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				releaseTasks();
			}
		});

		while (true)
			try {
				// Если до сих пор ничего критического не произошло, запускаем в
				// бесконечном цикле опрос и раздачу заданий.
				TaskManager.execute();
				// Если выполнение завершилось штатно -- выходим
				break;
			} catch (EFluteCritical e) {
				AppSettings.getLogger().log(
						Level.SEVERE,
						"The following critical problem stopped the process:\n"
								+ e.getMessage());
				if (!AppSettings.neverStop())
					System.exit(1);
				else {
					// Выдерживаем хорошую паузу
					try {
						Thread.sleep(10 * AppSettings.getQueryPeriod());
					} catch (InterruptedException e1) {
						// Do nothing
					}
					AppSettings.getLogger().log(Level.INFO,
							"Trying to revive the service...");
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
