package ru.curs.flute.rest.decorator;

import org.python.core.PyObject;
import ru.curs.flute.rest.FilterMapping;

/**
 * Created by ioann on 07.09.2017.
 */
public class FilterAfter extends AbstractFilter {

  public FilterAfter(PyObject... urlPatterns) {
    super(urlPatterns);
  }

  @Override
  protected String getType() {
    return FilterMapping.Type.AFTER;
  }
}
