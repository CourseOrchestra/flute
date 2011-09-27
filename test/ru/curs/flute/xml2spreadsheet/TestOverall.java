package ru.curs.flute.xml2spreadsheet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.junit.Test;

public class TestOverall {
	@Test
	public void test1() throws FileNotFoundException, XML2SpreadSheetError {
		InputStream descrStream = TestReader.class
				.getResourceAsStream("testdescriptor3.xml");
		InputStream dataStream = TestReader.class
				.getResourceAsStream("testdata.xml");
		InputStream templateStream = TestReader.class
				.getResourceAsStream("template.xls");

		File f = new File("c:/temp/result.xls");
		FileOutputStream fos = new FileOutputStream(f);

		XML2Spreadsheet.process(dataStream, descrStream, templateStream,
				OutputType.XLS, false, fos);
	}
}
