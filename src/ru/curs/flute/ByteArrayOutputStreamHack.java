package ru.curs.flute;

import java.io.ByteArrayOutputStream;

/**
 * Хак стандартного класса ByteArrayOutputStream, открывающий внутренний буфер.
 * Служит для того, чтобы избегать лишнего вызова toByteArray, когда нужно
 * записать данные, содержащиеся в буфере, в поток.
 * 
 */
public class ByteArrayOutputStreamHack extends ByteArrayOutputStream {
	byte[] getBuffer() {
		return buf;
	}
}
