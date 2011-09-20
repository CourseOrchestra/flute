package ru.curs.flute.xml2spreadsheet;

import java.io.InputStream;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

final class SAXDataReader extends XMLDataReader {

	private final Source xmlData;

	SAXDataReader(InputStream xmlData, DescriptorElement xmlDescriptor,
			ReportWriter writer) {
		super(writer, xmlDescriptor);
		this.xmlData = new StreamSource(xmlData);

	}

	@Override
	void process() throws XML2SpreadSheetError {
		// TODO Auto-generated method stub

	}

}
