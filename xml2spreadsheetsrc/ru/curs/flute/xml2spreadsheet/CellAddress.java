package ru.curs.flute.xml2spreadsheet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Преобразует адрес ячейки в формате A1 в пару "строка, столбец" и обратно.
 * 
 */
public final class CellAddress {

	private static final int RADIX = 'Z' - 'A' + 1;
	private static final Pattern CELL_ADDRESS = Pattern
			.compile("([A-Z]+)([0-9]+)");

	private int row;
	private int col;

	public CellAddress(String address) {
		setAddress(address);
	}

	public CellAddress(int col, int row) {
		this.col = col;
		this.row = row;
	}

	/**
	 * Возвращает номер строки.
	 */
	public int getRow() {
		return row;
	}

	/**
	 * Возвращает номер колонки.
	 */
	public int getCol() {
		return col;
	}

	/**
	 * Устанавливает номер строки.
	 * 
	 * @param row
	 *            номер строки
	 */
	public void setRow(int row) {
		this.row = row;
	}

	/**
	 * Устанавливает номер колонки.
	 * 
	 * @param col
	 *            номер колонки
	 * 
	 */
	public void setCol(int col) {
		this.col = col;
	}

	/**
	 * Возвращает адрес.
	 */
	public String getAddress() {
		int c = col;
		String sc = "";
		do {
			char d = (char) (c % RADIX + 'A' - 1);
			sc = d + sc;
			c = c / RADIX;
		} while (c > 0);
		return sc + String.valueOf(row);
	}

	/**
	 * Устанавливает адрес.
	 * 
	 * @param address
	 *            адрес
	 */
	public void setAddress(String address) {
		Matcher m = CELL_ADDRESS.matcher(address);
		m.matches();
		row = Integer.parseInt(m.group(2));

		col = 0;
		String a = m.group(1);
		for (int i = 0; i < a.length(); i++) {
			col = col * RADIX;
			char c = a.charAt(i);
			int d = c - 'A' + 1;
			col += d;
		}
	}

	@Override
	public int hashCode() {
		return row * col + row;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CellAddress)
			return equals((CellAddress) obj);
		else
			return super.equals(obj);
	}

	/**
	 * Метод equals специально для сравнения адресов ячеек.
	 * 
	 * @param a
	 *            другой адрес ячейки
	 */
	public boolean equals(CellAddress a) {
		return row == a.row && col == a.col;
	}
}
