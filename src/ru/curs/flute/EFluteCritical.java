package ru.curs.flute;

/**
 * Критическое исключение, приводящее к остановке работы приложения.
 * 
 */
public class EFluteCritical extends Exception {
	private static final long serialVersionUID = -1715303149623768585L;

	public EFluteCritical(String message) {
		super(message);
	}

}
