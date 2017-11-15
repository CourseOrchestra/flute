package ru.curs.flute.rest.decorator;

import org.python.core.PyFunction;
import org.python.core.PyObject;
import ru.curs.flute.rest.FilterMapping;
import ru.curs.flute.rest.RestMappingBuilder;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by ioann on 07.09.2017.
 */
public abstract class AbstractFilter {

  private Set<String> urlPatterns;

  protected AbstractFilter(PyObject... urlPatterns) {
    this.urlPatterns = Arrays.asList(urlPatterns).stream()
        .map(p -> p.__tojava__(String.class).toString())
        .collect(Collectors.toSet());
  }

  public PyFunction __call__(PyFunction func) {
    RestMappingBuilder restMappingBuilder = RestMappingBuilder.getInstance();
    String funcName = func.__module__ + "." + func.__name__;
    FilterMapping filterMapping = new FilterMapping(urlPatterns, funcName, getType());
    restMappingBuilder.addFilterMapping(filterMapping);
    return func;
  }

  protected abstract String getType();
}
