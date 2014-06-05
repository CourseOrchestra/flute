package ru.curs.flute.webrunner;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Перехватчик старта и остановки приложения.
 */
public class AppEventsListener implements ServletContextListener {

	@Override
	public final void contextInitialized(final ServletContextEvent arg0) {
		FluteRunner.RUNNER.start();
	}

	@Override
	public final void contextDestroyed(final ServletContextEvent arg0) {
		FluteRunner.RUNNER.stop();
	}

}