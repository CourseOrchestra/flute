package ru.curs.flute.exception;

public class EFluteNonCritical extends Exception {
	private static final long serialVersionUID = 1L;

	public EFluteNonCritical(String message) {
		super(message);
	}

	public EFluteNonCritical(String message, Throwable cause) {
		super(message, cause);
	}
}
