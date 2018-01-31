package ru.curs.flute.rest;

import org.eclipse.jetty.http.HttpMethod;
import org.python.core.Py;
import org.python.core.PyObject;
import ru.curs.celesta.Celesta;
import ru.curs.flute.source.RestTaskSource;
import spark.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by ioann on 01.08.2017.
 */
public class RestMappingBuilder {

  private static RestMappingBuilder instance = new RestMappingBuilder();

  private final Set<FilterMapping> filterMappings = new LinkedHashSet<>();
  private final Set<RequestMapping> requestMappings = new HashSet<>();

  private boolean inited;

  public static RestMappingBuilder getInstance() {
    return instance;
  }


  public void addFilterMapping(FilterMapping mapping) {
    filterMappings.add(mapping);
  }

  public void addRequestMapping(RequestMapping mapping) {
    requestMappings.add(mapping);
  }


  public synchronized void initRouters(Celesta celesta, RestTaskSource taskSource, String globalUserId) {

    if (inited)
      return;

    for (RequestMapping requestMappings : requestMappings) {

      //APPLY MAPPING
      Route func = (req, res) -> {
          String sesId = String.format("FLUTE%08X", ThreadLocalRandom.current().nextInt());

          String userIdAttribute = req.attribute("userId");

          final String userId = userIdAttribute != null ? userIdAttribute : globalUserId;

          celesta.login(sesId, userId);
          PyObject pyResult = celesta.runPython(sesId, requestMappings.getFunc(), taskSource.getTask(), req);
          celesta.logout(sesId, false);

          if (pyResult != null && !pyResult.equals(Py.None)) {

            if (Py.NoConversion != pyResult.__tojava__(FluteResponse.class)) {
              FluteResponse fr =  (FluteResponse)pyResult.__tojava__(FluteResponse.class);
              Spark.halt(fr.getStatus(), fr.getText());
            }

            Spark.halt(200, pyResult.toString());
            //return pyResult;

            return null;
          } else {
            return "";
          }

      };


      if (HttpMethod.GET.equals(requestMappings.getMethod())) {
          Spark.get(requestMappings.getUrl(), func);
      } else if (HttpMethod.POST.equals(requestMappings.getMethod())) {
        if (requestMappings.getContentType() != null)
          Spark.post(requestMappings.getUrl(), requestMappings.getContentType(), func);
        else
          Spark.post(requestMappings.getUrl(), func);
      }

    }


    for (FilterMapping filterMapping: filterMappings) {
      Filter filter = filter(filterMapping.getFunc(), celesta, taskSource, globalUserId);
      for (String urlPattern: filterMapping.getUrlPatterns()) {
        if (FilterMapping.Type.BEFORE.equals(filterMapping.getType()))
          Spark.before(urlPattern, filter);
        else
          Spark.after(urlPattern, filter);
      }
    }

    inited = true;
  }


  private Filter filter(String func, Celesta celesta, RestTaskSource taskSource, String userId) {
    return (req, res) -> {
      String sesId = String.format("FLUTE%08X", ThreadLocalRandom.current().nextInt());

      celesta.login(sesId, userId);
      PyObject pyResult = celesta.runPython(sesId, func, taskSource.getTask(), req);
      celesta.logout(sesId, false);

      if (pyResult != null && !pyResult.equals(Py.None)) {

        if (Py.NoConversion != pyResult.__tojava__(FluteResponse.class)) {
          FluteResponse fr = (FluteResponse) pyResult.__tojava__(FluteResponse.class);
          Spark.halt(fr.getStatus(), fr.getText());
        } else {
          Spark.halt(200, pyResult.toString());
        }

      }

    };
  }

}
