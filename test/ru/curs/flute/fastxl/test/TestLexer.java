package ru.curs.flute.fastxl.test;

import static org.junit.Assert.*;

import java.util.regex.Matcher;

import javax.xml.xpath.XPathExpressionException;

import org.junit.Test;

import ru.curs.flute.fastxl.StatementProducer;

public class TestLexer {
	@Test
	public void testLexer1() throws XPathExpressionException {
		assertNull(StatementProducer.validate("bl~~ah-blah", null));
		assertNull(StatementProducer.validate("~foo(1,2)~~AAA", null));

		StatementProducer l = StatementProducer.validate(" ~~dbo.[foo] (1, 2)~~AAA", null);
		assertNotNull(l);
		assertEquals("AAA", l.getRangeName());
		assertEquals("dbo.[foo]", l.getProcName());
		assertEquals("{call dbo.[foo](?,?)}", l.getStatementTemplate());
		
		l = StatementProducer.validate("\t~~foo(1,2)", null);
		assertNotNull(l);
		assertNull(l.getRangeName());
		l = StatementProducer.validate(" ~~foo(1.4,2,'hhhhh''iiii') ~~AABC_1", null);
		assertNotNull(l);
		assertEquals("{call foo(?,?,?)}", l.getStatementTemplate());
		assertEquals("AABC_1", l.getRangeName());
	}

	@Test
	public void testRegexps() {
		Matcher m = StatementProducer.IDENTIFIER.matcher("[db sqas wwwe o].blah");
		assertTrue(m.find());
		assertEquals("[db sqas wwwe o].blah", m.group(0));

		m = StatementProducer.IDENTIFIER.matcher("dbo.blah");
		assertTrue(m.find());
		assertEquals("dbo.blah", m.group(0));

		m = StatementProducer.IDENTIFIER
				.matcher("112 dbo.[blss ыыы ah].bb.[s sss] aaa");
		assertTrue(m.find());
		assertEquals("dbo.[blss ыыы ah].bb.[s sss]", m.group(0));

	}

	@Test
	public void testRegexps2() {
		Matcher m = StatementProducer.NUMBER.matcher("12314");
		assertTrue(m.find());
		assertEquals("12314", m.group(0));

		m = StatementProducer.NUMBER.matcher("-1112");
		assertTrue(m.find());
		assertEquals("-1112", m.group(0));

		m = StatementProducer.NUMBER.matcher("+111.22");
		assertTrue(m.find());
		assertEquals("+111.22", m.group(0));

		m = StatementProducer.NUMBER.matcher("11.22e-11");
		assertTrue(m.find());
		assertEquals("11.22e-11", m.group(0));

		m = StatementProducer.NUMBER.matcher("5E8");
		assertTrue(m.find());
		assertEquals("5E8", m.group(0));

	}

	@Test
	public void testRegexps3() {
		Matcher m = StatementProducer.STRING.matcher("'aaaa'");
		assertTrue(m.find());
		assertEquals("'aaaa'", m.group(0));

		m = StatementProducer.STRING.matcher("'a''bbbb'");
		assertTrue(m.find());
		assertEquals("'a''bbbb'", m.group(0));

		m = StatementProducer.STRING.matcher("'aaaa'bbbb'ddd'");
		assertTrue(m.find());
		assertEquals("'aaaa'", m.group(0));

		m = StatementProducer.STRING.matcher("''''");
		assertTrue(m.find());
		assertEquals("''''", m.group(0));

	}
}
