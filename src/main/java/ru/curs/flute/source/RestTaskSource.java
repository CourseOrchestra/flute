package ru.curs.flute.source;

import org.springframework.stereotype.Component;

import ru.curs.flute.exception.EFluteCritical;
import ru.curs.flute.source.TaskSource;
import ru.curs.flute.task.FluteTask;
import ru.curs.flute.task.TaskUnit;

@Component
public final class RestTaskSource extends TaskSource {
	
	private final RestFluteTask taskInstance = new RestFluteTask(this, 0, new TaskUnit("", TaskUnit.Type.SCRIPT), "");

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
	public RestFluteTask(RestTaskSource ts, int id, TaskUnit taskUnit, String params) {
		super(ts, id, taskUnit, params);
	}
}
