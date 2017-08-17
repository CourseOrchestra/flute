package ru.curs.flute.rest;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by ioann on 17.08.2017.
 */
public class FilterMappingTest {

  @Test
  public void testMatchingWithSimpleUrl() {
    FilterMapping filterMapping = new FilterMapping(
        "/foo/bar",
        "",
        FilterMapping.Type.BEFORE
    );

    assertTrue(filterMapping.matchesWithUrl("/foo/bar"));
    assertFalse(filterMapping.matchesWithUrl("/foo/bar/"));
    assertFalse(filterMapping.matchesWithUrl("foo/bar"));
    assertFalse(filterMapping.matchesWithUrl("/"));
  }

  @Test
  public void testMatchingWithRandomUrl() {
    FilterMapping filterMapping = new FilterMapping(
        "*",
        "",
        FilterMapping.Type.BEFORE
    );

    assertTrue(filterMapping.matchesWithUrl("/foo/bar"));
    assertTrue(filterMapping.matchesWithUrl("/foo/bar/"));
    assertTrue(filterMapping.matchesWithUrl("foo/bar"));
    assertTrue(filterMapping.matchesWithUrl("/"));
  }

  @Test
  public void testMatchingWithPartialRandomUrl() {
    FilterMapping filterMapping = new FilterMapping(
        "/foo/*/bar",
        "",
        FilterMapping.Type.BEFORE
    );

    assertTrue(filterMapping.matchesWithUrl("/foo/between/bar"));
    assertTrue(filterMapping.matchesWithUrl("/foo/5/bar"));
    assertTrue(filterMapping.matchesWithUrl("/foo/one/two/bar"));
    assertFalse(filterMapping.matchesWithUrl("foo/between/bar/"));
    assertFalse(filterMapping.matchesWithUrl("foo/*/bar"));
  }

  @Test
  public void testMatchingWithPartialRandomCompexUrl() {
    FilterMapping filterMapping = new FilterMapping(
        "/foo/*/bar/*/q.json",
        "",
        FilterMapping.Type.BEFORE
    );

    assertTrue(filterMapping.matchesWithUrl("/foo/between/bar/foo/q.json"));
    assertTrue(filterMapping.matchesWithUrl("/foo/bar/two/bar/bar/q.json"));
    assertTrue(filterMapping.matchesWithUrl("/foo/foo/1/2/bar/3/q.json"));
    assertFalse(filterMapping.matchesWithUrl("foo/bar/bar/q.json"));
    assertFalse(filterMapping.matchesWithUrl("foo/foo/bar/q.json"));
  }
}
