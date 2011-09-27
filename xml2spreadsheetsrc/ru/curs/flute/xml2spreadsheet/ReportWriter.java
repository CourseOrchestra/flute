package ru.curs.flute.xml2spreadsheet;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Класс, ответственный за формирование результирующего вывода в табличный
 * документ.
 */
abstract class ReportWriter {

	/**
	 * "Точка роста" -- ячейка, в которую предполагается копировать следующий
	 * элемент.
	 */
	private final CellAddress growthPoint = new CellAddress("A1");

	private OutputStream output;

	private static class LayoutBlock {

		/**
		 * true, если раскладка элементов в блоке идёт слева направо.
		 */
		private boolean horizontal;
		/**
		 * Заполненные границы блока, null, если блок пустой.
		 */
		private RangeAddress borders;

	}

	private final Deque<LayoutBlock> blocks = new LinkedList<LayoutBlock>();

	/**
	 * Создаёт форматировщик на основе шаблона и типа форматировщика.
	 * 
	 * @param template
	 *            Шаблон
	 * @param type
	 *            Тип форматировщика
	 * @param output
	 *            Поток для вывода результата
	 */
	static ReportWriter createWriter(InputStream template, OutputType type,
			OutputStream output) {

		ReportWriter result;
		switch (type) {
		case ODS:
			result = new ODSReportWriter(template);
			break;
		case XLS:
			result = new XLSReportWriter(template);
			break;
		case XLSX:
			result = new XLSXReportWriter(template);
			break;
		default:
			// This will never happen
			return null;
		}
		result.output = output;
		return result;
	}

	/**
	 * Создаёт новый лист электронной таблицы, копирует ширины столбцов из
	 * исходного листа и переводит позицию вывода в левый верхний угол.
	 * 
	 * @param sheetName
	 *            Имя создаваемого листа
	 * @param sourceSheet
	 *            Имя листа шаблона, с которого будут копироваться ширины
	 *            столбцов
	 */
	public void sheet(String sheetName, String sourceSheet) {
		newSheet(sheetName, sourceSheet);

		blocks.clear();
		blocks.push(new LayoutBlock());

		growthPoint.setAddress("A1");
	}

	/**
	 * Начинает группу элементов, запоминая текущую позицию и режим вывода
	 * (сверху вниз или слева направо).
	 * 
	 * @param horizontal
	 *            true для режима слева направо, false для режима сверху вниз.
	 */
	public void startSequence(boolean horizontal) {
		// На вершине стека должен находиться как минимум диапазон листа. Если
		// пользователь не создал листа, делаем это за него.
		if (blocks.isEmpty())
			sheet("Sheet1", null);

		LayoutBlock b = new LayoutBlock();
		b.horizontal = horizontal;

		blocks.push(b);
	}

	/**
	 * Оканчивает группу элементов, устанавливает текущую позицию вывода в
	 * зависимости от режима охватывающей группы: например, если до этого группа
	 * элементов выводилась слева направо, а охватывающая группа --- сверху
	 * вниз, то позиция смещается в столбец A, и строку, определяемую высотой
	 * самого высокого элемента в группе.
	 */
	public void endSequence() {
		LayoutBlock b2 = blocks.pop();
		// Если в группе так ничего и не было выведено или лист кончился ---
		// ничего и не делаем
		if (b2.borders == null || blocks.isEmpty())
			return;

		// Иначе, мы вставили прямоугольник, и его следует оприходовать, как
		// большую секцию:
		LayoutBlock b = blocks.peek();

		// Устанавливаем или расширяем границы текущего блока
		if (b.borders == null)
			b.borders = b2.borders;
		else {
			RangeAddress curRange = b.borders;
			curRange.setRight(Math.max(b2.borders.right(), curRange.right()));
			curRange.setBottom(Math.max(b2.borders.bottom(), curRange.bottom()));
		}

		// И устанавливаем точку роста в зависимости от режима раскладки
		updateGrowthPoint(b);
	}

	/**
	 * Выводит новую секцию отчёта, в данном контексте и базируясь на данном
	 * диапазоне ячеек шаблона. Позиция новой секции определяется режимом
	 * вывода, заданном вызовом startSequence.
	 * 
	 * @param context
	 *            Контекст данных
	 * @param sourceSheet
	 *            Лист шаблона, с которого скопировать заполненный данными
	 *            диапазон
	 * @param range
	 *            диапазон листа шаблона, который надо скопировать, заполнив
	 *            данными
	 * @throws XML2SpreadSheetError
	 *             Если что-то пошло не так.
	 */
	public void section(XMLContext context, String sourceSheet,
			RangeAddress range) throws XML2SpreadSheetError {
		// На вершине стека должен находиться как минимум диапазон листа. Если
		// пользователь не создал листа, делаем это за него.
		if (blocks.isEmpty())
			sheet("Sheet1", null);

		// Укладываем секцию на лист
		putSection(context, growthPoint, sourceSheet, range);

		// Мы только что вставили прямоугольник.
		// Его ЛВУ является growthPoint, а ПНУ сейчас вычислим:
		CellAddress bottomRight = new CellAddress(growthPoint.getCol()
				+ range.right() - range.left(), growthPoint.getRow()
				+ range.bottom() - range.top());

		LayoutBlock b = blocks.peek();
		// Устанавливаем или расширяем границы текущего блока

		if (b.borders == null) {
			b.borders = new RangeAddress(growthPoint.getAddress() + ":"
					+ bottomRight.getAddress());
		} else {
			RangeAddress curRange = b.borders;
			curRange.setRight(Math.max(bottomRight.getCol(), curRange.right()));
			curRange.setBottom(Math.max(bottomRight.getRow(), curRange.bottom()));
		}

		// И устанавливаем точку роста в зависимости от режима раскладки
		updateGrowthPoint(b);
	}

	private void updateGrowthPoint(LayoutBlock b) {
		RangeAddress curRange = b.borders;
		if (b.horizontal) {
			growthPoint.setCol(curRange.right() + 1);
			growthPoint.setRow(curRange.top());
		} else {
			growthPoint.setCol(curRange.left());
			growthPoint.setRow(curRange.bottom() + 1);
		}
	}

	final OutputStream getOutput() {
		return output;
	}

	abstract void newSheet(String sheetName, String sourceSheet);

	abstract void putSection(XMLContext context, CellAddress growthPoint2,
			String sourceSheet, RangeAddress range);
}
