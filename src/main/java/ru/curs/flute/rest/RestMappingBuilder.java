package ru.curs.flute.rest;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;
import ru.curs.celesta.Celesta;


import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by ioann on 01.08.2017.
 */
public class RestMappingBuilder {

  private static RestMappingBuilder instance = new RestMappingBuilder();

  private final Set<FilterMapping> filterMappings = new LinkedHashSet<>();
  private final Set<RequestMapping> requestMappings = new HashSet<>();

  private final Map<RequestMapping, RouterFunction> routers = new HashMap<>();

  public static RestMappingBuilder getInstance() {
    return instance;
  }


  public void addFilterMapping(FilterMapping mapping) {
    filterMappings.add(mapping);
  }

  public void addRequestMapping(RequestMapping mapping) {
    requestMappings.add(mapping);
  }


  public void initRouters(Celesta celesta, String userId) {

    if (!routers.isEmpty())
      return;

    for (RequestMapping requestMappings : requestMappings) {
      HandlerFunction func = request -> {
        try {
          FluteRequest fluteRequest = new FluteRequest();
          fluteRequest.setUrl(request.path());
          fluteRequest.setParams(request.queryParams().toSingleValueMap());

          if (HttpMethod.POST.equals(requestMappings.getMethod())) {
            final Map<String, Object> json;
            ParameterizedTypeReference<Map<String, Object>> prt = new ParameterizedTypeReference<Map<String, Object>>() {
            };
            json = request.body(BodyExtractors.toFlux(prt)).toStream()
                .findAny().get();

            fluteRequest.setBody(json);
          }

          String sesId = String.format("FLUTE%08X", ThreadLocalRandom.current().nextInt());
          celesta.login(sesId, userId);
          Object result = celesta.runPython(sesId, requestMappings.getFunc(), fluteRequest).__tojava__(Object.class);
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
          RequestPredicates.method(requestMappings.getMethod())
              .and(RequestPredicates.path(requestMappings.getUrl())),
          func);

      for (FilterMapping f : filterMappings) {
        if (!f.matchesWithUrl(requestMappings.getUrl())) {
          continue;
        }

        router = router.filter((request, next) -> {
          //TODO:Сделать отдельный метод для вызова celesta-функций и отдельный для генерации фильтров
          final Mono<ServerResponse> response;
          if (f.getType().equalsIgnoreCase(FilterMapping.Type.BEFORE)) {
            try {
              String sesId = String.format("FLUTE%08X", ThreadLocalRandom.current().nextInt());
              celesta.login(sesId, userId);
              Object result = celesta.runPython(sesId, f.getFunc(), "").__tojava__(Object.class);
              celesta.logout(sesId, false);
            } catch (Exception ex) {
              throw new RuntimeException(ex);
            }

            response = next.handle(request);
          } else {
            response = next.handle(request);
            try {
              String sesId = String.format("FLUTE%08X", ThreadLocalRandom.current().nextInt());
              celesta.login(sesId, userId);
              Object result = celesta.runPython(sesId, f.getFunc(), "").__tojava__(Object.class);
              celesta.logout(sesId, false);
            } catch (Exception ex) {
              throw new RuntimeException(ex);
            }
          }
          return response;
        });
      }

      routers.put(requestMappings, router);
    }


  }

  public Map<RequestMapping, RouterFunction> getRouters() {
    return routers;
  }

}
