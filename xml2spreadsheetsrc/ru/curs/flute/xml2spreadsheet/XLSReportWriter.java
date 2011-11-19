package ru.curs.flute.xml2spreadsheet;

import java.io.InputStream;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Реализация ReportWriter для вывода в формат MSOffice 97-2003 (XLS).
 */
final class XLSReportWriter extends POIReportWriter {

	XLSReportWriter(InputStream template) throws XML2SpreadSheetError {
		super(template);
	}

	@Override
	Workbook createResultWb() {
		return new HSSFWorkbook();
	}
}
