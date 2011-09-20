package ru.curs.flute.xml2spreadsheet;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

final class DOMDataReader extends XMLDataReader {

	private final Document xmlData;

	DOMDataReader(InputStream xmlData, DescriptorElement xmlDescriptor,
			ReportWriter writer) throws XML2SpreadSheetError {
		super(writer, xmlDescriptor);
		try {
			DocumentBuilder db = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			this.xmlData = db.parse(xmlData);
		} catch (Exception e) {
			throw new XML2SpreadSheetError("Error while parsing input data: "
					+ e.getMessage());
		}

	}

	// В режиме итерации нашёлся подходящий элемент
	private void processElement(DescriptorElement de, Element xe) {
		XMLContext context = null;
		for (DescriptorSubelement se : de.getSubelements()) {
			if (se instanceof DescriptorIteration) {
				processIteration(xe, (DescriptorIteration) se);
			} else if (se instanceof DescriptorOutput) {
				// Контекст имеет смысл создавать лишь если есть хоть один
				// output
				if (context == null)
					context = new XMLContext.DOMContext(xe);
				processOutput(context, (DescriptorOutput) se);
			}
		}

	}

	// По субэлементам текущего элемента надо провести итерацию
	private void processIteration(Element parent, DescriptorIteration i) {
		getWriter().startSequence(i.isHorizontal());
		Node n = parent.getFirstChild();
		int elementIndex = -1;
		while (n != null) {
			// Нас интересуют только элементы
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				elementIndex++;
				if (i.getIndex() < 0 || i.getIndex() == elementIndex)
					for (DescriptorElement e : i.getElements())
						if ("*".equals(e.getElementName())
								|| e.getElementName().equals(n.getNodeName()))
							processElement(e, (Element) n);
			}
			n = n.getNextSibling();
		}
		getWriter().endSequence();
	}

	// Обработка вывода
	private void processOutput(XMLContext c, DescriptorOutput o) {
		if (o.getWorksheet() != null && !"".equals(o.getWorksheet())) {
			String wsName = c.calc(o.getWorksheet());
			getWriter().sheet(wsName);
		}
		getWriter().section(c, o.getRange());
	}

	@Override
	void process() throws XML2SpreadSheetError {
		// Обработка в DOM-режиме --- рекурсивная, управляемая дескриптором.
		if (getDescriptor().getElementName().equals(
				xmlData.getDocumentElement().getNodeName()))
			processElement(getDescriptor(), xmlData.getDocumentElement());
	}
}
