package ru.curs.flute.source;

import org.springframework.stereotype.Component;

import ru.curs.flute.exception.EFluteCritical;
import ru.curs.flute.source.TaskSource;
import ru.curs.flute.task.FluteTask;

@Component
public final class RestTaskSource extends TaskSource {
	
	private final RestFluteTask taskInstance = new RestFluteTask(this, 0, "", "");

	@Override
	public void run() {
	}

	@Override
	public FluteTask getTask() throws InterruptedException, EFluteCritical {
		return taskInstance;
	}

	@Override
	public void release() {	
	}
}

final class RestFluteTask extends FluteTask {
	public RestFluteTask(RestTaskSource ts, int id, String script, String params) {
		super(ts, id, script, params);
	}
}
