package ru.curs.flute;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import ru.curs.celesta.Celesta;
import ru.curs.celesta.CelestaException;
import ru.curs.flute.exception.EFluteCritical;
import ru.curs.flute.rest.Mapping;
import ru.curs.flute.rest.RestMappingBuilder;

import java.io.File;
import java.util.Properties;

/**
 * Created by ioann on 01.08.2017.
 */
public class RestServiceTest {

  private static WebTestClient fooClient;
  private static WebTestClient paramsClient;
  private static WebTestClient jsonPostClient;

  @BeforeClass
  public static void setUp() throws Exception {

    Properties p = new Properties();
    p.setProperty("h2.in-memory", "true");
    p.setProperty("score.path", "src/test/resources/score");

    File pyLib = new File("wininstall/pylib");
    p.setProperty("pylib.path", pyLib.getAbsolutePath());

    try {
      Celesta.initialize(p);
    } catch (CelestaException e) {
      throw new EFluteCritical(e.getMessage());
    }

    RestMappingBuilder.getInstance().initRouters(Celesta.getInstance(), "flute");

    Mapping fooMapping = new Mapping("/foo", "", "GET");
    fooClient = WebTestClient.bindToRouterFunction(
        RestMappingBuilder.getInstance().getRouters().get(fooMapping)
    ).build();

    Mapping paramsMapping = new Mapping("/params", "", "GET");
    paramsClient = WebTestClient.bindToRouterFunction(
        RestMappingBuilder.getInstance().getRouters().get(paramsMapping)
    ).build();

    Mapping jsonPostMapping = new Mapping("/jsonPost", "", "POST");
    jsonPostClient = WebTestClient.bindToRouterFunction(
        RestMappingBuilder.getInstance().getRouters().get(jsonPostMapping)
    ).build();
  }


  @Test
  public void testRestServiceGetJson() {
    fooClient.get().uri("/foo").exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(String.class)
        .isEqualTo("{\"foo\":1,\"bar\":2}");
  }

  @Test
  public void testRestServiceGetCustomResponseSuccess() {
    paramsClient.get().uri("/params?sort=desc&id=5&category=task").exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(String.class)
        .isEqualTo("/params?sort=desc&id=5&category=task");
  }

  @Test
  public void testRestServiceGetCustomResponseWithoutParams() {
    paramsClient.get().uri("/params").exchange()
        .expectStatus().is4xxClientError()
        .expectBody(String.class)
        .isEqualTo("there are no params received");
  }


  @Test
  public void testJsonPost() {
    jsonPostClient.post().uri("/jsonPost")
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromObject("{\"numb\":1}"))
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(String.class)
        .isEqualTo("{\"numb\":2}");
  }
}

