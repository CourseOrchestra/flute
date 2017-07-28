package ru.curs.flute;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class LoopTaskSupplier extends SingleTaskSupplier {

	public static final int DEFAULT_WAIT_ON_SUCCESS = 1000;
	public static final int DEFAULT_WAIT_ON_FAILURE = 1000;

	private int waitOnSuccess = DEFAULT_WAIT_ON_SUCCESS;
	private int waitOnFailure = DEFAULT_WAIT_ON_FAILURE;

	@Override
	public void run() {
		try {
			while (true) {
				FluteTask command = getTask();
				command.run();
				switch (command.getState()) {
				case SUCCESS:
					Thread.sleep(waitOnSuccess);
					break;
				case FAIL:
					Thread.sleep(waitOnFailure);
					break;
				case INTERRUPTED:
				default:
					return;
				}
			}
		} catch (InterruptedException e) {
			return;
		}
	}

	int getWaitOnSuccess() {
		return waitOnSuccess;
	}

	int getWaitOnFailure() {
		return waitOnFailure;
	}

	void setWaitOnSuccess(int waitOnSuccess) {
		this.waitOnSuccess = waitOnSuccess;
	}

	void setWaitOnFailure(int waitOnFailure) {
		this.waitOnFailure = waitOnFailure;
	}

	@Override
	void release() {
		// do nothing for this supplier: we do not utilize multi-threading here
	}

}
