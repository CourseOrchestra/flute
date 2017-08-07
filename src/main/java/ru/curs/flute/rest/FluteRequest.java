package ru.curs.flute.rest;

import org.python.core.PyObject;

import java.util.Map;

/**
 * Created by ioann on 03.08.2017.
 */
public class FluteRequest {

  private String url;
  private Map<String, String> params;
  private Map<String, Object> body;


  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Map<String, String> getParams() {
    return params;
  }

  public void setParams(Map<String, String> params) {
    this.params = params;
  }

  public Map<String, Object> getBody() {
    return body;
  }

  public void setBody(Map<String, Object> body) {
    this.body = body;
  }
}
