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

import org.w3c.dom.Document;

import ru.curs.celesta.dbutils.BLOB;


/**
 * Содержит в себе параметры задания для выполнения.
 * 
 */
public final class TaskParams {
	private final int id;
	private final String scriptName;
	private final Document params;
	private final String strParams;
	private final BLOB blob = new BLOB();

	TaskParams(int id, String scriptName, Document params, String str) {
		this.id = id;
		this.scriptName = scriptName;
		this.params = params;
		this.strParams = str;
	}

	/**
	 * Идентификатор задания.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Скрипт задания (имя .py-файла).
	 */
	public String getScriptName() {
		return scriptName;
	}

	/**
	 * Параметры задания.
	 */
	public Document getParams() {
		return params;
	}

	/**
	 * Параметры задания (в строковом формате).
	 */
	public String getStrParams() {
		return strParams;
	}

	/**
	 * Возвращает BLOB для записи данных.
	 */
	public BLOB getBLOB() {
		return blob;
	}

}
