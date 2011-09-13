package ru.curs.flute.fastxl.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ru.curs.flute.fastxl.CellAddress;

public class TestCellAddress {
	@Test
	public void test1() {
		CellAddress ca = new CellAddress("D12");
		assertEquals(4, ca.getCol());
		assertEquals(12, ca.getRow());
		
		assertEquals("D12", ca.getAddress());

		ca = new CellAddress("AB11");
		assertEquals(28, ca.getCol());
		assertEquals(11, ca.getRow());
		
		assertEquals("AB11", ca.getAddress());

	}

}
