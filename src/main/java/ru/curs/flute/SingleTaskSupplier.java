package ru.curs.flute;

public abstract class SingleTaskSupplier extends TaskSource {

	private String script;
	private String params;

	@Override
	FluteTask getTask() {
		return new FluteTask(this, 0, script, params);
	}

	void setScript(String script) {
		this.script = script;
	}

	void setParams(String params) {
		this.params = params;
	}

	String getScript() {
		return script;
	}

	String getParams() {
		return params;
	}

}