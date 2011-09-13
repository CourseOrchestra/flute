package ru.curs.flute.fastxl;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Класс, работающий с файлом sharedStrings.xml.
 * 
 */
public class XLSharedStrings {

	private ArrayList<String> array = new ArrayList<String>();
	private HashMap<String, Integer> reversed = new HashMap<String, Integer>();

	XLSharedStrings(InputStream is) throws EFastXLRuntime {
		SharedStringsParser p = new SharedStringsParser();
		try {
			TransformerFactory.newInstance().newTransformer()
					.transform(new StreamSource(is), new SAXResult(p));
		} catch (Exception e) {
			if (p.getError() != null)
				throw p.getError();
			else
				throw new EFastXLRuntime(
						"Could not parse sharedStrings.xml: " + e.getMessage());
		}
	}

	/**
	 * Добавляет строку к строковому пулу.
	 * 
	 * @param s
	 *            Строка.
	 * @return Позиция строки в строковом пуле.
	 */
	public int appendString(String s) {
		Integer result = reversed.get(s);
		if (result == null) {
			// Индекс, который получит новая строка.
			result = array.size();
			array.add(s);
			reversed.put(s, result);
		}
		return result;
	}

	/**
	 * Возвращает строку из строкового пула.
	 * 
	 * @param position
	 *            Позиция строки в пуле.
	 * @return Строку из пула.
	 */
	public String getString(int position) {
		return array.get(position);
	}

	/**
	 * Возвращает длину массива строк в пуле.
	 */
	public int getCount() {
		return array.size();
	}

	/**
	 * Возвращает количество уникальных строк в пуле.
	 */
	public int getUniqueCount() {
		return reversed.size();
	}

	/**
	 * Созраняет XML в поток.
	 * 
	 * @param os
	 *            Поток, в который сохраняется xml.
	 * @throws EFastXLRuntime
	 *             Если что-то не получилось.
	 */
	public void saveXML(OutputStream os) throws EFastXLRuntime {
		try {
			XMLStreamWriter xmlWriter = XMLOutputFactory.newInstance()
					.createXMLStreamWriter(new OutputStreamWriter(os, "UTF-8"));
			xmlWriter.writeStartDocument("utf-8", "1.0");

			xmlWriter.writeStartElement("sst");
			xmlWriter
					.writeAttribute("xmlns",
							"http://schemas.openxmlformats.org/spreadsheetml/2006/main");
			xmlWriter.writeAttribute("count", String.valueOf(getCount()));
			xmlWriter.writeAttribute("uniqueCount",
					String.valueOf(getUniqueCount()));

			for (String value : array) {
				xmlWriter.writeStartElement("si");
				xmlWriter.writeStartElement("t");
				xmlWriter.writeCharacters(value);
				xmlWriter.writeEndElement();
				xmlWriter.writeEndElement();
			}

			// Финализируем документ
			xmlWriter.writeEndDocument();
			xmlWriter.flush();
		} catch (final Exception e) {
			throw new EFastXLRuntime(String.format(
					"Не удаётся сохранить sharedStrings.\n%s", e.getMessage()));
		}
	}

	private enum ParserState {
		INITIAL, SST, SI, T,
	}

	private class SharedStringsParser extends DefaultHandler {

		private ParserState state = ParserState.INITIAL;
		private StringBuilder text = new StringBuilder();
		private EFastXLRuntime e = null;

		public EFastXLRuntime getError() {
			return e;
		}

		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			// Читаем значение текста
			if (state == ParserState.T)
				text.append(ch, start, length);
		}

		@Override
		public void startElement(String uri, String localName, String name,
				Attributes atts) throws SAXException {

			switch (state) {
			case INITIAL:
				if ("sst".equals(localName)) {
					String count = atts.getValue("count");
					// Если файл содержит подсказки --- используем их, чтобы не
					// потом не перестраивать массив и хэш
					if (count != null)
						array = new ArrayList<String>(Integer.parseInt(count));
					count = atts.getValue("uniqueCount");
					if (count != null)
						reversed = new HashMap<String, Integer>(
								Integer.parseInt(count));
				}
			case SST:
				if ("si".equals(localName))
					state = ParserState.SI;
				break;
			case SI:
				if ("t".equals(localName)) {
					state = ParserState.T;
					text = new StringBuilder();
				}
			}
		}

		@Override
		public void endElement(String uri, String localName, String name)
				throws SAXException {
			switch (state) {
			case T:
				if ("t".equals(localName)) {
					// Запихиваем прочитанную строку в память
					String buf = text.toString();
					array.add(buf);
					reversed.put(buf, array.size() - 1);
					try {
						validateAddedString(array.size() - 1, buf);
					} catch (EFastXLRuntime e2) {
						e = e2;
					}
					state = ParserState.SI;
				}
				break;
			case SI:
				if ("si".equals(localName))
					state = ParserState.SST;
			}
		}
	}

	/**
	 * Вызывается в момент десериализации объекта. Наследники могут
	 * переопределить данный метод, чтобы выполнить тотальный поиск/индексацию
	 * по всему файлу.
	 * 
	 * @param index
	 *            индекс строки.
	 * @param value
	 *            значение строки.
	 * @throws EFastXLRuntime
	 *             Если строка ошибочна (т. е. явно содержит плейсхолдер, но у
	 *             плейсхолдера неверный синтаксис).
	 */
	protected void validateAddedString(int index, String value)
			throws EFastXLRuntime {

	}
}
