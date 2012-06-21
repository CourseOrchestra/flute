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

	private static void initCL(File lib, File pylib) {
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
		postProperties.setProperty("python.path", pylib.toString());

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
		initCL(new File(path + "lib"), new File(path + "pylib"));

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
}
