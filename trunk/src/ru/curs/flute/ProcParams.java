package ru.curs.flute;

import java.io.OutputStream;

/**
 * Параметры процедуры.
 */
public class ProcParams {
	private final int taskid;
	private final String params;
	private final OutputStream resultstream;
	private String message;

	public ProcParams(int taskid, String params, OutputStream resultstream) {
		this.taskid = taskid;
		this.params = params;
		this.resultstream = resultstream;
	}

	/**
	 * Сообщение.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Устанавливает сообщение.
	 * 
	 * @param message
	 *            сообщение.
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Номер задания.
	 */
	public int getTaskid() {
		return taskid;
	}

	/**
	 * Параметры задания.
	 */
	public String getParams() {
		return params;
	}

	/**
	 * Результирующий поток для сохранения результатов задания.
	 */
	public OutputStream getResultstream() {
		return resultstream;
	}

}
