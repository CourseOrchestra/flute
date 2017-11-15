package ru.curs.flute.rest;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by ioann on 17.08.2017.
 */
public class FilterMapping {

  private Set<String> urlPatterns = new HashSet<>();
  private String func;
  private String type = Type.BEFORE;

  private final List<Pattern> regexPatterns = new ArrayList<>();

  public FilterMapping(Set<String> urlPatterns, String func, String type) {
    this.urlPatterns = urlPatterns;
    this.func = func;
    this.type = type;
    resolveRegexPattern(urlPatterns);
  }

  private void resolveRegexPattern(Set<String> urlPatterns) {
    urlPatterns.forEach(urlPattern -> {
          StringBuilder regexBuilder = new StringBuilder("^")
              .append(urlPattern.replace("*", "[a-zA-Z0-9_.-/]*"))
              .append("$");
          Pattern p = Pattern.compile(regexBuilder.toString());
          regexPatterns.add(p);
        }
    );
  }

  public Set<String> getUrlPatterns() {
    return urlPatterns;
  }

  public String getFunc() {
    return func;
  }

  public String getType() {
    return type;
  }

  public boolean matchesWithUrl(String url) {
    return regexPatterns.stream()
        .filter(regexPattern -> regexPattern.matcher(url).matches())
        .findFirst().isPresent();
  }

  public static class Type {

    public final static String BEFORE = "BEFORE";
    public final static String AFTER = "AFTER";

    private Type() {
      throw new AssertionError();
    }
  }
}
