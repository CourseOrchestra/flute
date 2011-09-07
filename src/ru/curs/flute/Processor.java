package ru.curs.flute;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.xpath.XPathExpressionException;

/**
 * Класс обработчика заданий. Одновременно запускается несколько обработчиков.
 * 
 */
public abstract class Processor extends Thread {

	private static final Pattern SHEET_NAME = Pattern.compile(
			"xl/worksheets/sheet[0-9]+.xml", Pattern.CASE_INSENSITIVE);

	private TaskParams task;
	private Connection conn;

	private final byte buffer[] = new byte[2048];

	@Override
	public void run() {
		try {
			initConn();
			internalRun();
			finish(true, "");
		} catch (EXLReporterRuntime e) {
			finish(false, e.getMessage());
		}

	}

	private void initConn() throws EXLReporterRuntime {
		try {
			if (conn == null || conn.isClosed())
				conn = ConnectionPool.get();
		} catch (SQLException e) {
			throw new EXLReporterRuntime("Could not connect to "
					+ AppSettings.getDatabaseConnection() + "with error: "
					+ e.getMessage());
		} catch (EXLReporterCritical e) {
			throw new EXLReporterRuntime(e.getMessage());
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

	void internalRun() throws EXLReporterRuntime {
		final File f = new File(AppSettings.getTemplatesPath() + File.separator
				+ task.getTemplate());
		if (!f.exists())
			throw new EXLReporterRuntime("Report template file " + f
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
									String value) throws EXLReporterRuntime {
								StatementProducer sp;
								try {
									sp = StatementProducer.validate(value,
											task.getParams());
								} catch (XPathExpressionException e) {
									throw new EXLReporterRuntime(
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
				throw new EXLReporterRuntime(
						"I/O Exception while decompressing " + f + ": "
								+ e.getMessage());
			}
		} finally {
			try {
				fis.close();
			} catch (IOException e) {
				throw new EXLReporterRuntime("Failed to close template: "
						+ e.getMessage());
			}
		}

		// ФАЗА 2: Трансформация, в ходе которой мутирует SharedStrings
		if (wsProcs.isEmpty())
			new EXLReporterRuntime("Template " + f
					+ "has wrong format: no worksheet found.");
		if (sharedStrings == null)
			throw new EXLReporterRuntime("Template " + f
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
			OutputStream os = task.getOutStream();
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
				throw new EXLReporterRuntime("I/O Exception while repacking "
						+ f + ": " + e.getMessage());
			}
		} finally {
			try {
				fis.close();
			} catch (IOException e) {
				throw new EXLReporterRuntime("Failed to close template: "
						+ e.getMessage());
			}
		}
		AppSettings.getLogger().log(
				Level.INFO,
				String.format(
						"Task id = %d, template '%s' processed by thread %d\n",
						task.getId(), task.getTemplate(), this.getId()));
	}

	private FileInputStream getTemplateInputStream(File f)
			throws EXLReporterRuntime {
		FileInputStream fis;
		try {
			fis = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			throw new EXLReporterRuntime(e.getMessage());
		}
		return fis;
	}

	/**
	 * Завершение процедуры обработки.
	 * 
	 * @param success
	 *            true, если завершилось без ошибок.
	 * @param details
	 *            Строка с дополнительным сообщением --- например, с деталями
	 *            произошедшей ошибки.
	 */
	private void finish(boolean success, String details) {

		PreparedStatement finalizeTaskStmt;

		try {
			finalizeTaskStmt = conn
					.prepareStatement(String
							.format("UPDATE %s SET STATUS = ?, result = ?, errortext = ? WHERE ID = ?",
									AppSettings.getTableName()));

			finalizeTaskStmt.setInt(1, success ? 2 : 3);

			if (task.getBufferLength() == 0)
				finalizeTaskStmt.setNull(2, java.sql.Types.BLOB);
			else
				finalizeTaskStmt.setBinaryStream(2, new ByteArrayInputStream(
						task.getBuffer(), 0, task.getBufferLength()));

			finalizeTaskStmt.setString(3, details);
			finalizeTaskStmt.setInt(4, task.getId());
			finalizeTaskStmt.execute();

			if (!success)
				AppSettings
						.getLogger()
						.log(Level.WARNING,
								String.format(
										"Task %d for template '%s' failed with message: %s\n",
										task.getId(), task.getTemplate(),
										details));
		} catch (SQLException e) {
			// Перевыбросить эксепшн в этом контексте сделать нельзя...
			AppSettings.getLogger()
					.log(Level.SEVERE,
							"Could not finalize task with exception: "
									+ e.getMessage());
		}

		ConnectionPool.putBack(conn);
		finish();
	}

	/**
	 * Завершение процедуры обработки.
	 */
	protected abstract void finish();

	/**
	 * Устанавливает параметры задания.
	 * 
	 * @param currentTask
	 *            Параметры задания
	 */
	public void setTask(TaskParams currentTask) {
		task = currentTask;
	}

	/**
	 * Возвращает параметры задания.
	 */
	final TaskParams getTask() {
		return task;
	}

}
