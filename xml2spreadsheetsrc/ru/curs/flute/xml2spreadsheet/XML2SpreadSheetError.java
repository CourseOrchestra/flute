package ru.curs.flute.xml2spreadsheet;

/**
 * Ошибка, происходящая при работе построителя отчётов.
 * 
 */
public class XML2SpreadSheetError extends Exception {

	private static final long serialVersionUID = 4382588062277186741L;

	public XML2SpreadSheetError(String string) {
		super(string);
	}
}
