package ru.curs.flute;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class CRONTasksSupplier extends TaskSource {

	private String schedule;
	private String script;
	private String params;

	@Override
	FluteTask getTask() throws InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public void setParams(String params) {
		this.params = params;
	}

	public String getSchedule() {
		return schedule;
	}

	public String getScript() {
		return script;
	}

	public String getParams() {
		return params;
	}

}
