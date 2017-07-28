package ru.curs.flute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Test;

import ru.curs.flute.FluteTask;
import ru.curs.flute.FluteTaskState;
import ru.curs.flute.TaskSource;

class DummyFluteTask extends FluteTask {
	public DummyFluteTask(TaskSource ts1) {
		super(ts1, 0, "", "");
	}

	@Override
	protected void doJob() throws InterruptedException {
		Thread.sleep(200);
	}
}

class DummyFluteTask2 extends FluteTask {
	public DummyFluteTask2(TaskSource ts1) {
		super(ts1, 0, "", "");
	}

	@Override
	protected void doJob() throws InterruptedException {
		Thread.sleep(30);
		throw new IllegalStateException("foo");
	}
}

class DummyFluteSource extends TaskSource {
	private final BlockingQueue<FluteTask> q = new LinkedBlockingQueue<>();

	void addTask(FluteTask t) {
		q.add(t);
	}

	@Override
	FluteTask getTask() {
		try {
			return q.take();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}

public class ThreadPoolTest {

	@Test
	public void test() throws InterruptedException {
		// test task execution and semaphore

		DummyFluteSource ts1 = new DummyFluteSource();
		ts1.setMaxThreads(2);

		FluteTask[] tasks = new FluteTask[3];
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

	private void initTasks(DummyFluteSource ts1, FluteTask[] tasks) {
		for (int i = 0; i < tasks.length; i++) {
			tasks[i] = new DummyFluteTask(ts1);
			ts1.addTask(tasks[i]);
		}
	}

	@Test
	public void test2() throws InterruptedException {
		// test task cancellation
		DummyFluteSource ts1 = new DummyFluteSource();
		ts1.setMaxThreads(2);
		ts1.setTerminationTimeout(800);

		FluteTask[] tasks = new FluteTask[3];
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
	public void test3() throws InterruptedException {
		// test task cancellation
		DummyFluteSource ts1 = new DummyFluteSource();
		ts1.setMaxThreads(2);
		ts1.setTerminationTimeout(50);

		FluteTask[] tasks = new FluteTask[3];
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
