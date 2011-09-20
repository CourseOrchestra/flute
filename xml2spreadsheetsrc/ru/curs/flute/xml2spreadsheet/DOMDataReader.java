package ru.curs.flute.xml2spreadsheet;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

final class DOMDataReader extends XMLDataReader {

	private final Document xmlData;

	DOMDataReader(InputStream xmlData, DescriptorElement xmlDescriptor,
			ReportWriter writer) throws XML2SpreadSheetError {
		super(writer, xmlDescriptor);
		try {
			DocumentBuilder db = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			this.xmlData = db.parse(xmlData);
		} catch (Exception e) {
			throw new XML2SpreadSheetError("Error while parsing input data: "
					+ e.getMessage());
		}

	}

	@Override
	void process() throws XML2SpreadSheetError {
		// TODO Auto-generated method stub

	}

}
