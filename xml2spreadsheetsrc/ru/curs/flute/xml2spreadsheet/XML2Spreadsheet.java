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
package ru.curs.flute.xml2spreadsheet;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.File;
import java.io.OutputStream;

/**
 * Основной класс построителя отчётов из XML данных в формате электронных
 * таблиц.
 * 
 */
public final class XML2Spreadsheet {

	/**
	 * Запускает построение отчётов на исходных данных. Перегруженная версия
	 * метода, работающая на потоках.
	 * 
	 * @param xmlData
	 *            Исходные данные.
	 * @param xmlDescriptor
	 *            Дескриптор, описывающий порядок итерации по исходным данным.
	 * @param template
	 *            Шаблон отчёта.
	 * @param outputType
	 *            Тип шаблона отчёта (OpenOffice, XLS, XLSX).
	 * @param useSAX
	 *            Режим процессинга (DOM или SAX).
	 * @param output
	 *            Поток, в который записывается результирующий отчёт.
	 * @throws XML2SpreadSheetError
	 *             в случае возникновения ошибок
	 */
	public static void process(InputStream xmlData, InputStream xmlDescriptor,
			InputStream template, OutputType outputType, boolean useSAX,
			OutputStream output) throws XML2SpreadSheetError {
		ReportWriter writer = ReportWriter.createWriter(template, outputType,
				output);
		XMLDataReader reader = XMLDataReader.createReader(xmlData,
				xmlDescriptor, useSAX, writer);
		reader.process();
	}

	/**
	 * Запускает построение отчётов на исходных данных. Перегруженная версия
	 * метода, работающая на файлах (для удобства использования из
	 * Python-скриптов).
	 * 
	 * @param xmlData
	 *            Исходные данные.
	 * @param xmlDescriptor
	 *            Дескриптор, описывающий порядок итерации по исходным данным.
	 * @param template
	 *            Шаблон отчёта. Тип шаблона отчёта определяется по расширению.
	 * @param useSAX
	 *            Режим процессинга (false, если DOM, или true, если SAX).
	 * @param output
	 *            Поток, в который записывается результирующий отчёт.
	 * @throws FileNotFoundException
	 *             в случае, если указанные файлы не существуют
	 * @throws XML2SpreadSheetError
	 *             в случае иных ошибок
	 */
	public static void process(InputStream xmlData, File xmlDescriptor,
			File template, boolean useSAX, OutputStream output)
			throws FileNotFoundException, XML2SpreadSheetError {
		InputStream descr = new FileInputStream(xmlDescriptor);
		InputStream templ = new FileInputStream(template);
		String buf = template.toString();
		int dotInd = buf.lastIndexOf('.');
		buf = (dotInd > 0 && dotInd < buf.length()) ? buf.substring(dotInd + 1)
				: null;
		OutputType outputType;
		if ("ods".equalsIgnoreCase(buf))
			outputType = OutputType.ODS;
		else if ("xls".equalsIgnoreCase(buf))
			outputType = OutputType.XLS;
		else if ("xlsx".equalsIgnoreCase(buf))
			outputType = OutputType.XLSX;
		else
			throw new XML2SpreadSheetError(
					"Cannot define output format, template has non-standard extention.");
		process(xmlData, descr, templ, outputType, useSAX, output);
	}
}
