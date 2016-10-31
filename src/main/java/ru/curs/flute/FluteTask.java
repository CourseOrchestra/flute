package ru.curs.flute;

import java.io.OutputStream;

import ru.curs.celesta.dbutils.BLOB;

/**
 * Encapsulates Flute task in/out parameters.
 */
public class FluteTask implements Runnable {

	private final TaskSource ts;
	private Throwable error;

	private FluteTaskState state = FluteTaskState.NEW;

	private final String script;

	private final String params;

	private String message;

	private final BLOB blob = new BLOB();

	public FluteTask(TaskSource ts, String script, String params) {
		this.ts = ts;
		this.script = script;
		this.params = params;
	}

	public TaskSource getSource() {
		return ts;
	}

	public FluteTaskState getState() {
		return state;
	}

	public void setState(FluteTaskState newState) {
		state = newState;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getScript() {
		return script;
	}

	public String getParams() {
		return params;
	}

	public OutputStream getResultstream() {
		return blob.getOutStream();
	}

	public Throwable getError() {
		return error;
	}

	protected void doJob() throws InterruptedException, EFluteNonCritical {
		ts.process(this);
	}

	@Override
	public void run() {
		state = FluteTaskState.INPROCESS;
		try {
			doJob();
			state = FluteTaskState.SUCCESS;
		} catch (InterruptedException e) {
			state = FluteTaskState.INTERRUPTED;
		} catch (Throwable e) {
			state = FluteTaskState.FAIL;
			error = e;
			message = e.getMessage();
		} finally {
			ts.release();
		}
	}

}
