package ru.curs.flute.fastxl;

/**
 * Критическое исключение, приводящее к остановке работы приложения.
 * 
 */
public class EXLReporterCritical extends Exception {
	private static final long serialVersionUID = -1715303149623768585L;

	public EXLReporterCritical(String message) {
		super(message);
	}

}
