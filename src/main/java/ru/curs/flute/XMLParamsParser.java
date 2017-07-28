package ru.curs.flute;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.IntConsumer;

import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.ContentHandler;

import net.sf.saxon.jaxp.SaxonTransformerFactory;
import ru.curs.flute.EFluteCritical;

abstract class XMLParamsParser {

	private final StringBuilder errorBuilder = new StringBuilder();

	protected void parse(InputStream is, String id) throws TransformerFactoryConfigurationError, EFluteCritical {
		System.out.printf("Parsing Flute configuration file (%s)...", id);
		try {
			try {
				SaxonTransformerFactory.newInstance().newTransformer().transform(new StreamSource(is),
						new SAXResult(getSAXHandler()));
			} catch (Exception e) {
				errorBuilder.append("ERROR while parsing config: " + e.getMessage());
			}
			is.close();
		} catch (IOException e) {
			errorBuilder.append("ERROR while reading config: " + e.getMessage());
		}

		if (errorBuilder.length() > 0) {
			System.out.printf("%n%s", errorBuilder.toString());
			System.out.println("Flute initialization aborted.");
			throw new EFluteCritical(errorBuilder.toString());
		}
		System.out.printf("done.%n");
	}

	abstract ContentHandler getSAXHandler();

	protected void processInt(String buf, String paramName, boolean zeroAllowed, IntConsumer setter) {
		try {
			int val = Integer.parseInt(buf);
			if (zeroAllowed ? val >= 0 : val > 0)
				setter.accept(val);
			else
				errorBuilder.append(String.format("Invalid parameter '%s' value: %s. %s integer value exptected.%n",
						paramName, buf, zeroAllowed ? "Non-negative" : "Positive"));
		} catch (NumberFormatException e) {
			errorBuilder
					.append(String.format("Invalid parameter '%s': %s. Integer value exptected.%n", paramName, buf));
		}
	}
}