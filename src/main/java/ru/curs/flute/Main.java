/*
   (с) 2013 ООО "КУРС-ИТ"  

   Этот файл — часть КУРС:Flute.
   
   КУРС:Flute — свободная программа: вы можете перераспространять ее и/или изменять
   ее на условиях Стандартной общественной лицензии GNU в том виде, в каком
   она была опубликована Фондом свободного программного обеспечения; либо
   версии 3 лицензии, либо (по вашему выбору) любой более поздней версии.

   Эта программа распространяется в надежде, что она будет полезной,
   но БЕЗО ВСЯКИХ ГАРАНТИЙ; даже без неявной гарантии ТОВАРНОГО ВИДА
   или ПРИГОДНОСТИ ДЛЯ ОПРЕДЕЛЕННЫХ ЦЕЛЕЙ. Подробнее см. в Стандартной
   общественной лицензии GNU.

   Вы должны были получить копию Стандартной общественной лицензии GNU
   вместе с этой программой. Если это не так, см. http://www.gnu.org/licenses/.

   
   Copyright 2013, COURSE-IT Ltd.

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see http://www.gnu.org/licenses/.

 */

package ru.curs.flute;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.NestedRuntimeException;

/**
 * Запускаемый из консоли или из Apache Commons Service Runner класс приложения.
 * 
 */
public final class Main {

	private static final ApplicationContext ctx;

	private static final ExecutorService svc = Executors.newCachedThreadPool();

	static {
		ApplicationContext c = null;
		try {
			c = new AnnotationConfigApplicationContext(BeansFactory.class, ConfFileLocator.class);
		} catch (NestedRuntimeException e) {
			System.out.println("ERROR: " + e.getRootCause().getMessage());
			System.exit(1);
		}
		ctx = c;
	}

	/**
	 * Точка запуска приложения из консоли.
	 * 
	 * @param args
	 *            аргументы.
	 */
	public static void main(String[] args) {

		if (args.length > 1) {
			File f = new File(args[1]);
			ConfFileLocator.setFile(f);
		}

		String cmd = "start";
		if (args.length > 0)
			cmd = args[0];

		if ("start".equals(cmd)) {
			startService();
		} else {
			stopService();
		}
	}

	private static void startService() {
		System.out.printf("Flute (3rd generation) is starting.%n");
		List<TaskSource> taskSources = ctx.getBean(TaskSources.class).getSources();
		taskSources.forEach(svc::execute);
		if (taskSources.size() == 1) {
			System.out.printf("Flute started. One queue is being processed.%n", taskSources.size());
		} else {
			System.out.printf("Flute started. %d queues are being processed.%n", taskSources.size());
		}

		/*
		 * try { Thread.sleep(2000); } catch (InterruptedException e) {
		 * e.printStackTrace(); } stopService();
		 */
	}

	private static void stopService() {
		try {
			System.out.println("Flute stopping...");
			svc.shutdownNow();
			svc.awaitTermination(1, TimeUnit.MINUTES);
			System.out.println("Flute stopped.");
		} catch (InterruptedException e) {
			return;
		}
	}

	/**
	 * init-метод Apache Commons Daemon: Here open configuration files, create a
	 * trace file, create ServerSockets, Threads.
	 * 
	 * @param arguments
	 *            параметры (в нашем случае игнорируются).
	 */
	public void init(String[] arguments) {

	}

	/**
	 * start-метод Apache Commons Daemon: Start the Thread, accept incoming
	 * connections.
	 */
	public void start() {
		System.err.println("Flute starting...");
		startService();
		System.err.println("Flute started.");
	}

	/**
	 * stop-метод Apache Commons Daemon: Inform the Thread to terminate the
	 * run(), close the ServerSockets.
	 */
	public void stop() {
		System.err.println("Flute stopping...");
		stopService();
		System.err.println("Flute stopped");
	}

	/**
	 * destroy-метод Apache Commons Daemon: Destroy any object created in
	 * init().
	 */
	public void destroy() {

	}

}
