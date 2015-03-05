package ru.curs.flute.xml2spreadsheet;

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

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

		XML2SpreadseetBLOB b = new XML2SpreadseetBLOB();
		OutputStream fos = b.getOutStream();
		XML2Spreadsheet.process(dataStream, descrStream, templateStream,
				OutputType.XLS, false, fos);

		assertTrue(b.size() > 6000);
	}

	@Test
	public void test2() throws FileNotFoundException, XML2SpreadSheetError {
		InputStream descrStream = TestReader.class
				.getResourceAsStream("testsaxdescriptor3.xml");
		InputStream dataStream = TestReader.class
				.getResourceAsStream("testdata.xml");
		InputStream templateStream = TestReader.class
				.getResourceAsStream("template.xls");

		XML2SpreadseetBLOB b = new XML2SpreadseetBLOB();
		OutputStream fos = b.getOutStream();
		XML2Spreadsheet.process(dataStream, descrStream, templateStream,
				OutputType.XLS, true, fos);
		assertTrue(b.size() > 6000);
	}
}
