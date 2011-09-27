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
	private void processElement(DescriptorElement de, Element xe)
			throws XML2SpreadSheetError {
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
	private void processIteration(Element parent, DescriptorIteration i)
			throws XML2SpreadSheetError {
		getWriter().startSequence(i.isHorizontal());

		for (DescriptorElement de : i.getElements())
			if ("(before)".equals(de.getElementName()))
				processElement(de, parent);

		Node n = parent.getFirstChild();
		int elementIndex = -1;
		iteration: while (n != null) {
			// Нас интересуют только элементы
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				elementIndex++;
				if (compareIndices(i.getIndex(), elementIndex)) {
					for (DescriptorElement e : i.getElements())
						if (compareNames(e.getElementName(), n.getNodeName()))
							processElement(e, (Element) n);
					// Если явно задан индекс, то на этом заканчиваем итерацию
					if (i.getIndex() >= 0)
						break iteration;
				}
			}
			n = n.getNextSibling();
		}

		for (DescriptorElement de : i.getElements())
			if ("(after)".equals(de.getElementName()))
				processElement(de, parent);

		getWriter().endSequence();
	}

	@Override
	void process() throws XML2SpreadSheetError {
		// Обработка в DOM-режиме --- рекурсивная, управляемая дескриптором.
		if (getDescriptor().getElementName().equals(
				xmlData.getDocumentElement().getNodeName()))
			processElement(getDescriptor(), xmlData.getDocumentElement());
		getWriter().flush();
	}
}
