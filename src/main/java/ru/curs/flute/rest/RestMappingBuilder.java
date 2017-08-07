package ru.curs.flute.rest;

import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;
import ru.curs.celesta.Celesta;


import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by ioann on 01.08.2017.
 */
public class RestMappingBuilder {

  private static RestMappingBuilder instance = new RestMappingBuilder();

  private final Set<Mapping> mappings = new HashSet<>();
  private final Map<Mapping, RouterFunction> routers = new HashMap<>();

  public static RestMappingBuilder getInstance() {
    return instance;
  }

  public void addMapping(Mapping mapping) {
    mappings.add(mapping);
  }


  public void initRouters(Celesta celesta, String userId) {

    if (!routers.isEmpty())
      return;

    for (Mapping mapping : mappings) {
      HandlerFunction func = request -> {
        try {
          FluteRequest fluteRequest = new FluteRequest();
          fluteRequest.setUrl(request.path());
          fluteRequest.setParams(request.queryParams().toSingleValueMap());

          if (HttpMethod.POST.equals(mapping.getMethod())) {
            final Map<String, Object> json;
            ParameterizedTypeReference<Map<String, Object>> prt = new ParameterizedTypeReference<Map<String, Object>>() {
            };
            json = request.body(BodyExtractors.toFlux(prt)).toStream()
                .findAny().get();

            fluteRequest.setBody(json);
          }

          String sesId = String.format("FLUTE%08X", ThreadLocalRandom.current().nextInt());
          celesta.login(sesId, userId);
          Object result = celesta.runPython(sesId, mapping.getFunc(), fluteRequest).__tojava__(Object.class);
          celesta.logout(sesId, false);

          if (result instanceof FluteResponse) {
            FluteResponse fluteResponse = (FluteResponse) result;
            return ServerResponse.status(HttpStatus.valueOf(fluteResponse.getStatus()))
                .body(Mono.just(fluteResponse.getText()), String.class);
          } else {
            return ServerResponse.ok().body(Mono.just(result), Object.class);
          }

        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      };
      RouterFunction router = RouterFunctions.route(
          RequestPredicates.method(mapping.getMethod())
              .and(RequestPredicates.path(mapping.getUrl())),
          func);
      routers.put(mapping, router);
    }


  }

  public Map<Mapping, RouterFunction> getRouters() {
    return routers;
  }

}
