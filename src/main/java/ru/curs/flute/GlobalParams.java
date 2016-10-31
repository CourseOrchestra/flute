package ru.curs.flute;

public interface GlobalParams {

	/**
	 * Time to wait after critical error to continue execution.
	 */
	int getRetryWait();

	/**
	 * If true, Flute will sleep for 'retryWait' milliseconds and will try to
	 * continue execution even after a critical error.
	 */
	boolean isNeverStop();

	/**
	 * The user under which Flute is logged in Celesta.
	 */
	String getFluteUserId();

}
