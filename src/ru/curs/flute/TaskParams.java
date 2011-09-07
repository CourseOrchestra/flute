package ru.curs.flute;

import java.io.OutputStream;

import org.w3c.dom.Document;

/**
 * Содержит в себе параметры задания для выполнения.
 * 
 */
public class TaskParams {
	private final int id;
	private final String template;
	private final Document params;

	private final ByteArrayOutputStreamHack bos = new ByteArrayOutputStreamHack();

	TaskParams(int id, String template, Document params) {
		this.id = id;
		this.template = template;
		this.params = params;
	}

	/**
	 * Идентификатор задания.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Шаблон задания.
	 */
	public String getTemplate() {
		return template;
	}

	/**
	 * Параметры задания.
	 */
	public Document getParams() {
		return params;
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
