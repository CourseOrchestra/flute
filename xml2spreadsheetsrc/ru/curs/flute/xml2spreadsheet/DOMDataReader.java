package ru.curs.flute.xml2spreadsheet;

import java.io.InputStream;
import java.util.HashMap;

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
	private void processElement(String elementPath, DescriptorElement de,
			Element xe) throws XML2SpreadSheetError {
		XMLContext context = null;
		for (DescriptorSubelement se : de.getSubelements()) {
			if (se instanceof DescriptorIteration) {
				processIteration(elementPath, xe, (DescriptorIteration) se);
			} else if (se instanceof DescriptorOutput) {
				// Контекст имеет смысл создавать лишь если есть хоть один
				// output
				if (context == null)
					context = new XMLContext.DOMContext(xe, elementPath);
				processOutput(context, (DescriptorOutput) se);
			}
		}

	}

	// По субэлементам текущего элемента надо провести итерацию
	private void processIteration(String elementPath, Element parent,
			DescriptorIteration i) throws XML2SpreadSheetError {

		final HashMap<String, Integer> elementIndices = new HashMap<>();

		getWriter().startSequence(i.isHorizontal());

		for (DescriptorElement de : i.getElements())
			if ("(before)".equals(de.getElementName()))
				processElement(elementPath, de, parent);

		Node n = parent.getFirstChild();
		int elementIndex = -1;
		iteration: while (n != null) {
			// Нас интересуют только элементы
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				// Поддерживаем таблицу с нумерацией нод для вычисления пути
				Integer ind = elementIndices.get(n.getNodeName());
				if (ind == null)
					ind = 0;
				elementIndices.put(n.getNodeName(), ind + 1);

				elementIndex++;
				if (compareIndices(i.getIndex(), elementIndex)) {
					for (DescriptorElement e : i.getElements())
						if (compareNames(e.getElementName(), n.getNodeName()))
							processElement(String.format("%s/%s[%s]",
									elementPath, n.getNodeName(),
									elementIndices.get(n.getNodeName())
											.toString()), e, (Element) n);
					// Если явно задан индекс, то на этом заканчиваем итерацию
					if (i.getIndex() >= 0)
						break iteration;
				}
			}
			n = n.getNextSibling();
		}

		for (DescriptorElement de : i.getElements())
			if ("(after)".equals(de.getElementName()))
				processElement(elementPath, de, parent);

		getWriter().endSequence();
	}

	@Override
	void process() throws XML2SpreadSheetError {
		// Обработка в DOM-режиме --- рекурсивная, управляемая дескриптором.
		if (getDescriptor().getElementName().equals(
				xmlData.getDocumentElement().getNodeName()))
			processElement("/" + getDescriptor().getElementName() + "[1]",
					getDescriptor(), xmlData.getDocumentElement());
		getWriter().flush();
	}
}
