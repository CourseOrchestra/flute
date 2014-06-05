package ru.curs.flute.webrunner;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Закольцованный синхронизированный поток.
 */
public class ConsoleStream extends OutputStream {
	private static final int BYTE_MASK = 0xFF;
	private static final int DEFAULT_CAPACITY = 1024;
	/**
	 * Величина буфера.
	 */
	private final int bufferCapacity;
	/**
	 * Собственно буфер.
	 */
	private final byte[] buf;

	/**
	 * Текущая позиция, на которую будет записываться новое значение в буфер.
	 * Если буфер не полон, то это по совместительству длина буфера.
	 */
	private int pos = 0;

	private boolean full;

	public ConsoleStream() {
		this(DEFAULT_CAPACITY);
	}

	public ConsoleStream(int capacity) {
		buf = new byte[capacity];
		bufferCapacity = capacity;
	}

	@Override
	public synchronized void write(int b) throws IOException {
		// System.out.print((char) b);
		buf[pos] = (byte) (b & BYTE_MASK);
		pos++;
		if (pos == bufferCapacity) {
			pos = 0;
			full = true;
		}
	}

	/**
	 * Очищает содержимое буфера.
	 */
	public synchronized void clear() {
		pos = 0;
		full = false;
	}

	/**
	 * Представляет содержимое буфера в виде строки.
	 * 
	 * @param encoding
	 *            кодировка.
	 * @return содержимое буфера
	 * @throws UnsupportedEncodingException
	 *             неверная кодировка.
	 */
	public synchronized String asString(String encoding)
			throws UnsupportedEncodingException {
		String result;
		if (full) {
			result = new String(buf, pos, bufferCapacity - pos, encoding)
					+ new String(buf, 0, pos, encoding);
		} else {
			result = new String(buf, 0, pos, encoding);
		}
		return result;
	}
}
