package ru.curs.flute.fastxl;

/**
 * Ошибка выполнения процедуры создания отчёта.
 * 
 */
public class EFastXLRuntime extends Exception {
	private static final long serialVersionUID = 2747877507823456310L;

	public EFastXLRuntime(String string) {
		super(string);
	}
}
