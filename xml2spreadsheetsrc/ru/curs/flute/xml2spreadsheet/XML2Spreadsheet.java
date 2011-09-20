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
