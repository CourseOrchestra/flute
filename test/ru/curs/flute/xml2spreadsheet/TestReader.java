package ru.curs.flute.xml2spreadsheet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.curs.flute.xml2spreadsheet.XMLDataReader.DescriptorElement;
import ru.curs.flute.xml2spreadsheet.XMLDataReader.DescriptorIteration;
import ru.curs.flute.xml2spreadsheet.XMLDataReader.DescriptorOutput;

public class TestReader {
	private InputStream descrStream;
	private InputStream dataStream;

	@Before
	public void setUp() {
		descrStream = TestReader.class
				.getResourceAsStream("testdescriptor.xml");
		dataStream = TestReader.class.getResourceAsStream("testdata.xml");
	}

	@After
	public void TearDown() {
		try {
			descrStream.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			dataStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testParseDescriptor() throws XML2SpreadSheetError, IOException {

		XMLDataReader reader = XMLDataReader.createReader(dataStream, descrStream,
				false, null);
		DescriptorElement d = reader.getDescriptor();

		assertEquals(2, d.getSubelements().size());
		assertEquals("report", d.getElementName());
		DescriptorIteration i = (DescriptorIteration) d.getSubelements().get(0);
		assertEquals(0, i.getIndex());
		assertFalse(i.isHorizontal());
		i = (DescriptorIteration) d.getSubelements().get(1);
		assertEquals(-1, i.getIndex());
		assertFalse(i.isHorizontal());

		assertEquals(1, i.getElements().size());
		d = i.getElements().get(0);
		assertEquals("sheet", d.getElementName());
		assertEquals(4, d.getSubelements().size());
		DescriptorOutput o = (DescriptorOutput) d.getSubelements().get(0);
		assertEquals("~[@name]", o.getWorksheet());
		o = (DescriptorOutput) d.getSubelements().get(1);
		assertNull(o.getWorksheet());
		i = (DescriptorIteration) d.getSubelements().get(2);
		assertEquals(-1, i.getIndex());
		assertTrue(i.isHorizontal());

	}
}
