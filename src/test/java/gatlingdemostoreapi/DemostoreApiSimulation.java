package gatlingdemostoreapi;

import java.time.Duration;
import java.util.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import io.gatling.javaapi.jdbc.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
import static io.gatling.javaapi.jdbc.JdbcDsl.*;

public class DemostoreApiSimulation extends Simulation {

  private HttpProtocolBuilder httpProtocol = http
    .baseUrl("http://demostore.gatling.io")
    .inferHtmlResources()
    .acceptHeader("*/*")
    .acceptEncodingHeader("gzip, deflate")
    .userAgentHeader("PostmanRuntime/7.28.0");

  private Map<CharSequence, String> headers_0 = Map.of("Postman-Token", "573f8164-203a-4a4a-a559-88e8220fc1d6");

  private Map<CharSequence, String> headers_1 = Map.of("Postman-Token", "fc0cdc3d-e073-4448-baf7-827c28a86c9f");

  private Map<CharSequence, String> headers_2 = Map.of("Postman-Token", "838d2d7c-d2bf-4bd5-9eb0-b1d7de308624");

  private Map<CharSequence, String> headers_3 = Map.ofEntries(
    Map.entry("Content-Type", "application/json"),
    Map.entry("Postman-Token", "80504acb-0cb6-476e-ba89-21fd9b752f72")
  );

  private Map<CharSequence, String> headers_4 = Map.ofEntries(
    Map.entry("Content-Type", "application/json"),
    Map.entry("Postman-Token", "2e825324-c139-4a18-b49d-f564f63a8882"),
    Map.entry("authorization", "Bearer #{jwt}")
  );

  private Map<CharSequence, String> headers_5 = Map.ofEntries(
    Map.entry("Content-Type", "application/json"),
    Map.entry("Postman-Token", "517c04c5-5ba1-4247-80d9-1a1d464100ce"),
    Map.entry("authorization", "Bearer #{jwt}")
  );

  private Map<CharSequence, String> headers_6 = Map.ofEntries(
    Map.entry("Content-Type", "application/json"),
    Map.entry("Postman-Token", "082f2f79-c442-490a-b551-37dfdfb39ea3"),
    Map.entry("authorization", "Bearer #{jwt}")
  );

  private Map<CharSequence, String> headers_7 = Map.ofEntries(
    Map.entry("Content-Type", "application/json"),
    Map.entry("Postman-Token", "bb881215-c437-47c8-9628-8f09b2d1a9c6"),
    Map.entry("authorization", "Bearer #{jwt}")
  );

  private Map<CharSequence, String> headers_8 = Map.ofEntries(
    Map.entry("Content-Type", "application/json"),
    Map.entry("Postman-Token", "342dfa3e-703e-45e2-8aaf-78e88d32a0d5"),
    Map.entry("authorization", "Bearer #{jwt}")
  );


  private ScenarioBuilder scn = scenario("DemostoreApiSimulation")
    .exec(http("request_0")
      .get("/api/category")
      .headers(headers_0))
    .pause(2)
    .exec(http("request_1")
      .get("/api/product?category=7")
      .headers(headers_1))
    .pause(2)
    .exec(http("request_2")
      .get("/api/product/34")
      .headers(headers_2))
    .pause(2)
    .exec(http("request_3")
      .post("/api/authenticate")
      .headers(headers_3)
      .body(RawFileBody("gatlingdemostoreapi/demostoreapisimulation/0003_request.json"))
      .check(jsonPath("$.token").saveAs("jwt")))
    .pause(2)
    .exec(http("request_4")
      .put("/api/product/34")
      .headers(headers_4)
      .body(RawFileBody("gatlingdemostoreapi/demostoreapisimulation/0004_request.json")))
    .pause(2)
    .exec(http("request_5")
      .post("/api/product")
      .headers(headers_5)
      .body(RawFileBody("gatlingdemostoreapi/demostoreapisimulation/0005_request.json")))
    .pause(2)
    .exec(http("request_6")
      .post("/api/product")
      .headers(headers_6)
      .body(RawFileBody("gatlingdemostoreapi/demostoreapisimulation/0006_request.json")))
    .pause(2)
    .exec(http("request_7")
      .post("/api/product")
      .headers(headers_7)
      .body(RawFileBody("gatlingdemostoreapi/demostoreapisimulation/0007_request.json")))
    .pause(2)
    .exec(http("request_8")
      .put("/api/category/7")
      .headers(headers_8)
      .body(RawFileBody("gatlingdemostoreapi/demostoreapisimulation/0008_request.json")));

  {
	  setUp(scn.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
  }
}
