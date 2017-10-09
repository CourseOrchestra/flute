package ru.curs.flute.rest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import ru.curs.celesta.*;
import ru.curs.celesta.syscursors.UserRolesCursor;
import ru.curs.flute.exception.EFluteCritical;
import ru.curs.flute.source.RestTaskSource;
import ru.curs.flute.task.FluteTask;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.sql.Connection;
import java.util.Properties;

/**
 * Created by ioann on 01.08.2017.
 */
public class RestServiceTest {

  private final static String GLOBAL_USER_ID = "flute";
  private static Celesta celesta;

  private static WebTestClient fooClient;
  private static WebTestClient fluteParamClient;
  private static WebTestClient paramsClient;
  private static WebTestClient jsonPostClient;
  private static WebTestClient beforeFilterClient;
  private static WebTestClient doubleBeforeFilterClient;
  private static WebTestClient afterFilterClient;
  private static WebTestClient doubleAfterFilterClient;
  private static WebTestClient afterAndBeforeFilterClient;
  private static WebTestClient globalCelestaUserIdClient;
  private static WebTestClient customCelestaUserIdClient;
  private static WebTestClient returnFromBeforeFilterClient;
  private static WebTestClient returnFromHandlerClient;
  private static WebTestClient returnFromAfterFilterClient;
  private static WebTestClient noResultForHandlerClient;
  private static WebTestClient noResultForAfterFilterClient;

  
  private static RestTaskSource taskSource = new RestTaskSource();
  
  @BeforeClass
  public static void setUp() throws Exception {

    Properties p = new Properties();
    p.setProperty("h2.in-memory", "true");
    p.setProperty("score.path", "src/test/resources/score");

    File pyLib = new File("wininstall/pylib");
    p.setProperty("pylib.path", pyLib.getAbsolutePath());

    try {
      celesta = Celesta.createInstance(p);
    } catch (CelestaException e) {
      throw new EFluteCritical(e.getMessage());
    }



    SessionContext sc = new SessionContext("super", "celesta_init");

    try (CallContext context = celesta.callContext(sc)) {
      UserRolesCursor urCursor = new UserRolesCursor(context);
      urCursor.setUserid("testUser");
      urCursor.setRoleid("editor");
      urCursor.insert();
    }


    RestMappingBuilder.getInstance().initRouters(celesta, taskSource, GLOBAL_USER_ID);

    RequestMapping fooMapping = new RequestMapping("/foo", "", "GET");
    fooClient = WebTestClient.bindToRouterFunction(
        RestMappingBuilder.getInstance().getRouters().get(fooMapping)
    ).build();

    RequestMapping fluteParamMapping = new RequestMapping("/fluteparam", "", "GET");
    fluteParamClient = WebTestClient.bindToRouterFunction(
        RestMappingBuilder.getInstance().getRouters().get(fluteParamMapping)
    ).build();
    
    RequestMapping paramsMapping = new RequestMapping("/params", "", "GET");
    paramsClient = WebTestClient.bindToRouterFunction(
        RestMappingBuilder.getInstance().getRouters().get(paramsMapping)
    ).build();

    RequestMapping jsonPostMapping = new RequestMapping("/jsonPost", "", "POST");
    jsonPostClient = WebTestClient.bindToRouterFunction(
        RestMappingBuilder.getInstance().getRouters().get(jsonPostMapping)
    ).build();

    RequestMapping beforeTestMapping = new RequestMapping("/beforeTest", "", "GET");
    beforeFilterClient = WebTestClient.bindToRouterFunction(
        RestMappingBuilder.getInstance().getRouters().get(beforeTestMapping)
    ).build();

    RequestMapping doubleBeforeTestMapping = new RequestMapping("/doubleBeforeTest", "", "GET");
    doubleBeforeFilterClient = WebTestClient.bindToRouterFunction(
        RestMappingBuilder.getInstance().getRouters().get(doubleBeforeTestMapping)
    ).build();

    RequestMapping afterTestMapping = new RequestMapping("/afterTest", "", "GET");
    afterFilterClient = WebTestClient.bindToRouterFunction(
        RestMappingBuilder.getInstance().getRouters().get(afterTestMapping)
    ).build();

    RequestMapping doubleAfterTestMapping = new RequestMapping("/doubleAfterTest", "", "GET");
    doubleAfterFilterClient = WebTestClient.bindToRouterFunction(
        RestMappingBuilder.getInstance().getRouters().get(doubleAfterTestMapping)
    ).build();

    RequestMapping afterAndBeforeFilterTestMapping = new RequestMapping("/afterAndBeforeTest", "", "GET");
    afterAndBeforeFilterClient = WebTestClient.bindToRouterFunction(
        RestMappingBuilder.getInstance().getRouters().get(afterAndBeforeFilterTestMapping)
    ).build();

    RequestMapping globalCelestaUserIdTestMapping = new RequestMapping("/globalCelestaUserIdTest", "", "GET");
    globalCelestaUserIdClient = WebTestClient.bindToRouterFunction(
        RestMappingBuilder.getInstance().getRouters().get(globalCelestaUserIdTestMapping)
    ).build();

    RequestMapping customCelestaUserIdTestMapping = new RequestMapping("/customCelestaUserIdTest", "", "GET");
    customCelestaUserIdClient = WebTestClient.bindToRouterFunction(
        RestMappingBuilder.getInstance().getRouters().get(customCelestaUserIdTestMapping)
    ).build();

    RequestMapping testReturnFromBeforeMapping = new RequestMapping("/testReturnFromBefore", "", "GET");
    returnFromBeforeFilterClient = WebTestClient.bindToRouterFunction(
        RestMappingBuilder.getInstance().getRouters().get(testReturnFromBeforeMapping)
    ).build();

    RequestMapping testReturnFromHandlerMapping = new RequestMapping("/testReturnFromHandler", "", "GET");
    returnFromHandlerClient = WebTestClient.bindToRouterFunction(
        RestMappingBuilder.getInstance().getRouters().get(testReturnFromHandlerMapping)
    ).build();

    RequestMapping testReturnFromAfterMapping = new RequestMapping("/testReturnFromAfter", "", "GET");
    returnFromAfterFilterClient = WebTestClient.bindToRouterFunction(
        RestMappingBuilder.getInstance().getRouters().get(testReturnFromAfterMapping)
    ).build();

    RequestMapping testNoResultForHandlerMapping = new RequestMapping("/testNoResultForHandler", "", "GET");
    noResultForHandlerClient = WebTestClient.bindToRouterFunction(
        RestMappingBuilder.getInstance().getRouters().get(testNoResultForHandlerMapping)
    ).build();

    RequestMapping testNoResultForAfterFilterMapping = new RequestMapping("/testNoResultForAfterFilter", "", "GET");
    noResultForAfterFilterClient = WebTestClient.bindToRouterFunction(
        RestMappingBuilder.getInstance().getRouters().get(testNoResultForAfterFilterMapping)
    ).build();
  }


  @AfterClass
  public static void destroy() {
    celesta.close();
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
  
  
  @Test
  public void testFluteParam() throws InterruptedException, EFluteCritical {
	String sourceId = taskSource.getTask().getSourceId();
	assertFalse(sourceId.isEmpty());
	  
	fluteParamClient.get().uri("/fluteparam")
    .exchange()
    .expectStatus().is2xxSuccessful()
    .expectBody(String.class)
    .isEqualTo(sourceId);
    
  }

  @Test
  public void testBeforeFilter() {
    beforeFilterClient.get().uri("/beforeTest")
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(String.class)
        .isEqualTo("{\"foo\":2}");
  }

  @Test
  public void testDoubleBeforeFilter() {
    doubleBeforeFilterClient.get().uri("/doubleBeforeTest")
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(String.class)
        .isEqualTo("[1,2,3]");
  }

  @Test
  public void testAfterFilter() {
    afterFilterClient.get().uri("/afterTest")
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(String.class)
        .isEqualTo("{\"foo\":2}");
  }

  @Test
  public void testDoubleAfterFilter() {
    doubleAfterFilterClient.get().uri("/doubleAfterTest")
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(String.class)
        .isEqualTo("[1,2,3]");
  }

  @Test
  public void testAfterAndBeforeTestFilter() {
    afterAndBeforeFilterClient.get().uri("/afterAndBeforeTest")
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(String.class)
        .isEqualTo("[1,2,3]");
  }

  @Test
  public void testGlobalCelestaUserId() {
    globalCelestaUserIdClient.get().uri("/globalCelestaUserIdTest")
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(String.class)
        .isEqualTo("{\"userId\":\"" + GLOBAL_USER_ID +"\"}");
  }

  @Test
  public void testCustomCelestaUserId() {
    customCelestaUserIdClient.get().uri("/customCelestaUserIdTest")
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(String.class)
        .isEqualTo("{\"userId\":\"testUser\"}");
  }


  @Test
  public void testReturnFromBeforeFilter() {
    returnFromBeforeFilterClient.get().uri("/testReturnFromBefore")
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(String.class)
        .isEqualTo("{\"foo\":1}");
  }

  @Test
  public void testReturnFromHandler() {
    returnFromHandlerClient.get().uri("/testReturnFromHandler")
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(String.class)
        .isEqualTo("{\"foo\":1}");
  }

  @Test
  public void testReturnFromAfterFilter() {
    returnFromAfterFilterClient.get().uri("/testReturnFromAfter")
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(String.class)
        .isEqualTo("{\"foo\":1}");
  }

  @Test
  public void testNoResultForHandler() {
    noResultForHandlerClient.get().uri("/testNoResultForHandler")
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(String.class)
        .isEqualTo("");
  }

  @Test
  public void testNoResultForAfterFilter() {
    noResultForAfterFilterClient.get().uri("/testNoResultForAfterFilter")
        .exchange()
        .expectStatus().is2xxSuccessful()
        .expectBody(String.class)
        .isEqualTo("");
  }
}

