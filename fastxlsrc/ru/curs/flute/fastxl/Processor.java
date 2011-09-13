package ru.curs.flute.fastxl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;

/**
 * Класс обработчика заданий. Одновременно запускается несколько обработчиков.
 * 
 */
public class Processor {

	private static final Pattern SHEET_NAME = Pattern.compile(
			"xl/worksheets/sheet[0-9]+.xml", Pattern.CASE_INSENSITIVE);

	// Параметры задания
	private final Connection conn;
	private final String templatePath;
	private final Document xmlParams;
	private final OutputStream resultStream;

	private static class ByteArrayOutputStreamHack extends
			ByteArrayOutputStream {
		byte[] getBuffer() {
			return buf;
		}
	}

	private final byte buffer[] = new byte[2048];

	public Processor(Connection conn, String templatePath, Document xmlParams,
			OutputStream resultStream) {
		this.conn = conn;
		this.templatePath = templatePath;
		this.xmlParams = xmlParams;
		this.resultStream = resultStream;
	}

	/**
	 * Выполняет обработку задания по формирования xlsx-файла.
	 * 
	 * @throws EFastXLRuntime
	 *             В случае, если что-то пошло не так.
	 */
	public void execute() throws EFastXLRuntime {
		final File f = new File(templatePath);
		if (!f.exists())
			throw new EFastXLRuntime("Report template file " + f
					+ " does not exist!");

		// ФАЗА 1: Чтение шита.
		XLSharedStrings sharedStrings = null;
		final HashMap<String, WorksheetProcessor> wsProcs = new HashMap<String, WorksheetProcessor>();
		final HashMap<Integer, StatementProducer> placeholders = new HashMap<Integer, StatementProducer>();
		FileInputStream fis = getTemplateInputStream(f);
		try {
			ZipInputStream zis = new ZipInputStream(fis);
			ZipEntry ientry;
			try {
				while ((ientry = zis.getNextEntry()) != null) {
					if ("xl/sharedStrings.xml".equalsIgnoreCase(ientry
							.getName())) {
						ByteArrayOutputStreamHack bos = readZis(zis);
						sharedStrings = new XLSharedStrings(
								new ByteArrayInputStream(bos.getBuffer(), 0,
										bos.size())) {
							@Override
							protected void validateAddedString(int index,
									String value) throws EFastXLRuntime {
								StatementProducer sp;
								try {
									sp = StatementProducer.validate(value,
											xmlParams);
								} catch (XPathExpressionException e) {
									throw new EFastXLRuntime(
											"Wrong XPath expression in " + f
													+ ": " + e.getMessage());
								}
								if (sp != null)
									placeholders.put(index, sp);
							}
						};
					} else if (SHEET_NAME.matcher(ientry.getName()).matches()) {
						ByteArrayOutputStreamHack bos = readZis(zis);
						wsProcs.put(ientry.getName(), new WorksheetProcessor(
								new ByteArrayInputStream(bos.getBuffer(), 0,
										bos.size()), conn));
					}
				}
			} catch (IOException e) {
				throw new EFastXLRuntime(
						"I/O Exception while decompressing " + f + ": "
								+ e.getMessage());
			}
		} finally {
			try {
				fis.close();
			} catch (IOException e) {
				throw new EFastXLRuntime("Failed to close template: "
						+ e.getMessage());
			}
		}

		// ФАЗА 2: Трансформация, в ходе которой мутирует SharedStrings
		if (wsProcs.isEmpty())
			new EFastXLRuntime("Template " + f
					+ "has wrong format: no worksheet found.");
		if (sharedStrings == null)
			throw new EFastXLRuntime("Template " + f
					+ "has wrong format: no shared strings found.");

		final HashMap<String, ByteArrayOutputStreamHack> resultSheets = new HashMap<String, ByteArrayOutputStreamHack>(
				wsProcs.size());
		for (Entry<String, WorksheetProcessor> e : wsProcs.entrySet()) {
			ByteArrayOutputStreamHack bos = new ByteArrayOutputStreamHack();
			e.getValue().transform(bos, sharedStrings, placeholders);
			resultSheets.put(e.getKey(), bos);
		}
		// Больше процессоры страниц не нужны, они своё дело сделали...
		wsProcs.clear();

		// ФАЗА 3: Запись готового файла.
		fis = getTemplateInputStream(f);
		try {
			ZipInputStream zis = new ZipInputStream(fis);
			OutputStream os = resultStream;
			ZipOutputStream zos = new ZipOutputStream(os);
			ZipEntry ientry;
			try {
				while ((ientry = zis.getNextEntry()) != null) {
					ZipEntry oentry = new ZipEntry(ientry.getName());
					zos.putNextEntry(oentry);
					if ("xl/sharedStrings.xml".equals(ientry.getName())) {
						ByteArrayOutputStreamHack bos = new ByteArrayOutputStreamHack();
						sharedStrings.saveXML(bos);
						zos.write(bos.getBuffer(), 0, bos.size());
					} else if (SHEET_NAME.matcher(ientry.getName()).matches()) {
						ByteArrayOutputStreamHack bos = resultSheets.get(ientry
								.getName());
						// Нуллом bos быть не может --- мы же тот же самый zip
						// второй раз читаем...
						zos.write(bos.getBuffer(), 0, bos.size());
					} else {
						ByteArrayOutputStreamHack bos = readZis(zis);
						zos.write(bos.getBuffer(), 0, bos.size());
					}
					zos.closeEntry();
				}
				zos.finish();
			} catch (IOException e) {
				throw new EFastXLRuntime("I/O Exception while repacking "
						+ f + ": " + e.getMessage());
			}
		} finally {
			try {
				fis.close();
			} catch (IOException e) {
				throw new EFastXLRuntime("Failed to close template: "
						+ e.getMessage());
			}
		}

	}

	private ByteArrayOutputStreamHack readZis(ZipInputStream zis)
			throws IOException {
		ByteArrayOutputStreamHack os = new ByteArrayOutputStreamHack();
		int bytesRead;
		while ((bytesRead = zis.read(buffer)) != -1)
			os.write(buffer, 0, bytesRead);
		return os;
	}

	private FileInputStream getTemplateInputStream(File f)
			throws EFastXLRuntime {
		FileInputStream fis;
		try {
			fis = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			throw new EFastXLRuntime(e.getMessage());
		}
		return fis;
	}

}
