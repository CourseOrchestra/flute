package ru.curs.flute.xml2spreadsheet;

import java.io.InputStream;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	private static final Pattern RANGE = Pattern
			.compile("(-?[0-9]+):(-?[0-9]+)");

	private final ReportWriter writer;
	private final DescriptorElement descriptor;

	XMLDataReader(ReportWriter writer, DescriptorElement descriptor) {
		this.writer = writer;
		this.descriptor = descriptor;
	}

	private enum ParserState {
		ELEMENT, ITERATION, OUTPUT
	}

	private static final class DescriptorParser extends DefaultHandler {

		private final Deque<DescriptorElement> elementsStack = new LinkedList<>();
		private DescriptorElement root;
		private ParserState parserState = ParserState.ITERATION;

		@Override
		public void startElement(String uri, String localName, String name,
				final Attributes atts) throws SAXException {

			abstract class AttrReader<T> {
				T getValue(String qName) throws XML2SpreadSheetError {
					String buf = atts.getValue(qName);
					if (buf == null || "".equals(buf))
						return getIfEmpty();
					else
						return getIfNotEmpty(buf);
				}

				abstract T getIfNotEmpty(String value)
						throws XML2SpreadSheetError;

				abstract T getIfEmpty();
			}

			final class StringAttrReader extends AttrReader<String> {
				@Override
				String getIfNotEmpty(String value) throws XML2SpreadSheetError {
					return value;
				}

				@Override
				String getIfEmpty() {
					return null;
				}
			}

			try {
				switch (parserState) {
				case ELEMENT:
					if ("iteration".equals(localName)) {
						int index = (new AttrReader<Integer>() {
							@Override
							Integer getIfNotEmpty(String value) {
								return Integer.parseInt(value);
							}

							@Override
							Integer getIfEmpty() {
								return -1;
							}
						}).getValue("index");

						boolean horizontal = (new AttrReader<Boolean>() {
							@Override
							Boolean getIfNotEmpty(String value) {
								return "horizontal".equalsIgnoreCase(value);
							}

							@Override
							Boolean getIfEmpty() {
								return false;
							}
						}).getValue("mode");

						DescriptorIteration currIteration = new DescriptorIteration(
								index, horizontal);
						elementsStack.peek().getSubelements()
								.add(currIteration);
						parserState = ParserState.ITERATION;
					} else if ("output".equals(localName)) {
						RangeAddress range = (new AttrReader<RangeAddress>() {
							@Override
							RangeAddress getIfNotEmpty(String value)
									throws XML2SpreadSheetError {
								return new RangeAddress(value);
							}

							@Override
							RangeAddress getIfEmpty() {
								return null;
							}
						}).getValue("range");
						StringAttrReader sar = new StringAttrReader();
						DescriptorOutput output = new DescriptorOutput(
								sar.getValue("worksheet"), range,
								sar.getValue("sourcesheet"),
								sar.getValue("repeatingcols"),
								sar.getValue("repeatingrows"));
						elementsStack.peek().getSubelements().add(output);

						parserState = ParserState.OUTPUT;
					}
					break;
				case ITERATION:
					if ("element".equals(localName)) {
						String elementName = (new StringAttrReader())
								.getValue("name");
						DescriptorElement currElement = new DescriptorElement(
								elementName);

						if (root == null)
							root = currElement;
						else {
							// Добываем контекст текущей итерации...
							List<DescriptorSubelement> subelements = elementsStack
									.peek().getSubelements();
							DescriptorIteration iter = (DescriptorIteration) subelements
									.get(subelements.size() - 1);
							iter.getElements().add(currElement);
						}
						elementsStack.push(currElement);
						parserState = ParserState.ELEMENT;
					}
					break;
				}
			} catch (XML2SpreadSheetError e) {
				throw new SAXException(e.getMessage());
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
		if (xmlData == null)
			throw new XML2SpreadSheetError("XML Data is null.");
		if (xmlDescriptor == null)
			throw new XML2SpreadSheetError("XML descriptor is null.");

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

	/**
	 * Общий для DOM и SAX реализации метод обработки вывода.
	 * 
	 * @param c
	 *            Контекст.
	 * @param o
	 *            Дескриптор секции.
	 * @throws XML2SpreadSheetError
	 *             В случае возникновения ошибок ввода-вывода или при
	 *             интерпретации шаблона.
	 */
	final void processOutput(XMLContext c, DescriptorOutput o)
			throws XML2SpreadSheetError {
		if (o.getWorksheet() != null) {
			String wsName = c.calc(o.getWorksheet());
			getWriter().sheet(wsName, o.getSourceSheet(),
					o.getStartRepeatingColumn(), o.getEndRepeatingColumn(),
					o.getStartRepeatingRow(), o.getEndRepeatingRow());
		}
		if (o.getRange() != null)
			getWriter().section(c, o.getSourceSheet(), o.getRange());
	}

	final boolean compareIndices(int expected, int actual) {
		return (expected < 0) || (actual == expected);
	}

	final boolean compareNames(String expected, String actual) {
		return "*".equals(expected)
				|| (expected != null && expected.equals(actual));
	}

	final ReportWriter getWriter() {
		return writer;
	}

	final DescriptorElement getDescriptor() {
		return descriptor;
	}

	static final class DescriptorElement {
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

	static final class DescriptorIteration extends DescriptorSubelement {
		private final int index;
		private final boolean horizontal;
		private final List<DescriptorElement> elements = new LinkedList<>();

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

	static final class DescriptorOutput extends DescriptorSubelement {
		private final String worksheet;
		private final RangeAddress range;
		private final String sourceSheet;
		private final int startRepeatingColumn;
		private final int endRepeatingColumn;
		private final int startRepeatingRow;
		private final int endRepeatingRow;

		public DescriptorOutput(String worksheet, RangeAddress range,
				String sourceSheet, String repeatingCols, String repeatingRows)
				throws XML2SpreadSheetError {
			this.worksheet = worksheet;
			this.range = range;
			this.sourceSheet = sourceSheet;

			Matcher m1 = RANGE.matcher(repeatingCols == null ? "-1:-1"
					: repeatingCols);
			Matcher m2 = RANGE.matcher(repeatingRows == null ? "-1:-1"
					: repeatingRows);
			if (m1.matches() && m2.matches()) {
				this.startRepeatingColumn = Integer.parseInt(m1.group(1));
				this.endRepeatingColumn = Integer.parseInt(m1.group(2));
				this.startRepeatingRow = Integer.parseInt(m2.group(1));
				this.endRepeatingRow = Integer.parseInt(m2.group(2));
			} else
				throw new XML2SpreadSheetError(String.format(
						"Invalid col/row range %s %s", repeatingCols,
						repeatingRows));

		}

		String getWorksheet() {
			return worksheet;
		}

		String getSourceSheet() {
			return sourceSheet;
		}

		RangeAddress getRange() {
			return range;
		}

		public int getStartRepeatingColumn() {
			return startRepeatingColumn;
		}

		public int getEndRepeatingColumn() {
			return endRepeatingColumn;
		}

		public int getStartRepeatingRow() {
			return startRepeatingRow;
		}

		public int getEndRepeatingRow() {
			return endRepeatingRow;
		}

	}

}
