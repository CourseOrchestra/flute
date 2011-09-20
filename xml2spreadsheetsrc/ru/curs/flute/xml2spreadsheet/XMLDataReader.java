package ru.curs.flute.xml2spreadsheet;

import java.io.InputStream;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Класс, ответственный за чтение из XML-файла и перенаправление команд на вывод
 * в объект ReportWriter.
 */
abstract class XMLDataReader {

	private final ReportWriter writer;
	private final DescriptorElement descriptor;

	XMLDataReader(ReportWriter writer, DescriptorElement descriptor) {
		this.writer = writer;
		this.descriptor = descriptor;
	}

	private enum ParserState {
		ELEMENT, ITERATION, OUTPUT
	}

	/**
	 * Создаёт объект-читальщик исходных данных на основании предоставленных
	 * сведений.
	 * 
	 * @param xmlData
	 *            Поток с исходными данными.
	 * @param xmlDescriptor
	 *            Дескриптор отчёта.
	 * @param useSAX
	 *            Режим обработки (DOM или SAX).
	 * @param writer
	 *            Объект, осуществляющий вывод.
	 * @throws XML2SpreadSheetError
	 *             В случае ошибки обработки дескриптора отчёта.
	 */
	static XMLDataReader createReader(InputStream xmlData,
			InputStream xmlDescriptor, boolean useSAX, ReportWriter writer)
			throws XML2SpreadSheetError {

		final class DescriptorParser extends DefaultHandler {

			private final Deque<DescriptorElement> elementsStack = new LinkedList<DescriptorElement>();
			private DescriptorElement root;
			private ParserState parserState = ParserState.ITERATION;

			@Override
			public void startElement(String uri, String localName, String name,
					Attributes atts) throws SAXException {
				String buf;
				switch (parserState) {
				case ELEMENT:
					if ("iteration".equals(localName)) {
						buf = atts.getValue("index");
						int index = (buf == null || "".equals(buf)) ? -1
								: Integer.parseInt(buf);
						buf = atts.getValue("mode");
						boolean horizontal = "horizontal".equalsIgnoreCase(buf);
						DescriptorIteration currIteration = new DescriptorIteration(
								index, horizontal);
						elementsStack.peek().getSubelements()
								.add(currIteration);
						parserState = ParserState.ITERATION;
					} else if ("output".equals(localName)) {
						buf = atts.getValue("range");
						RangeAddress range = (buf == null || "".equals(buf)) ? null
								: new RangeAddress(buf);
						buf = atts.getValue("worksheet");
						String worksheet = (buf == null || "".equals(buf)) ? null
								: buf;
						DescriptorOutput output = new DescriptorOutput(
								worksheet, range);
						elementsStack.peek().getSubelements().add(output);
						parserState = ParserState.OUTPUT;
					}
					break;
				case ITERATION:
					if ("element".equals(localName)) {
						buf = atts.getValue("name");
						DescriptorElement currElement = new DescriptorElement(
								buf);
						// Добываем контекст текущей итерации...
						List<DescriptorSubelement> subelements = elementsStack
								.peek().getSubelements();
						DescriptorIteration iter = (DescriptorIteration) subelements
								.get(subelements.size() - 1);
						iter.getElements().add(currElement);
						elementsStack.push(currElement);

						if (root == null)
							root = currElement;

						parserState = ParserState.ELEMENT;
					}
					break;
				}

			}

			@Override
			public void endElement(String uri, String localName, String name)
					throws SAXException {
				switch (parserState) {
				case ELEMENT:
					elementsStack.pop();
					parserState = ParserState.ITERATION;
					break;
				case ITERATION:
					parserState = ParserState.ELEMENT;
					break;
				case OUTPUT:
					parserState = ParserState.ELEMENT;
					break;
				}
			}

		}

		// Сначала парсится дескриптор и строится его объектное представление.
		DescriptorParser parser = new DescriptorParser();
		try {
			TransformerFactory
					.newInstance()
					.newTransformer()
					.transform(new StreamSource(xmlDescriptor),
							new SAXResult(parser));
		} catch (Exception e) {
			throw new XML2SpreadSheetError(
					"Error while processing XML descriptor: " + e.getMessage());
		}
		// Затем инстанцируется конкретная реализация (DOM или SAX) ридера
		if (useSAX)
			return new SAXDataReader(xmlData, parser.root, writer);
		else
			return new DOMDataReader(xmlData, parser.root, writer);
	}

	/**
	 * Осуществляет генерацию отчёта.
	 * 
	 * @throws XML2SpreadSheetError
	 *             В случае возникновения ошибок ввода-вывода или при
	 *             интерпретации данных, шаблона или дескриптора.
	 */
	abstract void process() throws XML2SpreadSheetError;

	final ReportWriter getWriter() {
		return writer;
	}

	final DescriptorElement getDescriptor() {
		return descriptor;
	}

	static class DescriptorElement {
		private final String elementName;
		private final List<DescriptorSubelement> subelements = new LinkedList<DescriptorSubelement>();

		public DescriptorElement(String elementName) {
			this.elementName = elementName;
		}

		String getElementName() {
			return elementName;
		}

		List<DescriptorSubelement> getSubelements() {
			return subelements;
		}
	}

	abstract static class DescriptorSubelement {
	}

	static class DescriptorIteration extends DescriptorSubelement {
		private final int index;
		private final boolean horizontal;
		private final List<DescriptorElement> elements = new LinkedList<DescriptorElement>();

		public DescriptorIteration(int index, boolean horizontal) {
			this.index = index;
			this.horizontal = horizontal;
		}

		int getIndex() {
			return index;
		}

		boolean isHorizontal() {
			return horizontal;
		}

		List<DescriptorElement> getElements() {
			return elements;
		}
	}

	static class DescriptorOutput extends DescriptorSubelement {
		private final String worksheet;
		private final RangeAddress range;

		public DescriptorOutput(String worksheet, RangeAddress range) {
			this.worksheet = worksheet;
			this.range = range;
		}

		String getWorksheet() {
			return worksheet;
		}

		RangeAddress getRange() {
			return range;
		}
	}

}
