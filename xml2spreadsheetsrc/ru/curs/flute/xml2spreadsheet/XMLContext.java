package ru.curs.flute.xml2spreadsheet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.xml.sax.Attributes;

/**
 * Указывает на контекст XML файла, в котором могут быть вычислены
 * XPath-выражения.
 */
abstract class XMLContext {

	private static final Pattern P = Pattern.compile("~\\{([^}]+)\\}");

	/**
	 * Вычисляет значение строки, содержащей, возможно, xpath-выражение.
	 * 
	 * @param formatString
	 *            строка, содержащая, возможно, xpath-выражения.
	 */
	final String calc(String formatString) {
		Matcher m = P.matcher(formatString);
		StringBuffer sb = new StringBuffer();
		while (m.find())
			m.appendReplacement(sb, getXPathValue(m.group(1)));
		m.appendTail(sb);
		return sb.toString();
	}

	abstract String getXPathValue(String xpath);

	static final class DOMContext extends XMLContext {
		private final Node n;
		private XPath evaluator;

		DOMContext(Node n) {
			if (n == null)
				throw new NullPointerException();
			this.n = n;
		}

		@Override
		String getXPathValue(String xpath) {
			if (evaluator == null)
				evaluator = XPathFactory.newInstance().newXPath();
			try {
				XPathExpression expr = evaluator.compile(xpath);
				return (String) expr.evaluate(n, XPathConstants.STRING);
			} catch (XPathExpressionException e) {
				return "{" + e.getMessage() + "}";
			}
		}
	}

	static final class SAXContext extends XMLContext {

		private final Attributes attr;

		SAXContext(Attributes attr) {
			this.attr = attr;
		}

		@Override
		String getXPathValue(String xpath) {
			if (xpath.startsWith("@"))
				return attr.getValue(xpath.substring(1));
			else
				return "{Only references to attributes in SAX mode are allowed}";
		}
	}

}
