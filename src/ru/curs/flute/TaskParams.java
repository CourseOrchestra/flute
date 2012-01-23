package ru.curs.flute;

import java.io.OutputStream;

import org.w3c.dom.Document;

/**
 * Содержит в себе параметры задания для выполнения.
 * 
 */
public class TaskParams {
	private final int id;
	private final String scriptName;
	private final Document params;
	private final String strParams;

	private final ByteArrayOutputStreamHack bos = new ByteArrayOutputStreamHack();

	TaskParams(int id, String scriptName, Document params, String str) {
		this.id = id;
		this.scriptName = scriptName;
		this.params = params;
		this.strParams = str;
	}

	/**
	 * Идентификатор задания.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Скрипт задания (имя .py-файла).
	 */
	public String getScriptName() {
		return scriptName;
	}

	/**
	 * Параметры задания.
	 */
	public Document getParams() {
		return params;
	}

	/**
	 * Параметры задания (в строковом формате).
	 */
	public String getStrParams() {
		return strParams;
	}

	/**
	 * Возвращает поток для записи данных.
	 */
	public OutputStream getOutStream() {
		return bos;
	}

	/**
	 * Возвращает массив байт, содержащий сформированный результат.
	 */
	public byte[] getBuffer() {
		return bos.getBuffer();
	}

	/**
	 * Возвращает активную длину буфера, возвращённого методом getBuffer.
	 */
	public int getBufferLength() {
		return bos.size();
	}

}
