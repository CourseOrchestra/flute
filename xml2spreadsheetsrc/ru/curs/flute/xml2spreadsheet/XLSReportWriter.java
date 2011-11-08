package ru.curs.flute.xml2spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;

/**
 * Реализация ReportWriter для вывода в формат MSOffice 97-2003 (XLS).
 */
final class XLSReportWriter extends ReportWriter {

	/**
	 * Регексп для числа (в общем случае, с плавающей точкой --- целые числа
	 * также должны попадать под этот регексп.
	 */
	public static final Pattern NUMBER = Pattern
			.compile("[+-]?\\d+(\\.\\d+)?([eE][+-]?\\d+)?");

	private final Workbook template;
	private final Workbook result;
	private Sheet activeTemplateSheet;
	private Sheet activeResultSheet;
	private final HashMap<CellStyle, CellStyle> stylesMap = new HashMap<CellStyle, CellStyle>();

	XLSReportWriter(InputStream template) throws XML2SpreadSheetError {
		try {
			this.template = WorkbookFactory.create(template);
		} catch (InvalidFormatException e) {
			throw new XML2SpreadSheetError(e.getMessage());
		} catch (IOException e) {
			throw new XML2SpreadSheetError(e.getMessage());
		}

		// Создаём новую книгу
		result = new HSSFWorkbook();

		// Копируем стили ячеек
		for (short i = 0; i < this.template.getNumCellStyles(); i++) {
			CellStyle csSource = this.template.getCellStyleAt(i);
			CellStyle csResult = result.createCellStyle();
			csResult.cloneStyleFrom(csSource);
			stylesMap.put(csSource, csResult);
		}
	}

	private void updateActiveTemplateSheet(String sourceSheet)
			throws XML2SpreadSheetError {
		if (sourceSheet != null)
			activeTemplateSheet = template.getSheet(sourceSheet);
		if (activeTemplateSheet == null)
			activeTemplateSheet = template.getSheetAt(0);
		if (activeTemplateSheet == null)
			throw new XML2SpreadSheetError(String.format(
					"Sheet '%s' does not exist.", sourceSheet));
	}

	@Override
	void newSheet(String sheetName, String sourceSheet)
			throws XML2SpreadSheetError {
		activeResultSheet = result.createSheet(sheetName);
		updateActiveTemplateSheet(sourceSheet);
		// Ищем число столбцов в исходнике
		int maxCol = 1;
		for (int i = activeTemplateSheet.getFirstRowNum(); i <= activeTemplateSheet
				.getLastRowNum(); i++) {
			Row r = activeTemplateSheet.getRow(i);
			if (r == null)
				continue;
			int c = r.getLastCellNum();
			if (c > maxCol)
				maxCol = c;
		}
		// Копируем ширины колонок (знак <, а не <= здесь не случайно, т. к.
		// getLastCellNum возвращает ширину строки ПЛЮС ЕДИНИЦА)
		for (int i = 0; i < maxCol; i++)
			activeResultSheet.setColumnWidth(i, activeTemplateSheet
					.getColumnWidth(i));

		// Копируем все настройки печати
		PrintSetup sourcePS = activeTemplateSheet.getPrintSetup();
		PrintSetup resultPS = activeResultSheet.getPrintSetup();
		resultPS.setCopies(sourcePS.getCopies());
		resultPS.setDraft(sourcePS.getDraft());
		resultPS.setFitHeight(sourcePS.getFitHeight());
		resultPS.setFitWidth(sourcePS.getFitWidth());
		resultPS.setFooterMargin(sourcePS.getFooterMargin());
		resultPS.setHeaderMargin(sourcePS.getHeaderMargin());
		resultPS.setHResolution(sourcePS.getHResolution());
		resultPS.setLandscape(sourcePS.getLandscape());
		resultPS.setLeftToRight(sourcePS.getLeftToRight());
		resultPS.setNoColor(sourcePS.getNoColor());
		resultPS.setNoOrientation(sourcePS.getNoOrientation());
		resultPS.setNotes(sourcePS.getNotes());
		resultPS.setPageStart(sourcePS.getPageStart());
		resultPS.setPaperSize(sourcePS.getPaperSize());
		resultPS.setScale(sourcePS.getScale());
		resultPS.setUsePage(sourcePS.getUsePage());
		resultPS.setValidSettings(sourcePS.getValidSettings());
		resultPS.setVResolution(sourcePS.getVResolution());
	}

	@Override
	void putSection(XMLContext context, CellAddress growthPoint,
			String sourceSheet, RangeAddress range) throws XML2SpreadSheetError {
		updateActiveTemplateSheet(sourceSheet);

		for (int i = range.top(); i <= range.bottom(); i++) {
			Row sourceRow = activeTemplateSheet.getRow(i - 1);
			if (sourceRow == null)
				continue;
			Row resultRow = activeResultSheet.getRow(growthPoint.getRow() + i
					- range.top() - 1);
			if (resultRow == null)
				resultRow = activeResultSheet.createRow(growthPoint.getRow()
						+ i - range.top() - 1);

			for (int j = range.left(); j < Math.min(range.right(), sourceRow
					.getLastCellNum()) + 1; j++) {
				Cell sourceCell = sourceRow.getCell(j - 1);
				Cell resultCell = resultRow.createCell(growthPoint.getCol() + j
						- range.left() - 1);

				// Копируем стиль...
				CellStyle csResult = stylesMap.get(sourceCell.getCellStyle());
				if (csResult != null)
					resultCell.setCellStyle(csResult);

				// Копируем значение...
				switch (sourceCell.getCellType()) {
				case Cell.CELL_TYPE_BOOLEAN:
					resultCell.setCellValue(sourceCell.getBooleanCellValue());
					break;
				case Cell.CELL_TYPE_NUMERIC:
					resultCell.setCellValue(sourceCell.getNumericCellValue());
					break;
				case Cell.CELL_TYPE_STRING:
				case Cell.CELL_TYPE_FORMULA:
					// ДЛЯ СТРОКОВЫХ ЯЧЕЕК ВЫЧИСЛЯЕМ ПОДСТАНОВКИ!!
					String buf = context.calc(sourceCell.getStringCellValue());
					// Если ячейка содержит строковое представление числа ---
					// меняем его на число.
					if (NUMBER.matcher(buf.trim()).matches())
						resultCell.setCellValue(Double.parseDouble(buf));
					else
						resultCell.setCellValue(buf);
					break;
				// Остальные типы ячеек пока игнорируем
				}
			}
		}

		// Разбираемся с merged-ячейками
		int mr = activeTemplateSheet.getNumMergedRegions();
		for (int i = 0; i < mr; i++) {
			// Диапазон смёрдженных ячеек на листе шаблона
			RangeAddress ra = new RangeAddress(activeTemplateSheet
					.getMergedRegion(i).formatAsString());

			if (!(ra.top() >= range.top() && ra.bottom() <= range.bottom()
					&& ra.left() >= range.left() && ra.right() <= range.right()))
				continue;

			int ydiff = -range.top() + growthPoint.getRow() - 1;
			int firstRow = ra.top() + ydiff;
			int lastRow = ra.bottom() + ydiff;

			int xdiff = -range.left() + growthPoint.getCol() - 1;
			int firstCol = ra.left() + xdiff;
			int lastCol = ra.right() + xdiff;
			CellRangeAddress res = new CellRangeAddress(firstRow, lastRow,
					firstCol, lastCol);

			activeResultSheet.addMergedRegion(res);
		}
	}

	@Override
	public void flush() throws XML2SpreadSheetError {
		try {
			result.write(getOutput());
		} catch (IOException e) {
			throw new XML2SpreadSheetError(e.getMessage());
		}
	}
}
