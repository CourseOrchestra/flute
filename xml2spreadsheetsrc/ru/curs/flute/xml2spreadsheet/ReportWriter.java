package ru.curs.flute.xml2spreadsheet;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

/**
 * Класс, ответственный за формирование результирующего вывода в табличный
 * документ.
 */
abstract class ReportWriter {

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
	static ReportWriter createWriter(InputStream template,
			OutputType type, OutputStream output) {
		// TODO implement
		return null;
	}

	/**
	 * Создаёт новый лист электронной таблицы и переводит позицию вывода в левый
	 * верхний угол.
	 */
	public void sheet(String sheetName) {
		// TODO implement
	}

	/**
	 * Начинает группу элементов, запоминая текущую позицию и режим вывода
	 * (сверху вниз или слева направо).
	 * 
	 * @param horizontal
	 *            true для режима слева направо, false для режима сверху вниз.
	 */
	public void startSequence(boolean horizontal) {
		// TODO implement
	}

	/**
	 * Оканчивает группу элементов, устанавливает текущую позицию вывода в
	 * зависимости от режима охватывающей группы: например, если до этого группа
	 * элементов выводилась слева направо, а охватывающая группа --- сверху
	 * вниз, то позиция смещается в столбец A, и строку, определяемую высотой
	 * самого высокого элемента в группе.
	 */
	public void endSequence() {
		// TODO implement
	}

	/**
	 * Выводит новую секцию отчёта, в данном контексте и базируясь на данном
	 * диапазоне ячеек шаблона. Позиция новой секции определяется режимом
	 * вывода, заданном вызовом startSequence.
	 */
	public void section(XMLContext context, Random range) {
		// TODO implement
	}

}
