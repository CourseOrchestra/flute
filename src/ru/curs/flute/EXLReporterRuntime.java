package ru.curs.flute;

/**
 * Ошибка выполнения процедуры, не приводящая к остановке приложения.
 * 
 */
public class EXLReporterRuntime extends Exception {
	private static final long serialVersionUID = 2747877507823456310L;

	public EXLReporterRuntime(String string) {
		super(string);
	}
}
