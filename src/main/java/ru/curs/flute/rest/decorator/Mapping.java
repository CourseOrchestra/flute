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
  private final String contentType;

  public Mapping(String url) {
    this(url, HttpMethod.GET.toString(), null);
  }

  public Mapping(String url, String method) {
    this(url, method, null);
  }

  public Mapping(String url, String method, String contentType) {
    this.url = url;
    this.method = method;
    this.contentType = contentType;
  }

  public PyFunction __call__(PyFunction func) {
    RestMappingBuilder restMappingBuilder = RestMappingBuilder.getInstance();
    String funcName = func.__module__ + "." + func.__name__;
    RequestMapping requestMapping = new RequestMapping(url, funcName, method, contentType);
    restMappingBuilder.addRequestMapping(requestMapping);
    return func;
  }

}
