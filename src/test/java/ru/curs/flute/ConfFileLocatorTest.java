package ru.curs.flute;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.curs.flute.ConfFileLocator;
import ru.curs.flute.EFluteCritical;

public class ConfFileLocatorTest {

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Test
	public void test() throws EFluteCritical {
		String confFile = ConfFileLocator.getConfFile().toString();
		assertTrue(confFile.endsWith("flute.xml"));
		// exception.expectMessage("flute.xml");
		// ConfFileLocator.getConfInputStream();
	}

	@Test
	public void test2() throws EFluteCritical {
		ConfFileLocator.setFile(new File("abcdef.xml"));
		String confFile = ConfFileLocator.getConfFile().toString();
		assertTrue(confFile.endsWith("flute.xml"));
		// exception.expectMessage("abcdef.xml");
		// ConfFileLocator.getConfInputStream();
	}

}
