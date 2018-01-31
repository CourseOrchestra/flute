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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import ru.curs.flute.conf.CommonParameters;
import ru.curs.flute.conf.ConfFileLocator;
import ru.curs.flute.source.TaskSource;
import ru.curs.flute.source.TaskSources;
import spark.Spark;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Запускаемый из консоли или из Apache Commons Service Runner класс приложения.
 */
@SpringBootApplication
public class Main {

  private static ApplicationContext applicationContext;
  private static final ExecutorService svc = Executors.newCachedThreadPool();

  private List<TaskSource> taskSources;

  @Autowired
  private TaskSources taskSourcesBean;

  @Autowired
  private CommonParameters params;

  @PostConstruct
  public void postConstruct() {
    System.out.printf("Flute (3rd generation) is starting.%n");
    taskSources = taskSourcesBean.getSources();
    taskSources.forEach(svc::execute);

    if (taskSources.size() == 1) {
      System.out.printf("Flute started. One taskSource is being processed.%n", taskSources.size());
    } else {
      System.out.printf("Flute started. %d taskSources are being processed.%n", taskSources.size());
    }

    if (params.getRestPort() != null) {
      Spark.stop();
    }
  }

  @PreDestroy
  public void preDestroy() {
    try {
      System.out.println("Flute stopping...");
      svc.shutdownNow();
      svc.awaitTermination(1, TimeUnit.MINUTES);
      taskSources.forEach(t -> {
        t.tearDown();
      });
      System.out.println("Flute stopped.");
    } catch (InterruptedException e) {
      return;
    }
  }

  @Bean
  public ExitCodeGenerator exitCodeGenerator() {
    return () -> 42;
  }

  /**
   * Точка запуска приложения из консоли.
   *
   * @param args аргументы.
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
      startService(args);
    } else {
      stopService(args);
    }
  }

  private synchronized static void startService(String[] args) {
    SpringApplication app = new SpringApplication(Main.class);
    applicationContext = app.run(args);
  }

  private synchronized static void stopService(String[] args) {
    int result =SpringApplication.exit(applicationContext, applicationContext.getBean(ExitCodeGenerator.class));

    if (result != 0)
      System.exit(result);
  }

  /**
   * init-метод Apache Commons Daemon: Here open configuration files, create a
   * trace file, create ServerSockets, Threads.
   *
   * @param arguments параметры (в нашем случае игнорируются).
   */
  public void init(String[] arguments) {

  }

  /**
   * start-метод Apache Commons Daemon: Start the Thread, accept incoming
   * connections.
   */
  public void start() {
    System.err.println("Flute starting...");
    startService(new String[0]);
    System.err.println("Flute started.");
  }

  /**
   * stop-метод Apache Commons Daemon: Inform the Thread to terminate the
   * run(), close the ServerSockets.
   */
  public void stop() {
    System.err.println("Flute stopping...");
    stopService(new String[0]);
    System.err.println("Flute stopped");
  }

  /**
   * destroy-метод Apache Commons Daemon: Destroy any object created in
   * init().
   */
  public void destroy() {

  }

}
