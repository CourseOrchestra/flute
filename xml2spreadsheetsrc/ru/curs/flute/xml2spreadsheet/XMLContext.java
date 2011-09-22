package ru.curs.flute.xml2spreadsheet;

import org.w3c.dom.Node;
import org.xml.sax.Attributes;

/**
 * Указывает на контекст XML файла, в котором могут быть вычислены
 * XPath-выражения.
 */
abstract class XMLContext {

	/**
	 * Вычисляет значение строки, содержащей, возможно, xpath-выражение.
	 * 
	 * @param formatString
	 *            строка, содержащая, возможно, xpath-выражения.
	 */
	final String calc(String formatString) {
		return null;
		// TODO implement using regexp search and getXPathValue(..)
	}

	abstract String getXPathValue(String xpath);

	static final class DOMContext extends XMLContext {
		private final Node n;

		DOMContext(Node n) {
			this.n = n;
		}

		@Override
		String getXPathValue(String xpath) {
			// TODO Auto-generated method stub
			return null;
		}
	}

	static final class SAXContext extends XMLContext {

		private final Attributes attr;

		SAXContext(Attributes attr) {
			this.attr = attr;
		}

		@Override
		String getXPathValue(String xpath) {
			// TODO Auto-generated method stub
			return null;
		}
	}

}
