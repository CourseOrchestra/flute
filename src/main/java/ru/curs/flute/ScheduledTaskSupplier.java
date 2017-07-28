package ru.curs.flute;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.sauronsoftware.cron4j.Scheduler;

@Component
@Scope("prototype")
public class ScheduledTaskSupplier extends SingleTaskSupplier {

	private final Scheduler scheduler = new Scheduler();

	private final BlockingQueue<FluteTask> queue = new LinkedBlockingQueue<>();

	private String schedule;

	private String taskId = null;

	public void setSchedule(String schedule) {
		this.schedule = schedule;
		if (taskId == null) {
			taskId = scheduler.schedule(schedule, () -> {
				try {
					FluteTask t = super.getTask();
					internalAdd(t);
				} catch (Exception e) {
					// this will not happen
				}
			});
		} else {
			scheduler.reschedule(taskId, schedule);
		}
	}

	void internalAdd(FluteTask t) throws InterruptedException {
		queue.put(t);
	}

	@Override
	FluteTask getTask() throws InterruptedException {
		try {
			return queue.take();
		} catch (InterruptedException e) {
			if (taskId != null)
				scheduler.deschedule(taskId);
			throw e;
		}
	}

	public String getSchedule() {
		return schedule;
	}

	@Override
	public void run() {
		scheduler.start();
		super.run();
	}

}
