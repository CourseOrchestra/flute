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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Пул соединений с базой данных.
 * 
 */
public final class ConnectionPool {
	private static final Queue<Connection> POOL = new LinkedList<>();

	private ConnectionPool() {

	}

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

			} catch (SQLException e) { // CHECKSTYLE:OFF
				// CHECKSTYLE:ON
			}

			c = POOL.poll();
		}
		try {
			return DriverManager.getConnection(AppSettings
					.getDatabaseConnection());
		} catch (SQLException e) {
			throw new EFluteCritical("Could not connect to "
					+ AppSettings.getDatabaseConnection() + " with error: "
					+ e.getMessage());
		}

	}

	/**
	 * Выполняет починку коннекшна.
	 * 
	 * @param c
	 *            Коннекшн на починку.
	 * @return Новый коннекшн или тот же самый, если он непуст и валиден.
	 * @throws EFluteCritical
	 *             В случае, если новое соединение не удалось создать.
	 */
	public static synchronized Connection repair(Connection c)
			throws EFluteCritical {
		try {
			if (c.isValid(1))
				return c;
		} catch (SQLException e) { // CHECKSTYLE:OFF
			// CHECKSTYLE:ON
		}
		return get();
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
			if (c != null && c.isValid(1))
				POOL.add(c);
		} catch (SQLException e) { // CHECKSTYLE:OFF
			// CHECKSTYLE:ON
		}
	}

}
