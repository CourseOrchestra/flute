package ru.curs.flute.rest.decorator;

import org.python.core.PyFunction;

import org.springframework.http.HttpMethod;
import ru.curs.flute.rest.RequestMapping;
import ru.curs.flute.rest.RestMappingBuilder;

/**
 * Created by ioann on 07.09.2017.
 */
public class Mapping {

  private final String url;
  private final String method;

  public Mapping(String url) {
    this(url, HttpMethod.GET.toString());
  }

  public Mapping(String url, String method) {
    this.url = url;
    this.method = method;
  }

  public PyFunction __call__(PyFunction func) {
    RestMappingBuilder restMappingBuilder = RestMappingBuilder.getInstance();
    String funcName = func.__module__ + "." + func.__name__;
    RequestMapping requestMapping = new RequestMapping(url, funcName, method);
    restMappingBuilder.addRequestMapping(requestMapping);
    return func;
  }

}
