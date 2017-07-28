package ru.curs.flute;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

public class ScheduledTaskSupplierTest {
	@Test
	public void taskCanBeScheduled() throws InterruptedException {

		Set<String> t = new HashSet<>();
		Random r = new Random();
		for (int i = 0; i < 250; i++) {
			t.add(String.format("%08X", r.nextInt()));
		}

		final Set<String> expected = Collections.synchronizedSet(new HashSet<>());

		ScheduledTaskSupplier sts = new ScheduledTaskSupplier() {
			@Override
			void process(FluteTask task) throws InterruptedException, EFluteNonCritical {
				expected.add(task.getParams());
			}
		};

		sts.setParams("asdfasdf");
		sts.setSchedule("* * * * *");
		ExecutorService es = Executors.newSingleThreadExecutor();
		es.submit(sts);
		t.forEach(s -> {
			FluteTask ft = new FluteTask(sts, 0, "ss", s);
			try {
				sts.internalAdd(ft);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		Thread.sleep(100);
		es.shutdownNow();

		t.forEach(s -> {
			assertTrue(expected.contains(s));
		});
	}

}
