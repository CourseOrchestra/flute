package ru.curs.flute;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.python.core.PyInteger;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;

/**
 * Класс обработчика заданий. Одновременно запускается несколько обработчиков.
 * 
 */
public abstract class PythonProcessor extends Thread {

	private TaskParams task;
	private Connection conn;

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

	void internalRun() throws EXLReporterRuntime {
		final File f = new File(AppSettings.getScriptsPath() + File.separator
				+ task.getScriptName());
		if (!f.exists())
			throw new EXLReporterRuntime("Report template file " + f
					+ " does not exist!");
		if (!f.canRead())
			throw new EXLReporterRuntime("Report template file " + f
					+ " cannot be read!");
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			// This should not happen here.
			e.printStackTrace();
		}

		StringWriter sw = new StringWriter();
		if (task.getParams() != null)
			try {
				Transformer tr = javax.xml.transform.TransformerFactory
						.newInstance().newTransformer();
				tr.transform(new DOMSource(task.getParams()), new StreamResult(
						sw));
			} catch (TransformerException e) {
				throw new EXLReporterRuntime(
						"Error while processing XML parameters: "
								+ e.getMessage() + " for task " + task.getId());
			}

		PythonInterpreter interp = new PythonInterpreter();
		interp.set("taskid", new PyInteger(task.getId()));
		interp.set("params", new PyString(sw.toString()));
		interp.execfile(fis);
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

			finalizeTaskStmt.setNull(2, java.sql.Types.BLOB);

			finalizeTaskStmt.setString(3, details);
			finalizeTaskStmt.setInt(4, task.getId());
			finalizeTaskStmt.execute();

			if (!success)
				AppSettings
						.getLogger()
						.log(Level.WARNING,
								String.format(
										"Task %d for template '%s' failed with message: %s\n",
										task.getId(), task.getScriptName(),
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

}
