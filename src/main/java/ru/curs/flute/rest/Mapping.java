package ru.curs.flute.rest;

import org.springframework.http.HttpMethod;

import java.util.Objects;

/**
 * Created by ioann on 04.08.2017.
 */
public class Mapping {

  private String url;
  private String func;
  private String method = HttpMethod.GET.toString();

  public Mapping(String url, String func, String method) {
    this.url = url;
    this.func = func;
    this.method = method;
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

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;

    if (obj instanceof Mapping) {
      Mapping that = (Mapping) obj;

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
