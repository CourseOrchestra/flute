package ru.curs.flute.xml2spreadsheet;

import java.io.InputStream;

import org.xml.sax.helpers.DefaultHandler;

/**
 * Класс, ответственный за чтение из XML-файла и перенаправление команд на вывод
 * в объект ReportWriter.
 */
abstract class XMLDataReader {

	private final ReportWriter writer;
	private final InputStream descriptor;

	XMLDataReader(ReportWriter writer, InputStream descriptor) {
		this.writer = writer;
		this.descriptor = descriptor;
	}

	/**
	 * Создаёт объект-читальщик исходных данных на основании предоставленных
	 * сведений.
	 * 
	 * @param xmlData
	 *            Поток с исходными данными.
	 * @param xmlDescriptor
	 *            Дескриптор отчёта.
	 * @param processingMode
	 *            Режим обработки (DOM или SAX).
	 * @param writer
	 *            Объект, осуществляющий вывод.
	 */
	static XMLDataReader createReader(InputStream xmlData,
			InputStream xmlDescriptor, ProcessingMode processingMode,
			ReportWriter writer) {
		// Сначала парсится дескриптор и строится его объектное представление.
		// TODO
		// Затем инстанцируется конкретная реализация (DOM или SAX) ридера
		switch (processingMode) {
		case DOM:
			return new DOMDataReader(xmlData, xmlDescriptor, writer);
		case SAX:
			return new SAXDataReader(xmlData, xmlDescriptor, writer);
		}
		// Это никогда не произойдёт.
		return null;
	}

	/**
	 * Осуществляет генерацию отчёта.
	 * 
	 * @throws XML2SpreadSheetError
	 *             В случае возникновения ошибок ввода-вывода или при
	 *             интерпретации данных, шаблона или дескриптора.
	 */
	final void process() throws XML2SpreadSheetError {
		// TODO
	}

	final ReportWriter getWriter() {
		return writer;
	}

	private class DescriptorParser extends DefaultHandler {

	}

}
