package ru.curs.flute.xml2spreadsheet;

import java.io.InputStream;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ru.curs.flute.xml2spreadsheet.XMLContext.SAXContext;

final class SAXDataReader extends XMLDataReader {

	private final Source xmlData;

	SAXDataReader(InputStream xmlData, DescriptorElement xmlDescriptor,
			ReportWriter writer) {
		super(writer, xmlDescriptor);
		this.xmlData = new StreamSource(xmlData);

	}

	/**
	 * Адаптирует дескриптор элемента к SAX-парсингу.
	 * 
	 */
	private final class SAXElementDescriptor {
		private int elementIndex = -1;
		private final int desiredIndex;
		private final XMLContext context;
		private final boolean iterate;
		private final boolean horizontal;
		private final List<DescriptorOutput> preOutputs = new LinkedList<>();
		private final List<DescriptorElement> expectedElements = new LinkedList<>();
		private final List<DescriptorOutput> postOutputs = new LinkedList<>();
		private final List<DescriptorOutput> headerOutputs = new LinkedList<>();
		private final List<DescriptorOutput> footerOutputs = new LinkedList<>();

		SAXElementDescriptor() {
			context = null;
			iterate = false;
			horizontal = false;
			desiredIndex = -1;
		}

		SAXElementDescriptor(DescriptorElement e, XMLContext context)
				throws SAXException {
			this.context = context;
			boolean iterate = false;
			boolean horizontal = false;
			int desiredIndex = -1;
			for (DescriptorSubelement se : e.getSubelements())
				if (!iterate) {
					// До тэга iteration
					if (se instanceof DescriptorOutput)
						preOutputs.add((DescriptorOutput) se);
					else if (se instanceof DescriptorIteration) {
						for (DescriptorElement de : ((DescriptorIteration) se)
								.getElements()) {
							if ("(before)".equals(de.getElementName())) {
								for (DescriptorSubelement se2 : de
										.getSubelements())
									if (se2 instanceof DescriptorOutput)
										headerOutputs
												.add((DescriptorOutput) se2);
							} else if ("(after)".equals(de.getElementName())) {
								for (DescriptorSubelement se2 : de
										.getSubelements())
									if (se2 instanceof DescriptorOutput)
										footerOutputs
												.add((DescriptorOutput) se2);
							} else
								expectedElements.add(de);
						}
						desiredIndex = ((DescriptorIteration) se).getIndex();
						iterate = true;
						horizontal = ((DescriptorIteration) se).isHorizontal();

					}
				} else {
					// После тэга iteration
					if (se instanceof DescriptorOutput)
						postOutputs.add((DescriptorOutput) se);
					else if (se instanceof DescriptorIteration)
						throw new SAXException(
								"For SAX mode only one iteration element is allowed for each element descriptor.");
				}
			this.iterate = iterate;
			this.horizontal = horizontal;
			this.desiredIndex = desiredIndex;
		}
	}

	@Override
	void process() throws XML2SpreadSheetError {

		final class Parser extends DefaultHandler {
			private final Deque<SAXElementDescriptor> elementsStack = new LinkedList<SAXElementDescriptor>();

			private void bypass() {
				elementsStack.push(new SAXElementDescriptor());
			}

			@Override
			public void startElement(String uri, String localName, String name,
					Attributes atts) throws SAXException {
				SAXElementDescriptor curDescr = elementsStack.peek();
				curDescr.elementIndex++;
				if (compareIndices(curDescr.desiredIndex, curDescr.elementIndex)) {
					boolean found = false;
					searchElements: for (DescriptorElement e : curDescr.expectedElements) {
						if (compareNames(e.getElementName(), localName)) {

							XMLContext context = new SAXContext(atts);
							SAXElementDescriptor sed = new SAXElementDescriptor(
									e, context);
							elementsStack.push(sed);

							// По пред-выводам выполняем вывод.
							for (DescriptorOutput o : sed.preOutputs)
								try {
									processOutput(sed.context, o);
								} catch (XML2SpreadSheetError e1) {
									throw new SAXException(e1.getMessage());
								}
							// Начинаем обрамление итерации
							try {
								if (sed.iterate) {
									getWriter().startSequence(sed.horizontal);
									for (DescriptorOutput deo : sed.headerOutputs)
										processOutput(sed.context, deo);
								}
							} catch (XML2SpreadSheetError e1) {
								throw new SAXException(e1.getMessage());
							}
							found = true;
							break searchElements;
						}
					}
					if (!found)
						bypass();
				} else
					bypass();
			}

			@Override
			public void endElement(String uri, String localName, String name)
					throws SAXException {
				SAXElementDescriptor sed = elementsStack.pop();
				try {
					// Завершаем обрамление итерации
					if (sed.iterate) {
						for (DescriptorOutput deo : sed.footerOutputs)
							processOutput(sed.context, deo);
						getWriter().endSequence();
					}
					// По пост-выводам выполняем вывод
					for (DescriptorOutput o : sed.postOutputs)
						processOutput(sed.context, o);
				} catch (XML2SpreadSheetError e1) {
					throw new SAXException(e1.getMessage());
				}
			}
		}

		Parser parser = new Parser();
		SAXElementDescriptor sed = new SAXElementDescriptor();
		sed.expectedElements.add(getDescriptor());
		parser.elementsStack.push(sed);

		try {
			TransformerFactory.newInstance().newTransformer()
					.transform(xmlData, new SAXResult(parser));
		} catch (Exception e) {
			throw new XML2SpreadSheetError("Error while processing XML data: "
					+ e.getMessage());

		}
		getWriter().flush();
	}
}
