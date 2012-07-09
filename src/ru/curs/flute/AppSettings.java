package ru.curs.flute;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Класс, хранящий параметры приложения. Разбирает .properties-файл.
 */
public final class AppSettings {

	private static AppSettings theSettings;

	private final int threadsNumber;
	private final File scriptsPath;
	private final int queryPeriod;
	private final String dbClassName;
	private final String databaseConnection;
	private final String tableName;
	private final Logger logger;
	private final boolean neverStop;
	{
		logger = Logger.getLogger("ru.curs.flute");
		logger.setLevel(Level.INFO);
	}

	private AppSettings(File f) throws EFluteCritical {
		Properties settings = new Properties();
		try {
			FileInputStream in = new FileInputStream(f);
			settings.load(in);
		} catch (IOException e) {
			throw new EFluteCritical("IOException: " + e.getMessage());
		}

		StringBuffer sb = new StringBuffer();
		// Читаем настройки и проверяем их, где можно.
		int threadsNumber = 0;
		try {
			threadsNumber = Integer.parseInt(settings.getProperty(
					"threads.number", "1"));
			if (threadsNumber < 1)
				sb.append("Invalid number of execution threads (threads.number): "
						+ threadsNumber + '\n');
		} catch (NumberFormatException e) {
			sb.append("Invalid number of execution threads (threads.number): "
					+ settings.getProperty("threads.number") + '\n');
		}
		this.threadsNumber = threadsNumber;

		File sp = new File(settings.getProperty("scripts.path", ""));
		if (!sp.isDirectory())
			sp = new File(f.getParent() + File.separator + "scripts");
		if (sp.isDirectory() && sp.exists())
			scriptsPath = sp;
		else {
			scriptsPath = null;
			sb.append("Invalid path to Python scripts (scripts.path): " + sp
					+ '\n');
		}
		int queryPeriod = 0;
		try {
			queryPeriod = Integer.parseInt(settings.getProperty("query.period",
					"3000"));
			if (queryPeriod < 0)
				sb.append("Invalid value of query period in milliseconds (query.period): "
						+ queryPeriod + '\n');
		} catch (NumberFormatException e) {
			sb.append("Invalid value of query period in milliseconds (query.period): "
					+ settings.getProperty("query.period") + '\n');
		}
		this.queryPeriod = queryPeriod;

		dbClassName = settings.getProperty("database.classname", "");
		if (dbClassName.isEmpty())
			sb.append("No JDBC driver class name given (database.classname).\n");

		databaseConnection = settings.getProperty("database.connection", "");
		if (databaseConnection.isEmpty())
			sb.append("No JDBC URL given (database.connection).\n");

		tableName = settings.getProperty("table.name", "");
		if (tableName.isEmpty())
			sb.append("No tasks table name given (table.name).\n");

		String lf = settings.getProperty("log.file");

		if (lf != null)
			try {
				FileHandler fh = new FileHandler(lf, true);
				fh.setFormatter(new SimpleFormatter());
				logger.addHandler(fh);
			} catch (IOException e) {
				sb.append("Could not access or create log file " + lf + '\n');
			}

		if (sb.length() > 0)
			throw new EFluteCritical(sb.toString());

		neverStop = Boolean.parseBoolean(settings.getProperty("never.stop",
				"false"));
	}

	static void init(File f) throws EFluteCritical {
		theSettings = new AppSettings(f);
	}

	/**
	 * Значение параметра "Количество одновременных потоков приложения".
	 */
	public static int getThreadNumber() {
		return theSettings.threadsNumber;
	}

	/**
	 * Значение параметра "Путь к папке, в которой лежат xlsx/xlsm-шаблоны".
	 */
	public static File getScriptsPath() {
		return theSettings.scriptsPath;
	}

	/**
	 * Значение параметра "Время (в миллисекундах), на которое должен заснуть
	 * поток, не получивший ни одного задания на обработку.
	 */
	public static int getQueryPeriod() {
		return theSettings.queryPeriod;
	}

	/**
	 * Значение параметра "Класс JDBC-подключения".
	 */
	public static String getDbClassName() {
		return theSettings.dbClassName;
	}

	/**
	 * Значение параметра "Строка JDBC-подключения".
	 */
	public static String getDatabaseConnection() {
		return theSettings.databaseConnection;
	}

	/**
	 * Значение параметра "Имя таблицы отчетов".
	 */
	public static String getTableName() {
		return theSettings.tableName;
	}

	/**
	 * Значение параметра "Файл лога".
	 */
	public static Logger getLogger() {
		return theSettings.logger;
	}

	/**
	 * Значение параметра "не останавливаться на критических ошибках".
	 */
	public static boolean neverStop() {
		return theSettings.neverStop;
	}

	/**
	 * Тип базы данных.
	 * 
	 */
	public enum DBType {
		/**
		 * Postgre.
		 */
		POSTGRES, /**
		 * MS SQL.
		 */
		MSSQL, /**
		 * Неизвестный тип.
		 */
		UNKNOWN
	}

	/**
	 * Возвращает тип базы данных на основе JDBC-строки подключения.
	 */
	public static DBType getDBType() {
		if (theSettings.databaseConnection.startsWith("jdbc:sqlserver"))
			return DBType.MSSQL;
		else if (theSettings.databaseConnection.startsWith("jdbc:postgresql"))
			return DBType.POSTGRES;
		else
			return DBType.UNKNOWN;
	}
}
