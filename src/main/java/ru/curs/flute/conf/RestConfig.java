package ru.curs.flute.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import ru.curs.celesta.vintage.Celesta;
import ru.curs.flute.GlobalParams;
import ru.curs.flute.rest.RestMappingBuilder;
import ru.curs.flute.source.RestTaskSource;
import spark.Spark;

@Configuration
public class RestConfig {

    public RestConfig(
            @Autowired CommonParameters params,
            @Autowired Celesta celesta,
            @Autowired RestTaskSource taskSource,
            @Autowired GlobalParams globalParams) {
        if (params.getRestPort() == null)
            return;


        params.getRestPort().ifPresent(port -> {
            Spark.port(port);
            Spark.ipAddress(params.getRestHost());
            RestMappingBuilder.getInstance().initRouters(celesta, taskSource, globalParams.getFluteUserId());
        });

    }

}
