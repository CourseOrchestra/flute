package ru.curs.flute.fastxl;

/**
 * Ошибка выполнения процедуры, не приводящая к остановке приложения.
 * 
 */
public class EFastXLRuntime extends Exception {
	private static final long serialVersionUID = 2747877507823456310L;

	public EFastXLRuntime(String string) {
		super(string);
	}
}
