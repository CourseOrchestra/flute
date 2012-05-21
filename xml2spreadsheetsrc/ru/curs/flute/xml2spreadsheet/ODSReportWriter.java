package ru.curs.flute.xml2spreadsheet;

import java.io.InputStream;

/**
 * Реализация ReportWriter для вывода в формат OpenOffice (ODS).
 */
final class ODSReportWriter extends ReportWriter {

	ODSReportWriter(InputStream template) throws XML2SpreadSheetError {
		// TODO Auto-generated constructor stub
		throw new XML2SpreadSheetError("ODS is not implemented yet!..");
	}

	@Override
	void newSheet(String sheetName, String sourceSheet,
			int startRepeatingColumn, int endRepeatingColumn,
			int startRepeatingRow, int endRepeatingRow) {
		// TODO Auto-generated method stub

	}

	@Override
	void putSection(XMLContext context, CellAddress growthPoint2,
			String sourceSheet, RangeAddress range) {
		// TODO Auto-generated method stub

	}

	@Override
	public void flush() throws XML2SpreadSheetError {
		// TODO Auto-generated method stub

	}
}
