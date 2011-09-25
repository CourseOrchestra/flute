package ru.curs.flute.xml2spreadsheet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Указывает на диапазон ячеек.
 */
final class RangeAddress {

	private static final Pattern RANGE_ADDRESS = Pattern
			.compile("([A-Z]+[0-9]+)(:([A-Z]+[0-9]+))?");
	private final CellAddress topLeft;
	private final CellAddress bottomRight;

	public RangeAddress(String address) throws XML2SpreadSheetError {
		Matcher m = RANGE_ADDRESS.matcher(address);
		if (!m.matches())
			throw new XML2SpreadSheetError("Incorrect range: " + address);
		CellAddress c1 = new CellAddress(m.group(1));
		CellAddress c2 = new CellAddress(m.group(3) == null ? m.group(1)
				: m.group(3));
		topLeft = new CellAddress(Math.min(c1.getCol(), c2.getCol()), Math.min(
				c1.getRow(), c2.getRow()));
		bottomRight = new CellAddress(Math.max(c1.getCol(), c2.getCol()),
				Math.max(c1.getRow(), c2.getRow()));
	}

	public CellAddress topLeft() {
		return topLeft;
	}

	public CellAddress bottomRight() {
		return bottomRight;
	}

	public int left() {

		return topLeft.getCol();
	}

	public int right() {
		return bottomRight.getCol();
	}

	public int top() {
		return topLeft.getRow();
	}

	public int bottom() {
		return bottomRight.getRow();
	}
}
