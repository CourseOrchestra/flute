package ru.curs.flute.xml2spreadsheet;

import java.io.InputStream;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Реализация ReportWriter для вывода в формат MSOffice (XLSX).
 */
final class XLSXReportWriter extends POIReportWriter {

	XLSXReportWriter(InputStream template) throws XML2SpreadSheetError {
		super(template);
	}

	@Override
	Workbook createResultWb() {
		return new XSSFWorkbook();
	}
}
