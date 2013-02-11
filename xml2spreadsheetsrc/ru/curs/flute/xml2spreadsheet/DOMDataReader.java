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
			Element xe, int position) throws XML2SpreadSheetError {
		XMLContext context = null;
		for (DescriptorSubelement se : de.getSubelements()) {
			if (se instanceof DescriptorIteration) {
				processIteration(elementPath, xe, (DescriptorIteration) se,
						position);
			} else if (se instanceof DescriptorOutput) {
				// Контекст имеет смысл создавать лишь если есть хоть один
				// output
				if (context == null)
					context = new XMLContext.DOMContext(xe, elementPath,
							position);
				processOutput(context, (DescriptorOutput) se);
			}
		}

	}

	// По субэлементам текущего элемента надо провести итерацию
	private void processIteration(String elementPath, Element parent,
			DescriptorIteration i, int position) throws XML2SpreadSheetError {

		final HashMap<String, Integer> elementIndices = new HashMap<>();

		getWriter().startSequence(i.isHorizontal());

		for (DescriptorElement de : i.getElements())
			if ("(before)".equals(de.getElementName()))
				processElement(elementPath, de, parent, position);

		Node n = parent.getFirstChild();
		int elementIndex = -1;

		int pos = 0;
		iteration: while (n != null) {
			// Нас интересуют только элементы
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				// Поддерживаем таблицу с нумерацией нод для вычисления пути
				Integer ind = elementIndices.get(n.getNodeName());
				if (ind == null)
					ind = 0;
				elementIndices.put(n.getNodeName(), ind + 1);

				elementIndex++;
				boolean found = false;
				if (compareIndices(i.getIndex(), elementIndex)) {
					HashMap<String, String> atts = new HashMap<>();

					for (int j = 0; j < n.getAttributes().getLength(); j++) {
						Node att = n.getAttributes().item(j);
						atts.put(att.getNodeName(), att.getNodeValue());
					}

					for (DescriptorElement e : i.getElements())
						if (compareNames(e.getElementName(), n.getNodeName(),
								atts)) {
							found = true;
							processElement(String.format("%s/%s[%s]",
									elementPath, n.getNodeName(),
									elementIndices.get(n.getNodeName())
											.toString()), e, (Element) n,
									pos + 1);
						}
					// Если явно задан индекс, то на этом заканчиваем итерацию
					if (i.getIndex() >= 0)
						break iteration;
				}
				if (found)
					pos++;
			}
			n = n.getNextSibling();
		}

		for (DescriptorElement de : i.getElements())
			if ("(after)".equals(de.getElementName()))
				processElement(elementPath, de, parent, position);

		getWriter().endSequence(i.getMerge());
	}

	@Override
	void process() throws XML2SpreadSheetError {
		// Обработка в DOM-режиме --- рекурсивная, управляемая дескриптором.
		if (getDescriptor().getElementName().equals(
				xmlData.getDocumentElement().getNodeName()))
			processElement("/" + getDescriptor().getElementName() + "[1]",
					getDescriptor(), xmlData.getDocumentElement(), 1);
		getWriter().flush();
	}
}
