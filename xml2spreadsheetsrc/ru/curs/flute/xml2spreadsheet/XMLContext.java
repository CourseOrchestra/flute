package ru.curs.flute.xml2spreadsheet;

/**
 * Указывает на контекст XML файла, в котором могут быть вычислены
 * XPath-выражения.
 */
abstract class XMLContext {
	// TODO implement
	static final class DOMContext extends XMLContext {
	}

	static final class SAXContext extends XMLContext {
	}

	public String getValue(String xpath) {
		return null;
	}

}
