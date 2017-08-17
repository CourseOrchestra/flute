package ru.curs.flute.rest;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ioann on 17.08.2017.
 */
public class FilterMapping {

  private String urlPattern;
  private String func;
  private String type = Type.BEFORE;

  private final Pattern regexPattern;

  public FilterMapping(String urlPattern, String func, String type) {
    this.urlPattern = urlPattern;
    this.func = func;
    this.type = type;
    this.regexPattern = resolveRegexPattern(urlPattern);
  }

  private Pattern resolveRegexPattern(String urlPattern) {
    StringBuilder regexBuilder = new StringBuilder("^")
        .append(urlPattern.replace("*", "[a-zA-Z0-9_.-/]*"))
        .append("$");
    Pattern p = Pattern.compile(regexBuilder.toString());
    return p;
  }

  public String getUrlPattern() {
    return urlPattern;
  }

  public String getFunc() {
    return func;
  }

  public String getType() {
    return type;
  }

  public boolean matchesWithUrl(String url) {
    Matcher m = regexPattern.matcher(url);
    return m.matches();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;

    if (obj instanceof FilterMapping) {
      FilterMapping that = (FilterMapping) obj;

      return Objects.equals(urlPattern, that.urlPattern)
          && Objects.equals(type, that.type);
    }

    return false;
  }

  @Override
  public int hashCode() {
    int result = 17;

    result = 31 * result + urlPattern.hashCode();
    result = 31 * result + type.hashCode();

    return result;
  }

  public static class Type {

    public final static String BEFORE = "BEFORE";
    public final static String AFTER = "AFTER";

    private Type() {
      throw new AssertionError();
    }
  }
}
