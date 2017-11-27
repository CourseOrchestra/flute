package ru.curs.flute.rest;

import org.springframework.http.HttpMethod;

import java.util.Objects;

/**
 * Created by ioann on 04.08.2017.
 */
public class RequestMapping {

  private final String url;
  private final String func;
  private final String method;
  private final String contentType;

  public RequestMapping(String url, String func, String method, String contentType) {
    this.url = url;
    this.func = func;
    this.method = method;
    this.contentType = contentType;
  }

  public String getUrl() {
    return url;
  }

  public String getFunc() {
    return func;
  }

  public HttpMethod getMethod() {
    return HttpMethod.resolve(method.toUpperCase());
  }

  public String getContentType() {
    return contentType;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;

    if (obj instanceof RequestMapping) {
      RequestMapping that = (RequestMapping) obj;

      return Objects.equals(url, that.url)
          && Objects.equals(method, that.method);
    }

    return false;
  }

  @Override
  public int hashCode() {
    int result = 17;

    result = 31 * result + url.hashCode();
    result = 31 * result + method.hashCode();

    return result;
  }
}
