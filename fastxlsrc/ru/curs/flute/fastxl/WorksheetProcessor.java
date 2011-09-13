package ru.curs.flute.fastxl;

import static java.sql.Types.DECIMAL;
import static java.sql.Types.DOUBLE;
import static java.sql.Types.FLOAT;
import static java.sql.Types.INTEGER;
import static java.sql.Types.NUMERIC;
import static java.sql.Types.REAL;
import static java.sql.Types.SMALLINT;
import static java.sql.Types.TINYINT;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Обработчик worksheet-xml.
 * 
 */
public final class WorksheetProcessor {

	private final InputStream is;
	private final Connection conn;

	public WorksheetProcessor(InputStream is, Connection conn) {
		this.is = is;
		this.conn = conn;
	}

	/**
	 * Выполняет трансформацию листа Excel, вставляя вместо плейсхолдеров
	 * результаты выполнения хранимых процедур.
	 * 
	 * @param os
	 *            поток, в который записывается трансформированный лист.
	 * @param sharedStrings
	 *            пул строк
	 * 
	 * @param placeholders
	 *            мэппинг между номерами строк и плейсхолдерами
	 * 
	 * @throws EXLReporterRuntime
	 *             Если что-то пошло не так...
	 * 
	 */
	public void transform(OutputStream os, XLSharedStrings sharedStrings,
			Map<Integer, StatementProducer> placeholders)
			throws EXLReporterRuntime {

		WorksheetParser p = null;
		try {
			XMLStreamWriter xmlWriter = XMLOutputFactory.newInstance()
					.createXMLStreamWriter(new OutputStreamWriter(os, "UTF-8"));
			p = new WorksheetParser(xmlWriter, sharedStrings, placeholders);
			TransformerFactory.newInstance().newTransformer()
					.transform(new StreamSource(is), new SAXResult(p));
			xmlWriter.flush();
		} catch (Exception e) {
			if (p != null && p.getError() != null)
				throw p.getError();
			else
				throw new EXLReporterRuntime(
						"Error while transforming worksheet: " + e.getClass()
								+ " - " + e.getMessage());
		}
	}

	private enum ParserState {
		INITIAL, SHEET_DATA, ROW, C, V, AFTER_PLACEHOLDER;
	}

	private class WorksheetParser extends DefaultHandler {

		private EXLReporterRuntime e = null;

		private final XMLStreamWriter xmlWriter;
		private final XLSharedStrings sharedStrings;
		private final Map<Integer, StatementProducer> placeholders;

		private ParserState state = ParserState.INITIAL;

		private String templateCellType = "";

		private int activeRowNum = 0;
		private CellAddress activeCellAddress = new CellAddress(1, 1);

		private ResultSet activeResultSet = null;

		private int rowOffset = 0;

		public WorksheetParser(XMLStreamWriter xmlWriter,
				XLSharedStrings sharedStrings,
				Map<Integer, StatementProducer> placeholders) {
			this.xmlWriter = xmlWriter;
			this.sharedStrings = sharedStrings;
			this.placeholders = placeholders;
		}

		public EXLReporterRuntime getError() {
			return e;
		}

		private void error(Exception e) throws SAXException {
			this.e = new EXLReporterRuntime(e.getMessage());
			throw new SAXException(e);
		}

		@Override
		public void startPrefixMapping(String prefix, String uri)
				throws SAXException {
			try {
				if ("".equals(prefix))
					xmlWriter.setDefaultNamespace(uri);
				else
					xmlWriter.setPrefix(prefix, uri);

			} catch (XMLStreamException e) {
				error(e);
			}
		}

		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			StatementProducer p = null;
			try {
				String chars = new String(ch, start, length);
				if (state == ParserState.V
						&& "s".equalsIgnoreCase(templateCellType)) {
					int index = Integer.parseInt(chars);
					p = placeholders.get(index);
					if (p != null) {
						PreparedStatement stmt = p.produceStatement(conn);
						if (stmt.execute()) {
							activeResultSet = stmt.getResultSet();
							// Подменяем левый верхний угол названием столбца
							index = sharedStrings.appendString(activeResultSet
									.getMetaData().getColumnName(1));

							state = ParserState.AFTER_PLACEHOLDER;
						} else
							index = sharedStrings
									.appendString("No result set.");
					}
					xmlWriter.writeCharacters(String.valueOf(index));
					// И если при этом мы --- в резалтсете, то заполняем именами
					// столбцов текущую ячейку и те, что идут вправо
					if (state == ParserState.AFTER_PLACEHOLDER) {
						xmlWriter.writeEndElement(); // v
						xmlWriter.writeEndElement(); // c
						ResultSetMetaData md = activeResultSet.getMetaData();
						CellAddress ca = new CellAddress(
								activeCellAddress.getAddress());
						for (int i = 2; i <= md.getColumnCount(); i++) {
							xmlWriter.writeStartElement("c");
							ca.setCol(ca.getCol() + 1);
							xmlWriter.writeAttribute("r", ca.getAddress());
							xmlWriter.writeAttribute("t", "s");
							xmlWriter.writeStartElement("v");

							index = sharedStrings.appendString(md
									.getColumnName(i));
							xmlWriter.writeCharacters(String.valueOf(index));

							xmlWriter.writeEndElement(); // v
							xmlWriter.writeEndElement(); // c
						}
						return;
					}
				} else if (state != ParserState.AFTER_PLACEHOLDER)
					xmlWriter.writeCharacters(chars);
			} catch (XMLStreamException e) {
				this.e = new EXLReporterRuntime(e.getMessage());
				throw new SAXException(e);
			} catch (EXLReporterRuntime e) {
				this.e = e;
				throw new SAXException(e);
			} catch (SQLException e) {
				this.e = new EXLReporterRuntime(String.format(
						"SQL error while running %s with message: %s\n",
						p.getProcCall(), e.getMessage()));
				throw new SAXException(e);
			}
		}

		@Override
		public void endDocument() throws SAXException {
			try {
				xmlWriter.writeEndDocument();
			} catch (XMLStreamException e) {
				error(e);
			}
		}

		@Override
		public void processingInstruction(String target, String data)
				throws SAXException {
			try {
				xmlWriter.writeProcessingInstruction(target, data);
			} catch (XMLStreamException e) {
				error(e);
			}
		}

		@Override
		public void skippedEntity(String name) throws SAXException {

			try {
				xmlWriter.writeEntityRef(name);
			} catch (XMLStreamException e) {
				error(e);
			}
		}

		@Override
		public void startDocument() throws SAXException {
			try {
				xmlWriter.writeStartDocument("utf-8", "1.0");
			} catch (XMLStreamException e) {
				error(e);
			}

		}

		@Override
		public void startElement(String uri, String localName, String name,
				Attributes atts) throws SAXException {
			try {

				switch (state) {
				case INITIAL:
					if ("sheetData".equals(localName))
						state = ParserState.SHEET_DATA;
					break;
				case SHEET_DATA:
					if ("row".equals(localName)) {
						state = ParserState.ROW;
						activeRowNum = Integer.parseInt(atts.getValue("r"))
								+ rowOffset;
						// Информацию о строке копируем, но номер строки
						// увеличиваем на смещение
						xmlWriter.writeStartElement(uri, localName);
						copyAttrs(atts, String.valueOf(activeRowNum));
						return;
					}
					break;
				case ROW:
					if ("c".equals(localName)) {
						state = ParserState.C;

						templateCellType = atts.getValue("t");
						activeCellAddress = new CellAddress(atts.getValue("r"));
						activeCellAddress.setRow(activeCellAddress.getRow()
								+ rowOffset);

						// Информацию о ячейке копируем, но номер строки
						// увеличиваем на смещение.
						xmlWriter.writeStartElement(uri, localName);
						copyAttrs(atts, activeCellAddress.getAddress());
						return;
					}
					break;
				case C:
					if ("v".equals(localName)) {
						state = ParserState.V;
					}
					break;
				case AFTER_PLACEHOLDER:
					return;
				}
				xmlWriter.writeStartElement(uri, localName);
				copyAttrs(atts, atts.getValue("r"));

			} catch (XMLStreamException e) {
				error(e);
			}
		}

		private void copyAttrs(Attributes atts, String newAddress)
				throws XMLStreamException {
			for (int i = 0; i < atts.getLength(); i++)
				if ("r".equals(atts.getQName(i)))
					xmlWriter.writeAttribute("r", newAddress);
				else
					xmlWriter
							.writeAttribute(atts.getQName(i), atts.getValue(i));
		}

		@Override
		public void endElement(String uri, String localName, String name)
				throws SAXException {
			try {
				switch (state) {
				case V:
					state = ParserState.C;
					break;
				case C:
					// Вылезли из ячейки
					state = ParserState.ROW;
					break;
				case ROW:
					// Вылезли из строки
					state = ParserState.SHEET_DATA;

					break;
				case AFTER_PLACEHOLDER:
					if ("row".equals(localName)) {
						// Вылезли из строки с плейсхолдером
						state = ParserState.SHEET_DATA;
						xmlWriter.writeEndElement(); 
						// Самое главное: пишем строки данных
						ResultSetMetaData md = activeResultSet.getMetaData();
						CellAddress ca = new CellAddress(
								activeCellAddress.getAddress());
						while (activeResultSet.next()) {
							ca.setRow(ca.getRow() + 1);
							rowOffset++;
							xmlWriter.writeStartElement("row");
							xmlWriter.writeAttribute("r",
									String.valueOf(ca.getRow()));

							for (int i = 0; i < md.getColumnCount(); i++) {

								int ct = md.getColumnType(i + 1);

								CellAddress ca2 = new CellAddress(ca.getCol()
										+ i, ca.getRow());
								xmlWriter.writeStartElement("c");
								xmlWriter.writeAttribute("r", ca2.getAddress());
								if (!isInteger(ct) && !isFloat(ct))
									xmlWriter.writeAttribute("t", "s");
								xmlWriter.writeStartElement("v");

								String value;
								if (isInteger(ct))
									value = String.valueOf(activeResultSet
											.getInt(i + 1));
								else if (isFloat(ct))
									value = String.valueOf(activeResultSet
											.getDouble(i + 1));
								else
									value = String.valueOf(sharedStrings
											.appendString(activeResultSet
													.getString(i + 1)));
								xmlWriter.writeCharacters(value);
								xmlWriter.writeEndElement(); // v
								xmlWriter.writeEndElement(); // c
							}

							xmlWriter.writeEndElement(); // row

						}
						activeResultSet.close();
						activeResultSet = null;
					}
					return;
				case SHEET_DATA:
					state = ParserState.INITIAL;
					break;
				}
				xmlWriter.writeEndElement();
			} catch (Exception e) {
				error(e);
			}
		}

		private boolean isFloat(int ct) {
			return ct == FLOAT || ct == REAL || ct == DOUBLE || ct == NUMERIC
					|| ct == DECIMAL;
		}

		private boolean isInteger(int ct) {
			return ct == TINYINT || ct == SMALLINT || ct == INTEGER;
		}
	}

}
