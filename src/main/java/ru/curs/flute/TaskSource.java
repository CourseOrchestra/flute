package ru.curs.flute;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import redis.clients.jedis.JedisPool;
import ru.curs.celesta.Celesta;
import ru.curs.celesta.CelestaException;

/**
 * Flute's task source.
 *
 */
public abstract class TaskSource implements Runnable {

	static final int DEFAULT_MAX_THREADS = 4;
	static final int DEFAULT_TERMINATION_TIMEOUT = 4000;

	@Autowired
	private GlobalParams params;

	@Autowired
	private Celesta celesta;

	@Autowired
	ApplicationContext ctx;

	private final ResizeableSemaphore semaphore = new ResizeableSemaphore();
	private int terminationTimeout = DEFAULT_TERMINATION_TIMEOUT;
	private int maxThreads = DEFAULT_MAX_THREADS;

	private static final class ResizeableSemaphore extends Semaphore {
		private static final long serialVersionUID = 1L;

		/**
		 * Create a new semaphore with 0 permits.
		 */
		ResizeableSemaphore() {
			super(DEFAULT_MAX_THREADS);
		}

		@Override
		protected void reducePermits(int reduction) {
			super.reducePermits(reduction);
		}
	}

	public synchronized void setMaxThreads(int newMax) {
		if (newMax < 1) {
			throw new IllegalArgumentException("Maximum threads number must be at least 1," + " was " + newMax);
		}

		int delta = newMax - this.maxThreads;

		if (delta == 0) {
			return;
		} else if (delta > 0) {
			this.semaphore.release(delta);
		} else {
			this.semaphore.reducePermits(-delta);
		}

		this.maxThreads = newMax;
	}

	public int getMaxThreads() {
		return maxThreads;
	}

	@Override
	public void run() {
		ExecutorService threads = Executors.newCachedThreadPool();
		retrycycle: while (true) {

			try {
				while (true) {
					semaphore.acquire();
					FluteTask command = getTask();
					threads.execute(command);
				}
			} catch (InterruptedException e) {
				try {
					threads.shutdown();
					threads.awaitTermination(terminationTimeout, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e1) {
					// do nothing, contintue shutdown
				}
				// send termination signal to all still executing tasks
				List<Runnable> waitingTasks = threads.shutdownNow();
				waitingTasks.forEach((t) -> {
					((FluteTask) t).setState(FluteTaskState.INTERRUPTED);
				});
				break retrycycle;
			} catch (EFluteCritical e) {
				e.printStackTrace();
				System.out.printf("Task source %s stopped execution on critical error (see stderr for details).%n",
						this.toString());
				if (params == null || !params.isNeverStop()) {
					break retrycycle;
				} else if (params.getRetryWait() > 0) {
					try {
						System.out.print("Restarting in %d milliseconds...");
						Thread.sleep(params.getRetryWait());
					} catch (InterruptedException e1) {
						// do nothing, return
						break retrycycle;
					}
					System.out.println("done.");
				}
			}
		}
	}

	void release() {
		semaphore.release();
	}

	abstract FluteTask getTask() throws InterruptedException, EFluteCritical;

	void changeTaskState(FluteTask t) {

	}

	public int getTerminationTimeout() {
		return terminationTimeout;
	}

	public void setTerminationTimeout(int terminationTimeout) {
		this.terminationTimeout = terminationTimeout;
	}

	void process(FluteTask task) throws InterruptedException, EFluteNonCritical {
		try {
			String sesId = String.format("FLUTE%08X", ThreadLocalRandom.current().nextInt());
			celesta.login(sesId, params.getFluteUserId());
			celesta.runPython(sesId, task.getScript(), task);
			Celesta.getInstance().logout(sesId, false);
		} catch (CelestaException e) {
			throw new EFluteNonCritical(String.format("Celesta execution error: %s", e.getMessage()));
		}
	}

	public JedisPool getJedisPool() {
		if (params.isExposeRedis())
			return ctx.getBean(JedisPool.class);
		else
			return null;
	}

}
