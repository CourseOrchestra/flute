package ru.curs.flute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Ignore;
import org.junit.Test;
import ru.curs.flute.source.QueueSource;
import ru.curs.flute.task.FluteTask;
import ru.curs.flute.task.FluteTaskState;
import ru.curs.flute.task.QueueTask;
import ru.curs.flute.task.TaskUnit;

class DummyFluteTask extends QueueTask {
	public DummyFluteTask(QueueSource ts1) {
		super(ts1, 0, new TaskUnit("", TaskUnit.Type.SCRIPT), "");
	}

	@Override
	protected void doJob() throws InterruptedException {
		Thread.sleep(200);
	}
}

class DummyFluteTask2 extends QueueTask {
	public DummyFluteTask2(QueueSource ts1) {
		super(ts1, 0, new TaskUnit("", TaskUnit.Type.SCRIPT), "");
	}

	@Override
	protected void doJob() throws InterruptedException {
		Thread.sleep(30);
		throw new IllegalStateException("foo");
	}
}

class DummyFluteSource extends QueueSource {
	private final BlockingQueue<QueueTask> q = new LinkedBlockingQueue<>();

	void addTask(QueueTask t) {
		q.add(t);
	}

	@Override
	public QueueTask getTask() {
		try {
			return q.take();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void changeTaskState(FluteTask t) {}
}

public class ThreadPoolTest {

	@Test
	@Ignore
	public void test() throws InterruptedException {
		// test task execution and semaphore

		DummyFluteSource ts1 = new DummyFluteSource();
		ts1.setMaxThreads(2);

		QueueTask[] tasks = new QueueTask[3];
		initTasks(ts1, tasks);

		for (int i = 0; i < tasks.length; i++)
			assertEquals(FluteTaskState.NEW, tasks[i].getState());

		for (int i = 0; i < tasks.length; i++)
			assertEquals(FluteTaskState.NEW, tasks[i].getState());

		ExecutorService es = Executors.newCachedThreadPool();
		es.execute(ts1);

		Thread.sleep(50);
		assertEquals(FluteTaskState.INPROCESS, tasks[0].getState());
		assertEquals(FluteTaskState.INPROCESS, tasks[1].getState());
		assertEquals(FluteTaskState.NEW, tasks[2].getState());

		Thread.sleep(300);
		assertEquals(FluteTaskState.SUCCESS, tasks[0].getState());
		assertEquals(FluteTaskState.SUCCESS, tasks[1].getState());
		assertEquals(FluteTaskState.INPROCESS, tasks[2].getState());
		Thread.sleep(200);
		assertEquals(FluteTaskState.SUCCESS, tasks[0].getState());
		assertEquals(FluteTaskState.SUCCESS, tasks[1].getState());
		assertEquals(FluteTaskState.SUCCESS, tasks[2].getState());

		tasks[0] = new DummyFluteTask(ts1);
		tasks[1] = new DummyFluteTask2(ts1);
		tasks[2] = new DummyFluteTask(ts1);
		ts1.addTask(tasks[0]);
		ts1.addTask(tasks[1]);
		ts1.addTask(tasks[2]);
		Thread.sleep(10);
		assertEquals(FluteTaskState.INPROCESS, tasks[0].getState());
		assertEquals(FluteTaskState.INPROCESS, tasks[1].getState());
		assertEquals(FluteTaskState.NEW, tasks[2].getState());
		Thread.sleep(300);
		assertEquals(FluteTaskState.SUCCESS, tasks[0].getState());
		assertEquals(FluteTaskState.FAIL, tasks[1].getState());
		org.junit.Assert.assertNotEquals(FluteTaskState.FAIL, tasks[2].getState());

		assertNull(tasks[0].getError());
		Throwable e = tasks[1].getError();
		assertNotNull(e);
		assertEquals("foo", e.getMessage());
		assertNull(tasks[2].getError());

	}

	private void initTasks(DummyFluteSource ts1, QueueTask[] tasks) {
		for (int i = 0; i < tasks.length; i++) {
			tasks[i] = new DummyFluteTask(ts1);
			ts1.addTask(tasks[i]);
		}
	}

	@Test
	@Ignore
	public void test2() throws InterruptedException {
		// test task cancellation
		DummyFluteSource ts1 = new DummyFluteSource();
		ts1.setMaxThreads(2);
		ts1.setTerminationTimeout(800);

		QueueTask[] tasks = new QueueTask[3];
		initTasks(ts1, tasks);
		ExecutorService es = Executors.newCachedThreadPool();
		es.execute(ts1);
		Thread.sleep(10);
		es.shutdownNow();
		assertEquals(FluteTaskState.INPROCESS, tasks[0].getState());
		assertEquals(FluteTaskState.INPROCESS, tasks[1].getState());
		assertEquals(FluteTaskState.NEW, tasks[2].getState());

		// here we are testing the fact that
		Thread.sleep(900);
		assertEquals(FluteTaskState.SUCCESS, tasks[0].getState());
		assertEquals(FluteTaskState.SUCCESS, tasks[1].getState());
		assertEquals(FluteTaskState.NEW, tasks[2].getState());
	}

	@Test
	@Ignore
	public void test3() throws InterruptedException {
		// test task cancellation
		DummyFluteSource ts1 = new DummyFluteSource();
		ts1.setMaxThreads(2);
		ts1.setTerminationTimeout(50);

		QueueTask[] tasks = new QueueTask[3];
		initTasks(ts1, tasks);
		ExecutorService es = Executors.newCachedThreadPool();
		es.execute(ts1);
		Thread.sleep(10);
		es.shutdownNow();
		assertEquals(FluteTaskState.INPROCESS, tasks[0].getState());
		assertEquals(FluteTaskState.INPROCESS, tasks[1].getState());
		assertEquals(FluteTaskState.NEW, tasks[2].getState());

		// here we are testing the fact that
		Thread.sleep(900);
		assertEquals(FluteTaskState.INTERRUPTED, tasks[0].getState());
		assertEquals(FluteTaskState.INTERRUPTED, tasks[1].getState());
		assertEquals(FluteTaskState.NEW, tasks[2].getState());
	}

}
