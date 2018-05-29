package ru.curs.flute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;
import ru.curs.flute.exception.EFluteNonCritical;
import ru.curs.flute.source.LoopTaskSupplier;
import ru.curs.flute.task.FluteTask;
import ru.curs.flute.task.SingleTask;
import ru.curs.flute.task.TaskUnit;

public class LoopTaskSupplierTest {
	@Test
	public void singleTaskSuppliedEachTime() throws InterruptedException {
		LoopTaskSupplier s = new LoopTaskSupplier();
		s.setTaskUnit(new TaskUnit("foobar", TaskUnit.Type.SCRIPT));
		s.setParams("barfoo");

		assertEquals(1000, s.getWaitOnFailure());
		assertEquals(1000, s.getWaitOnSuccess());

		SingleTask t1 = s.getTask();
		assertEquals("foobar", t1.getTaskUnit().getQualifier());
		assertEquals(TaskUnit.Type.SCRIPT, t1.getTaskUnit().getType());
		assertEquals("barfoo", t1.getParams());

		SingleTask t2 = s.getTask();
		assertEquals("foobar", t2.getTaskUnit().getQualifier());
		assertEquals(TaskUnit.Type.SCRIPT, t2.getTaskUnit().getType());
		assertEquals("barfoo", t2.getParams());

		assertNotSame(t1, t2);
	}

	@Test
	public void taskIsExecutedInLoop() throws InterruptedException {
		TestLoopTaskSupplier s = new TestLoopTaskSupplier();
		s.setTaskUnit(new TaskUnit("foobar", TaskUnit.Type.SCRIPT));
		s.setParams("barfoo");
		s.setWaitOnSuccess(10);
		ExecutorService es = Executors.newSingleThreadExecutor();
		es.submit(s);
		while (s.result.size() < 10) {
			Thread.sleep(10);
		}
		es.shutdownNow();
		s.result.forEach(msg -> {
			assertEquals("barfoo", msg);
		});
	}

	@Test
	public void failedTaskIsExecutedInLoop() throws InterruptedException {
		TestLoopTaskSupplier s = new TestLoopTaskSupplier();
		s.setTaskUnit(new TaskUnit("foobar", TaskUnit.Type.SCRIPT));
		s.setParams("FAIL");
		s.setWaitOnFailure(10);
		ExecutorService es = Executors.newSingleThreadExecutor();
		es.submit(s);
		while (s.result.size() < 10) {
			Thread.sleep(5);
		}
		es.shutdownNow();
		int a = s.result.size();
		Thread.sleep(100);
		assertEquals(a, s.result.size());
		s.result.forEach(msg -> {
			assertEquals("FAIL", msg);
		});
	}

}

class TestLoopTaskSupplier extends LoopTaskSupplier {

	LinkedList<String> result = new LinkedList<>();

	@Override
	public void process(FluteTask task) throws InterruptedException, EFluteNonCritical {
		result.add(task.getParams());
		if ("FAIL".equalsIgnoreCase(task.getParams()))
			throw new EFluteNonCritical("fail");
	}

}