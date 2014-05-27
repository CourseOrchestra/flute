/*
   (с) 2013 ООО "КУРС-ИТ"  

   Этот файл — часть КУРС:Flute.
   
   КУРС:Flute — свободная программа: вы можете перераспространять ее и/или изменять
   ее на условиях Стандартной общественной лицензии GNU в том виде, в каком
   она была опубликована Фондом свободного программного обеспечения; либо
   версии 3 лицензии, либо (по вашему выбору) любой более поздней версии.

   Эта программа распространяется в надежде, что она будет полезной,
   но БЕЗО ВСЯКИХ ГАРАНТИЙ; даже без неявной гарантии ТОВАРНОГО ВИДА
   или ПРИГОДНОСТИ ДЛЯ ОПРЕДЕЛЕННЫХ ЦЕЛЕЙ. Подробнее см. в Стандартной
   общественной лицензии GNU.

   Вы должны были получить копию Стандартной общественной лицензии GNU
   вместе с этой программой. Если это не так, см. http://www.gnu.org/licenses/.

   
   Copyright 2013, COURSE-IT Ltd.

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see http://www.gnu.org/licenses/.

 */
package ru.curs.flute;

import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import ru.curs.celesta.Celesta;
import ru.curs.celesta.CelestaException;
import ru.curs.celesta.ConnectionPool;
import ru.curs.celesta.dbutils.BLOB;

/**
 * Класс обработчика заданий. Одновременно запускается несколько обработчиков.
 * 
 */
public abstract class PythonProcessor extends Thread {

	private TaskParams task;

	@Override
	public void run() {
		try {
			String message = internalRun();
			finish(true, message == null ? "" : message);
		} catch (EFluteRuntime e) {
			finish(false, e.getMessage());
		}

	}

	private String internalRun() throws EFluteRuntime {

		StringWriter sw = new StringWriter();
		if (task.getParams() != null)
			try {
				Transformer tr = javax.xml.transform.TransformerFactory
						.newInstance().newTransformer();
				tr.setOutputProperty(OutputKeys.ENCODING, "UTF-16");

				tr.transform(new DOMSource(task.getParams()), new StreamResult(
						sw));
			} catch (TransformerException e) {
				throw new EFluteRuntime(
						"Error while processing XML parameters: "
								+ e.getMessage() + " for task " + task.getId());
			}
		else if (task.getStrParams() != null)
			sw.append(task.getStrParams());

		ProcParams params = new ProcParams(task.getId(), sw.toString(), task
				.getBLOB().getOutStream());

		try {
			Celesta.getInstance().runPython(AppSettings.getFluteUserId(),
					task.getScriptName(), params);
			return params.getMessage();
		} catch (CelestaException e) {
			throw new EFluteRuntime(String.format("Celesta error: %s",
					e.getMessage()));
		}
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
			Connection conn = ConnectionPool.get();
			finalizeTaskStmt = conn
					.prepareStatement(String
							.format("UPDATE %s SET STATUS = ?, result = ?, errortext = ? WHERE ID = ?",
									AppSettings.getTableName()));
			try {
				finalizeTaskStmt.setInt(1, success ? 2 : 3);
				BLOB blob = task.getBLOB();
				if (blob.isNull()) {
					finalizeTaskStmt.setNull(2, java.sql.Types.NULL);
				} else {
					finalizeTaskStmt.setBinaryStream(2, blob.getInStream(),
							blob.size());
				}

				finalizeTaskStmt.setString(3, details);
				finalizeTaskStmt.setInt(4, task.getId());
				finalizeTaskStmt.executeUpdate();
			} finally {
				finalizeTaskStmt.close();
			}
			ConnectionPool.commit(conn);
			ConnectionPool.putBack(conn);
			if (!success)
				AppSettings
						.getLogger()
						.log(Level.WARNING,
								String.format(
										"Task %d for template '%s' failed with message: %s\n",
										task.getId(), task.getScriptName(),
										details));

		} catch (SQLException | CelestaException e) {
			// Перевыбросить эксепшн в этом контексте сделать нельзя...
			AppSettings.getLogger()
					.log(Level.SEVERE,
							"Could not finalize task with exception: "
									+ e.getMessage());
		}

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
