package ru.curs.flute.rest;

import org.python.core.Py;
import org.python.core.PyObject;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;
import ru.curs.celesta.Celesta;
import ru.curs.flute.exception.EFluteCritical;
import ru.curs.flute.source.RestTaskSource;
import ru.curs.flute.task.AbstractFluteTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by ioann on 01.08.2017.
 */
public class RestMappingBuilder {

  private static RestMappingBuilder instance = new RestMappingBuilder();

  private final Set<FilterMapping> filterMappings = new LinkedHashSet<>();
  private final Set<RequestMapping> requestMappings = new HashSet<>();

  
  private final Map<RequestMapping, RouterFunction> routers = new HashMap<>();
  
  private final RestTaskSource dummyTaskSource = new RestTaskSource();

  public static RestMappingBuilder getInstance() {
    return instance;
  }


  public void addFilterMapping(FilterMapping mapping) {
    filterMappings.add(mapping);
  }

  public void addRequestMapping(RequestMapping mapping) {
    requestMappings.add(mapping);
  }


  public void initRouters(Celesta celesta, String globalUserId) {

    if (!routers.isEmpty())
      return;

    for (RequestMapping requestMappings : requestMappings) {
      //EXTRACT FILTERS
      Predicate<FilterMapping> filterMappingMatchesWithUrl = f -> f.matchesWithUrl(requestMappings.getUrl());
      Predicate<FilterMapping> isBeforeFilterMapping = f -> FilterMapping.Type.BEFORE.equalsIgnoreCase(f.getType());
      Predicate<FilterMapping> isAfterFilterMapping = f -> FilterMapping.Type.AFTER.equalsIgnoreCase(f.getType());

      List<FilterMapping> beforeFilterMappings = filterMappings.stream()
          .filter(filterMappingMatchesWithUrl.and(isBeforeFilterMapping))
          .collect(Collectors.toList());

      /**
       * В spring-webflux по умолчанию каждый новый before фильтр присоединяется к цепочке слева.
       * Нам же нужно, чтобы фильтры выполнялись в порядке их определения пользователем.
       * Поэтому разворачиваем список before фильтров
       */
      Collections.reverse(beforeFilterMappings);


      List<FilterMapping> afterFilterMappings = filterMappings.stream()
          .filter(filterMappingMatchesWithUrl.and(isAfterFilterMapping))
          .collect(Collectors.toList());

      FilterMapping lastFilter = null;
      if (!afterFilterMappings.isEmpty()) {
        lastFilter = afterFilterMappings.get(afterFilterMappings.size() - 1);
      }

      //APPLY MAPPING
      HandlerFunction func = request -> {
        try {

          String sesId = String.format("FLUTE%08X", ThreadLocalRandom.current().nextInt());

          final String userId = String.valueOf(
              request.attribute("userId").orElse(globalUserId)
          );

          celesta.login(sesId, userId);
          PyObject pyResult = celesta.runPython(sesId, requestMappings.getFunc(), dummyTaskSource.getTask(), request);
          celesta.logout(sesId, false);

          if (pyResult != null && !pyResult.equals(Py.None)) {
            Mono result = (Mono)pyResult.__tojava__(Mono.class);
            return result;
          } else if (afterFilterMappings.isEmpty()) {
            return ServerResponse.ok().body(Mono.just(""), String.class);
          } else {
            return null;
          }

        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      };
      RouterFunction router = RouterFunctions.route(
          RequestPredicates.method(requestMappings.getMethod())
              .and(RequestPredicates.path(requestMappings.getUrl())),
          func);


      //APPLY FILTERS
      for (FilterMapping beforeFilterMapping : beforeFilterMappings)
        router = appendFilterToRouter(router, beforeFilterMapping, celesta, globalUserId, false);
      for (FilterMapping afterFilterMapping : afterFilterMappings)
        router = appendFilterToRouter(
            router, afterFilterMapping, celesta,
            globalUserId, afterFilterMapping == lastFilter
        );

      routers.put(requestMappings, router);
    }


  }

  AbstractFluteTask getTask() throws InterruptedException, EFluteCritical{
	  return dummyTaskSource.getTask();
  }
  
  private RouterFunction appendFilterToRouter(
      RouterFunction router,
      FilterMapping filterMapping,
      Celesta celesta,
      String userId,
      boolean isLast
  ) {
    return router.filter((request, next) -> {
      //TODO:Сделать отдельный метод для вызова celesta-функций
      final Mono<ServerResponse> response;
      if (filterMapping.getType().equalsIgnoreCase(FilterMapping.Type.BEFORE)) {
        try {
          String sesId = String.format("FLUTE%08X", ThreadLocalRandom.current().nextInt());
          celesta.login(sesId, userId);
          PyObject pyResult = celesta.runPython(sesId, filterMapping.getFunc(), dummyTaskSource.getTask(), request);
          celesta.logout(sesId, false);


          //Если pyResult не null, то возвращаем его как ответ и прерываем цепочку хандлеров
          if (pyResult != null && !pyResult.equals(Py.None)) {
            response = (Mono)pyResult.__tojava__(Mono.class);
            return response;
          }

        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }

        response = next.handle(request);
      } else {
        response = next.handle(request);

        if (response != null) {
          return response;
        }

        try {
          String sesId = String.format("FLUTE%08X", ThreadLocalRandom.current().nextInt());
          celesta.login(sesId, userId);
          PyObject pyResult = celesta.runPython(sesId, filterMapping.getFunc(), dummyTaskSource.getTask(), request);
          celesta.logout(sesId, false);


          //Если pyResult не null, то возвращаем его как ответ и прерываем цепочку хандлеров
          if (pyResult != null && !pyResult.equals(Py.None)) {
            Mono<ServerResponse> javaResult = (Mono) pyResult.__tojava__(Mono.class);
            return javaResult;
          } else if (isLast) {
            return ServerResponse.ok().body(Mono.just(""), String.class);
          }
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
      return response;
    });
  }

  public Map<RequestMapping, RouterFunction> getRouters() {
    return routers;
  }

}
