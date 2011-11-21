package ru.curs.flute.xml2spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;

abstract class POIReportWriter extends ReportWriter {

	/**
	 * Регексп для числа (в общем случае, с плавающей точкой --- целые числа
	 * также должны попадать под этот регексп.
	 */
	private static final Pattern NUMBER = Pattern
			.compile("[+-]?\\d+(\\.\\d+)?([eE][+-]?\\d+)?");
	private final Workbook template;
	private final Workbook result;
	private Sheet activeTemplateSheet;
	private Sheet activeResultSheet;
	private final Map<CellStyle, CellStyle> stylesMap = new HashMap<>();

	public POIReportWriter(InputStream template) throws XML2SpreadSheetError {
		try {
			this.template = WorkbookFactory.create(template);
		} catch (InvalidFormatException | IOException e) {
			throw new XML2SpreadSheetError(e.getMessage());
		}

		// Создаём новую книгу
		result = createResultWb();

		final Map<Short, Font> fontMap = new HashMap<>();

		// Копируем шрифты
		for (short i = 0; i < this.template.getNumberOfFonts(); i++) {
			Font fSource = this.template.getFontAt(i);
			Font fResult = result.createFont();
			fResult.setBoldweight(fSource.getBoldweight());
			// Для XLSX, похоже, не работает...
			if (this instanceof XLSReportWriter)
				fResult.setCharSet(fSource.getCharSet());
			fResult.setColor(fSource.getColor());
			fResult.setFontHeight(fSource.getFontHeight());
			fResult.setFontName(fSource.getFontName());
			fResult.setItalic(fSource.getItalic());
			fResult.setStrikeout(fSource.getStrikeout());
			fResult.setTypeOffset(fSource.getTypeOffset());
			fResult.setUnderline(fSource.getUnderline());
			fontMap.put(fSource.getIndex(), fResult);
		}

		DataFormat df = result.createDataFormat();

		// Копируем стили ячеек (cloneStyleFrom не работает для нас)
		for (short i = 0; i < this.template.getNumCellStyles(); i++) {

			CellStyle csSource = this.template.getCellStyleAt(i);
			CellStyle csResult = result.createCellStyle();

			csResult.setAlignment(csSource.getAlignment());
			csResult.setBorderBottom(csSource.getBorderBottom());
			csResult.setBorderLeft(csSource.getBorderLeft());
			csResult.setBorderRight(csSource.getBorderRight());
			csResult.setBorderTop(csSource.getBorderTop());
			csResult.setBottomBorderColor(csSource.getBottomBorderColor());
			csResult.setDataFormat(df.getFormat(csSource.getDataFormatString()));
			csResult.setFillBackgroundColor(csSource.getFillBackgroundColor());
			csResult.setFillForegroundColor(csSource.getFillForegroundColor());
			csResult.setFillPattern(csSource.getFillPattern());
			Font f = fontMap.get(csSource.getFontIndex());
			if (f != null)
				csResult.setFont(f);
			csResult.setHidden(csSource.getHidden());
			csResult.setIndention(csSource.getIndention());
			csResult.setLeftBorderColor(csSource.getLeftBorderColor());
			csResult.setLocked(csSource.getLocked());
			csResult.setRightBorderColor(csSource.getRightBorderColor());
			csResult.setRotation(csSource.getRotation());
			csResult.setTopBorderColor(csSource.getTopBorderColor());
			csResult.setVerticalAlignment(csSource.getVerticalAlignment());
			csResult.setWrapText(csSource.getWrapText());

			stylesMap.put(csSource, csResult);
		}
	}

	abstract Workbook createResultWb();

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
	protected void newSheet(String sheetName, String sourceSheet)
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
			activeResultSheet.setColumnWidth(i,
					activeTemplateSheet.getColumnWidth(i));

		// Переносим дефолтную высоту
		activeResultSheet.setDefaultRowHeight(activeTemplateSheet
				.getDefaultRowHeight());

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
	protected void putSection(XMLContext context, CellAddress growthPoint,
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

			if (sourceRow.getHeight() != activeTemplateSheet
					.getDefaultRowHeight())
				resultRow.setHeight(sourceRow.getHeight());

			for (int j = range.left(); j < Math.min(range.right(),
					sourceRow.getLastCellNum()) + 1; j++) {
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
					String val = sourceCell.getStringCellValue();
					String buf = context.calc(val);
					// Если ячейка содержит строковое представление числа и при
					// этом содержит плейсхолдер --- меняем его на число.
					if (NUMBER.matcher(buf.trim()).matches()
							&& context.containsPlaceholder(val))
						resultCell.setCellValue(Double.parseDouble(buf));
					else
						resultCell.setCellValue(buf);
					break;
				// Остальные типы ячеек пока игнорируем
				}
			}
		}

		// Разбираемся с merged-ячейками
		arrangeMergedCells(growthPoint, range);
	}

	private void arrangeMergedCells(CellAddress growthPoint, RangeAddress range)
			throws XML2SpreadSheetError {
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