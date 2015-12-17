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
	private final int queryPeriod;
	private final String tableName;
	private final String fluteUserId;
	private final Logger logger;
	private final boolean neverStop;
	private final int errorTextMaxLength;

	private final String cronTableName;
	private final boolean cronMode;

	{
		logger = Logger.getLogger("ru.curs.flute");
		logger.setLevel(Level.INFO);
	}

	private AppSettings(File f, Properties settings) throws EFluteCritical {

		StringBuilder sb = new StringBuilder();
		// Читаем настройки и проверяем их, где можно.
		int tn = 0;
		try {
			tn = Integer.parseInt(settings.getProperty("threads.number", "1").trim());
			if (tn < 1)
				sb.append("Invalid number of execution threads (threads.number): " + tn + '\n');
		} catch (NumberFormatException e) {
			sb.append("Invalid number of execution threads (threads.number): " + settings.getProperty("threads.number")
					+ '\n');
		}
		this.threadsNumber = tn;

		int qp = 0;
		try {
			qp = Integer.parseInt(settings.getProperty("query.period", "3000").trim());
			if (qp < 0)
				sb.append("Invalid value of query period in milliseconds (query.period): " + qp + '\n');
		} catch (NumberFormatException e) {
			sb.append("Invalid value of query period in milliseconds (query.period): "
					+ settings.getProperty("query.period") + '\n');
		}
		this.queryPeriod = qp;

		tableName = readStringProperty(settings, sb, "table.name", "tasks table name");

		cronTableName = readStringProperty(settings, sb, "crontable.name", "cron schedule table name");

		fluteUserId = readStringProperty(settings, sb, "flute.userid", "flute user id");

		String lf = settings.getProperty("log.file", "").trim();
		if (!lf.isEmpty())
			try {
				FileHandler fh = new FileHandler(lf, true);
				fh.setFormatter(new SimpleFormatter());
				logger.addHandler(fh);
			} catch (IOException e) {
				sb.append("Could not access or create log file " + lf + '\n');
			}

		neverStop = Boolean.parseBoolean(settings.getProperty("never.stop", "false").trim());

		cronMode = Boolean.parseBoolean(settings.getProperty("cron.mode", "false").trim());

		int ml = 0;
		try {
			ml = Integer.parseInt(settings.getProperty("message.max.length", "0").trim());
			if (ml < 0)
				sb.append("Invalid value of maximum message length (message.max.length): " + qp + '\n');
		} catch (NumberFormatException e) {
			sb.append("Invalid value of maximum message length (message.max.length): "
					+ settings.getProperty("message.max.length") + '\n');
		}
		this.errorTextMaxLength = ml;

		if (sb.length() > 0)
			throw new EFluteCritical(sb.toString());
	}

	private String readStringProperty(Properties settings, StringBuilder sb, String propName, String propDescr) {
		String result = settings.getProperty(propName, "").trim();
		if (result.isEmpty())
			sb.append(String.format("No %s given (%s).\n", propDescr, propName));
		return result;
	}

	/**
	 * Инициализация по properties-файлу.
	 * 
	 * @param f
	 *            файл
	 * 
	 * @throws EFluteCritical
	 *             сбой
	 */
	public static Properties init(File f) throws EFluteCritical {
		Properties settings = new Properties();
		try {
			FileInputStream in = new FileInputStream(f);
			settings.load(in);
		} catch (IOException e) {
			throw new EFluteCritical("IOException: " + e.getMessage());
		}
		theSettings = new AppSettings(f, settings);
		return settings;
	}

	/**
	 * Значение параметра "Количество одновременных потоков приложения".
	 */
	public static int getThreadNumber() {
		return theSettings.threadsNumber;
	}

	/**
	 * Значение параметра "Время (в миллисекундах), на которое должен заснуть
	 * поток, не получивший ни одного задания на обработку.
	 */
	public static int getQueryPeriod() {
		return theSettings.queryPeriod;
	}

	/**
	 * Значение параметра "Имя таблицы отчетов".
	 */
	public static String getTableName() {
		return theSettings.tableName;
	}

	/**
	 * Значение параметра "Пользователь Flute".
	 */
	public static String getFluteUserId() {
		return theSettings.fluteUserId;
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
	 * Значение параметра "максимальная длина поля для сообщения об ошибке".
	 */
	public static int getErrorTextMaxLength() {
		return theSettings.errorTextMaxLength;
	}

	/**
	 * Работает ли Флейта в CRON-режиме.
	 */
	public static boolean isCronMode() {
		return theSettings.cronMode;
	}

	/**
	 * Возвращает имя таблицы с cron-заданиями.
	 */
	public static String getCronTableName() {
		return theSettings.cronTableName;
	}
}
