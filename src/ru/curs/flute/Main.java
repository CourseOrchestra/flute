package ru.curs.flute;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Запускаемый из консоли или из Apache Commons Service Runner класс приложения.
 * 
 */
public class Main {

	/**
	 * Точка запуска приложения из консоли.
	 * 
	 * @param args
	 *            аргументы.
	 */
	public static void main(String[] args) {
		startService();
	}

	/**
	 * Точка запуска приложения из Apache Commons Service Starter. Аргумент
	 * "start" (или отсутствие аргументов) запускает сервис, аргумент "stop"
	 * (или любой другой) --- останавливает.
	 * 
	 * @param args
	 *            Команды, передаваемые prunsrv в статический метод.
	 */
	public static void windowsService(String args[]) {
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
		} catch (EXLReporterCritical e) {
			// Ошибка возникла при финализации
			e.printStackTrace();
		} catch (SQLException e) {
			// Ошибка возникла при финализации
			e.printStackTrace();
		}
	}

	private static void startService() {
		// ДО ЭТОГО МОМЕНТА У НАС НЕТ ЛОГГЕРА
		// Сперва разбираемся с настроечным файлом: читаем его.
		String path = Main.class.getProtectionDomain().getCodeSource()
				.getLocation().getPath();
		File f = new File(path.replace("%20", " "));

		if (f.getAbsolutePath().endsWith(".jar"))
			f = new File(f.getParent() + File.separator
					+ "flute.properties");
		else
			f = new File(f.getAbsolutePath() + File.separator
					+ "flute.properties");

		System.out.println();

		if (!f.exists()) {
			System.out.println("File " + f + " cannot be found.");
			return;
		}

		try {
			AppSettings.init(f);
		} catch (EXLReporterCritical e) {
			System.out
					.println("The following problems occured while reading file "
							+ f + ":");
			System.out.print(e.getMessage());
			return;
		}
		// С ЭТОГО МОМЕНТА У НАС ЕСТЬ ЛОГГЕР
		// Затем готовим драйвер подключения к БД...
		try {
			Class.forName(AppSettings.getDbClassName());
		} catch (ClassNotFoundException e) {
			AppSettings.getLogger().log(
					Level.SEVERE,
					"Class " + AppSettings.getDbClassName()
							+ " (JDBC driver) not found.");
		}

		// Затем инициализируем код, призванный высвобождать задания при
		// закрытии приложения.
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				releaseTasks();
			}
		});

		// Если до сих пор ничего критического не произошло, запускаем в
		// бесконечном цикле опрос и раздачу заданий.
		try {
			TaskManager.execute();
		} catch (EXLReporterCritical e) {
			AppSettings.getLogger().log(
					Level.SEVERE,
					"The following critical problem stopped the process:\n"
							+ e.getMessage());
			return;
		}
	}

	private static void stopService() {
		TaskManager.stop();
		System.out.println("Service stopped on request.");
	}
}
