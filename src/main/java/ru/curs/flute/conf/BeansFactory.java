package ru.curs.flute.conf;

import java.sql.Connection;
import java.sql.SQLException;
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
import ru.curs.celesta.*;
import ru.curs.flute.GlobalParams;
import ru.curs.flute.JDBCConnectionPool;
import ru.curs.flute.exception.EFluteCritical;
import ru.curs.flute.rest.RestMappingBuilder;
import ru.curs.flute.source.RestTaskSource;

@Configuration
public class BeansFactory {
    private static final int MAX_REDIS_CONN = 32;

    private final CommonParameters params;

    public BeansFactory(@Autowired CommonParameters params) throws EFluteCritical {
        this.params = params;
    }

    /**
     * A pool with JDBC connections.
     */
    @Bean
    public JDBCConnectionPool getJDBCConnectionPool(@Autowired CommonParameters params) throws EFluteCritical {

        ConnectionPoolConfiguration cpConf = new ConnectionPoolConfiguration();
        String jdbcConnectionUrl = params.getConnString();
        cpConf.setJdbcConnectionUrl(jdbcConnectionUrl);
        cpConf.setDriverClassName(AppSettings.resolveDbType(jdbcConnectionUrl).getDriverClassName());
        cpConf.setLogin(params.getDBUser());
        cpConf.setPassword(params.getDBPassword());

        final ConnectionPool connectionPool;
        try {
            connectionPool = ConnectionPool.create(cpConf);
        } catch (CelestaException e) {
            throw new EFluteCritical(e.getMessage());
        }


        return new JDBCConnectionPool() {
            @Override
            public Connection get() throws Exception {
                return connectionPool.get();
            }

            @Override
            public void putBack(Connection conn) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void commit(Connection conn) {
                connectionPool.commit(conn);
            }

            @Override
            public DBType getDBType() {
                final Properties properties;
                try {
                    properties = getCelesta().getSetupProperties();

                    switch (new AppSettings(properties).getDBType()) {
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
                } catch (EFluteCritical | CelestaException e) {
                    throw new RuntimeException(e);
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
    public Celesta getCelesta() throws EFluteCritical {
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
            return Celesta.createInstance(p);
        } catch (CelestaException e) {
            throw new EFluteCritical(e.getMessage());
        }
    }

    @Bean
    public HttpServer httpServer() {
        return params.getRestPort().map(i -> HttpServer.create(i)).orElse(null);
    }

    @Bean
    public ReactorHttpHandlerAdapter httpHandlerAdapter(@Autowired RestTaskSource taskSource) throws Exception {
        ReactorHttpHandlerAdapter adapter = null;

        if (params.getRestPort() != null) {
            RestMappingBuilder.getInstance().initRouters(getCelesta(), taskSource, getGlobalParams().getFluteUserId());
            if (!RestMappingBuilder.getInstance().getRouters().isEmpty()) {
                Collection<RouterFunction> routers = RestMappingBuilder.getInstance()
                        .getRouters()
                        .values();

                RouterFunction router = routers.stream().reduce((r, r2) -> r == null ? r : r.and(r2)).get();
                HttpHandler httpHandler = RouterFunctions.toHttpHandler(router);

                adapter = new ReactorHttpHandlerAdapter(httpHandler);
            }
        }
        return adapter;
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
