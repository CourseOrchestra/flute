package ru.curs.flute.rest;

import com.google.gson.Gson;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.curs.celesta.CallContext;
import ru.curs.celesta.CelestaException;
import ru.curs.celesta.PySessionContext;
import ru.curs.celesta.syscursors.UserrolesCursor;
import ru.curs.celesta.vintage.Celesta;
import ru.curs.flute.exception.EFluteCritical;
import ru.curs.flute.source.RestTaskSource;
import spark.Spark;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by ioann on 01.08.2017.
 */
public class RestServiceTest {

  private final static String GLOBAL_USER_ID = "flute";
  private static Celesta celesta;

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


      PySessionContext sc = new PySessionContext("super", "celesta_init");

    try (CallContext context = celesta.callContext(sc)) {
      UserrolesCursor urCursor = new UserrolesCursor(context);
      urCursor.setUserid("testUser");
      urCursor.setRoleid("editor");
      urCursor.insert();
    }

    RestMappingBuilder.getInstance().initRouters(celesta, taskSource, GLOBAL_USER_ID);
  }


  @AfterClass
  public static void destroy() {
    celesta.close();
    Spark.stop();
  }

  @Test
  public void testRestServiceGetJson() {
    TestResponse res = get("/foo");

    assertEquals(200, res.status);
    assertEquals("{\"foo\": 1, \"bar\": 2}", res.body);
  }

  @Test
  public void testFluteParam() throws InterruptedException, EFluteCritical {
    String sourceId = taskSource.getTask().getSourceId();
    assertFalse(sourceId.isEmpty());

    TestResponse res = get("/fluteparam");

    assertEquals(200, res.status);
    assertEquals(sourceId, res.body);
  }


  @Test
  public void testRestServiceGetCustomResponseSuccess() {
    TestResponse res = get("/params?sort=desc&id=5&category=task");

    assertEquals(200, res.status);
    assertEquals("/params?sort=desc&id=5&category=task", res.body);
  }


  @Test
  public void testRestServiceGetCustomResponseWithoutParams() {
    TestResponse res = get("/params");

    assertEquals(422, res.status);
    assertEquals("there are no params received", res.body);
  }


  @Test
  public void testJsonPost() throws Exception {
      TestResponse res = post("/jsonPost", new StringEntity("{\"numb\":1}"));

      assertEquals(200, res.status);
      assertEquals("{\"numb\": 2}", res.body);
  }

  @Test
  public void testBeforeFilter() {
      TestResponse res = get("/beforeTest");

      assertEquals(200, res.status);
      assertEquals("{\"foo\": 2}", res.body);
  }


  @Test
  public void testDoubleBeforeFilter() {
      TestResponse res = get("/double/before/test");

      assertEquals(200, res.status);
      assertEquals("[1, 2, 3]", res.body);
  }

  @Test
  public void testAfterFilter() {
      TestResponse res = get("/afterTest");

      assertEquals(200, res.status);
      assertEquals("{\"foo\": 2}", res.body);
  }

  @Test
  public void testDoubleAfterFilter() {
      TestResponse res = get("/double/after/test");

      assertEquals(200, res.status);
      assertEquals("[1, 2, 3]", res.body);
  }

  @Test
  public void testAfterAndBeforeTestFilter() {
      TestResponse res = get("/afterAndBeforeTest");

      assertEquals(200, res.status);
      assertEquals("[1, 2, 3]", res.body);
  }

  @Test
  public void testGlobalCelestaUserId() {
      TestResponse res = get("/globalCelestaUserIdTest");
      assertEquals(200, res.status);
      assertEquals("{\"userId\": \"" + GLOBAL_USER_ID +"\"}", res.body);
  }

  @Test
  public void testCustomCelestaUserId() {
      TestResponse res = get("/customCelestaUserIdTest");
      assertEquals(200, res.status);
      assertEquals("{\"userId\": \"testUser\"}", res.body);
  }

  @Test
  public void testReturnFromBeforeFilter() {
      TestResponse res = get("/testReturnFromBefore");
      assertEquals(200, res.status);
      assertEquals("{\"foo\": 1}", res.body);
  }


  @Test
  public void testReturnFromHandler() {
      TestResponse res = get("/testReturnFromHandler");
      assertEquals(200, res.status);
      assertEquals("{\"foo\": 1}", res.body);
  }

  @Test
  public void testReturnFromAfterFilter() {
      TestResponse res = get("/testReturnFromAfter");
      assertEquals(200, res.status);
      assertEquals("{\"foo\": 1}", res.body);
  }

  @Test
  public void testNoResultForHandler() {
      TestResponse res = get("/testNoResultForHandler");
      assertEquals(200, res.status);
      assertEquals("", res.body);
  }


  @Test
  public void testNoResultForAfterFilter() {
      TestResponse res = get("/testNoResultForAfterFilter");
      assertEquals(200, res.status);
      assertEquals("", res.body);
  }

  @Test
  public void testFormUrlencodedSupport() {
      List<NameValuePair> formData = new ArrayList<>();

      formData.add(new BasicNameValuePair("param1", "val1"));
      formData.add(new BasicNameValuePair("param1", "1"));

      formData.add(new BasicNameValuePair("param2", "val2"));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formData, Consts.UTF_8);

      TestResponse res = post("/applicationFormUrlencoded", entity);


      assertEquals(200, res.status);
      assertEquals("param1=val1,1,param2=val2,", res.body);
  }

  @Test
  public void testCountFromDb() {
      TestResponse res = get("/count");
      assertEquals(200, res.status);
      assertEquals("{\"count\": 0}", res.body);
  }

  private TestResponse get(String path) {
      try {
          HttpGet request = new HttpGet("http://localhost:" + Spark.port() + path);
          HttpResponse response = HttpClientBuilder.create().build().execute(request);

          return new TestResponse(response.getStatusLine().getStatusCode(), EntityUtils.toString(response.getEntity()));
      } catch (IOException e) {
          throw new RuntimeException(e);
      }
  }

  private TestResponse post(String path, HttpEntity entity) {
      try {
      HttpPost request = new HttpPost("http://localhost:" + Spark.port() + path);
      if (entity != null)
          request.setEntity(entity);
      HttpResponse response = HttpClientBuilder.create().build().execute(request);
          return new TestResponse(response.getStatusLine().getStatusCode(), EntityUtils.toString(response.getEntity()));
      } catch (IOException e) {
          throw new RuntimeException(e);
      }
  }

  private static class TestResponse {

    public final String body;
    public final int status;

    public TestResponse(int status, String body) {
      this.status = status;
      this.body = body;
    }

    public Map<String,String> json() {
      return new Gson().fromJson(body, HashMap.class);
    }
  }
}

