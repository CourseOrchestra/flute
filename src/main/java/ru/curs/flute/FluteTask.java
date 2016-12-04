package ru.curs.flute;

import java.io.OutputStream;

import redis.clients.jedis.Jedis;
import ru.curs.celesta.dbutils.BLOB;

/**
 * Encapsulates Flute task in/out parameters.
 */
public class FluteTask implements Runnable {

	private final TaskSource ts;
	private final String script;
	private final String params;
	private final int id;

	private Throwable error;
	private FluteTaskState state = FluteTaskState.NEW;
	private String message;

	private final BLOB blob = new BLOB();

	public FluteTask(TaskSource ts, int id, String script, String params) {
		this.ts = ts;
		this.id = id;
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
		ts.changeTaskState(this);
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

	public int getId() {
		return id;
	}

	public OutputStream getResultstream() {
		return blob.getOutStream();
	}

	public Jedis getJedis() {
		return ts.getJedisPool().getResource();
	}

	BLOB getBLOB() {
		return blob;
	}

	public Throwable getError() {
		return error;
	}

	protected void doJob() throws InterruptedException, EFluteNonCritical {
		ts.process(this);
	}

	@Override
	public void run() {
		setState(FluteTaskState.INPROCESS);
		try {
			doJob();
			setState(FluteTaskState.SUCCESS);
		} catch (InterruptedException e) {
			setState(FluteTaskState.INTERRUPTED);
		} catch (Throwable e) {
			setState(FluteTaskState.FAIL);
			error = e;
			message = e.getMessage();
			System.err.printf("Task failed: %s%n", e.getMessage());
		} finally {
			ts.release();
		}
	}

}
