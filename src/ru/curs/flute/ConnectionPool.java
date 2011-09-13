package ru.curs.flute;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;

/**
 * Пул соединений с базой данных.
 * 
 */
public class ConnectionPool {
	private static final LinkedList<Connection> POOL = new LinkedList<Connection>();

	/**
	 * Извлекает соединение из пула.
	 * 
	 * @throws EFluteCritical
	 *             В случае, если новое соединение не удалось создать.
	 */
	public static synchronized Connection get() throws EFluteCritical {
		Connection c = POOL.poll();
		while (c != null) {
			try {
				if (c.isValid(1)) {
					return c;
				}
			} catch (SQLException e) {
			}
			c = POOL.poll();
		}
		try {
			return DriverManager.getConnection(AppSettings
					.getDatabaseConnection());
		} catch (SQLException e) {
			throw new EFluteCritical("Could not connect to "
					+ AppSettings.getDatabaseConnection() + "with error: "
					+ e.getMessage());
		}

	}

	/**
	 * Возвращает соединение в пул.
	 * 
	 * @param c
	 *            возвращаемое соединение.
	 */
	public static synchronized void putBack(Connection c) {
		// Вставляем только хорошие соединения...
		try {
			if (c != null && !c.isValid(1))
				POOL.add(c);
		} catch (SQLException e) {
		}
	}

}
