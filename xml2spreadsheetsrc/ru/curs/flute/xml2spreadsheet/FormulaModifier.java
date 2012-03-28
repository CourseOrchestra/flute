package ru.curs.flute.xml2spreadsheet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Содержит метод обработки формул (сдвижки адресов ячеек).
 */
public class FormulaModifier {

	private static final Pattern CELL_ADDRESS = Pattern
			.compile("[A-Z]+[0-9]+[(]?");

	/**
	 * Модифицирует формулу, сдвигая адреса ячеек.
	 * 
	 * @param val
	 *            Текстовое значение формулы.
	 * @param dx
	 *            Сдвижка по колонкам.
	 * @param dy
	 *            Сдвижка по строкам.
	 */
	public static String modifyFormula(String val, int dx, int dy) {
		Matcher m = CELL_ADDRESS.matcher(val);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String buf = m.group(0);
			if (buf.endsWith("(")) {
				// Случай формулы (LOG10, ATAN2 и т. п.) обрабатываем отдельно
				m.appendReplacement(sb, buf);
			} else {
				CellAddress cellAddr = new CellAddress(buf);
				cellAddr.setCol(cellAddr.getCol() + dx);
				cellAddr.setRow(cellAddr.getRow() + dy);
				m.appendReplacement(sb, cellAddr.getAddress());
			}
		}
		m.appendTail(sb);
		return sb.toString();
	}
}
