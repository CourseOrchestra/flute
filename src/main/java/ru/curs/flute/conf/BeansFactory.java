package ru.curs.flute.conf;

import java.sql.Connection;
import java.util.Collection;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.server.*;
import reactor.ipc.netty.http.server.HttpServer;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import ru.curs.celesta.AppSettings;
import ru.curs.celesta.Celesta;
import ru.curs.celesta.CelestaException;
import ru.curs.celesta.ConnectionPool;
import ru.curs.flute.GlobalParams;
import ru.curs.flute.JDBCConnectionPool;
import ru.curs.flute.exception.EFluteCritical;
import ru.curs.flute.rest.RestMappingBuilder;

@Configuration
public class BeansFactory {
  private static final int MAX_REDIS_CONN = 32;

  private final CommonParameters params;

  public BeansFactory(@Autowired CommonParameters params) throws EFluteCritical {
    this.params = params;
    Properties p = new Properties();
    if (params.getScorePath() == null || params.getScorePath().isEmpty()) {
      throw new EFluteCritical("Score path setting is missing in configuration file!");
    } else {
      p.setProperty("score.path", params.getScorePath());
    }
    if (params.getPylibPath() != null)
      p.setProperty("pylib.path", params.getPylibPath());
    if (params.getJavaLibPath() != null)
      p.setProperty("javalib.path", params.getJavaLibPath());

    if (params.getConnString() == null || params.getConnString().isEmpty()) {
      throw new EFluteCritical("dbconnstring setting is missing in configuration file!");
    } else {
      p.setProperty("rdbms.connection.url", params.getConnString());
    }

    if (params.getDBUser() != null)
      p.setProperty("rdbms.connection.username", params.getDBUser());
    if (params.getDBPassword() != null)
      p.setProperty("rdbms.connection.password", params.getDBPassword());
    p.setProperty("log.logins", Boolean.toString(params.isLogLogins()));
    p.setProperty("skip.dbupdate", Boolean.toString(params.isSkipDBUpdate()));
    p.setProperty("force.dbinitialize", Boolean.toString(params.isForceDBInitialize()));

    Properties additionalProps = params.getSetupProperties();
    additionalProps.stringPropertyNames().forEach((s) -> {
      p.setProperty(s, additionalProps.getProperty(s));
    });

    try {
      Celesta.initialize(p);
    } catch (CelestaException e) {
      throw new EFluteCritical(e.getMessage());
    }
  }

  /**
   * A pool with JDBC connections.
   */
  @Bean
  public JDBCConnectionPool getJDBCConnectionPool() {
    return new JDBCConnectionPool() {
      @Override
      public Connection get() throws Exception {
        return ConnectionPool.get();
      }

      @Override
      public void putBack(Connection conn) {
        ConnectionPool.putBack(conn);
      }

      @Override
      public void commit(Connection conn) {
        ConnectionPool.commit(conn);
      }

      @Override
      public DBType getDBType() {
        switch (AppSettings.getDBType()) {
          case MSSQL:
            return DBType.MSSQLServer;
          case ORACLE:
            return DBType.Oracle;
          case H2:
            return DBType.H2;
          case POSTGRES:
          default:
            return DBType.PostgreSQL;
        }
      }
    };
  }

  /**
   * The Celesta singleton instance.
   *
   * @throws CelestaException Celesta configuration exception.
   */
  @Bean
  public Celesta getCelesta() throws CelestaException {
    return Celesta.getInstance();
  }

  @Bean
  public HttpServer httpServer() throws Exception {
    final HttpServer httpServer;

    if (params.getRestPort() != null) {
      httpServer = HttpServer.create("localhost", params.getRestPort());

      RestMappingBuilder.getInstance().initRouters(getCelesta(), getGlobalParams().getFluteUserId());

      if (!RestMappingBuilder.getInstance().getRouters().isEmpty()) {
        Collection<RouterFunction> routers = RestMappingBuilder.getInstance()
            .getRouters()
            .values();

        routers.stream()
            .forEach(r -> {
              HttpHandler httpHandler = RouterFunctions.toHttpHandler(r);
              httpServer.newHandler(new ReactorHttpHandlerAdapter(httpHandler)).block();
            });

        System.out.println("Rest service is started.");
      }
    } else httpServer = null;

    return httpServer;
  }

  /**
   * A pool of RedisConnections.
   */
  @Bean
  public JedisPool getJedisPool() {
    JedisPool p;
    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(MAX_REDIS_CONN);
    poolConfig.setTestOnBorrow(true);
    poolConfig.setTestOnCreate(true);
    if (params.getRedisPassword() != null) {
      p = new JedisPool(poolConfig, params.getRedisHost(), params.getRedisPort(), Protocol.DEFAULT_TIMEOUT,
          params.getRedisPassword());
    } else {
      p = new JedisPool(poolConfig, params.getRedisHost(), params.getRedisPort());
    }
    return p;
  }

  /**
   * Global Flute parameters.
   */
  @Bean
  public GlobalParams getGlobalParams() {
    return new GlobalParams() {
      @Override
      public int getRetryWait() {
        return params.getRetryWait();
      }

      @Override
      public boolean isNeverStop() {
        return params.isNeverStop();
      }

      @Override
      public String getFluteUserId() {
        return params.getFluteUserId();
      }

      @Override
      public boolean isExposeRedis() {
        return params.isExposeRedis();
      }
    };
  }

}