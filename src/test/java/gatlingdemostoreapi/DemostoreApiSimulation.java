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
    .acceptHeader("application/json");

  private static Map<CharSequence, String> jsonContentHeader =
    Map.of("Content-Type", "application/json");

  private static Map<CharSequence, String> authorizedHeader = Map.ofEntries(
    Map.entry("Content-Type", "application/json"),
    Map.entry("authorization", "Bearer #{jwt}")
  );

  private static class Authentication {
    public static ChainBuilder authenticate =
      exec(http("Authenticate")
        .post("/api/authenticate")
        .headers(jsonContentHeader)
        .body(StringBody("{\"username\": \"admin\",\"password\": \"admin\"}"))
        .check(status().is(200))
        .check(jsonPath("$.token").saveAs("jwt")));
  }

  private static class Categories {
    public static ChainBuilder list =
      exec(http("List categories")
        .get("/api/category")
        .check(jsonPath("$[?(@.id == 6)].name").is("For Her")));

    public static ChainBuilder update =
      exec(http("Update category")
        .put("/api/category/7")
        .headers(authorizedHeader)
        .body(StringBody("{\"name\": \"Everyone\"}"))
        .check(jsonPath("$.name").is("Everyone")));
  }

  private ScenarioBuilder scn = scenario("DemostoreApiSimulation")
    .exec(Categories.list)
    .pause(2)
    .exec(http("List products")
      .get("/api/product?category=7"))
    .pause(2)
    .exec(http("Get product")
      .get("/api/product/34"))
    .pause(2)
    .exec(Authentication.authenticate)
    .pause(2)
    .exec(http("Update product")
      .put("/api/product/34")
      .headers(authorizedHeader)
      .body(RawFileBody("gatlingdemostoreapi/demostoreapisimulation/update-product.json")))
    .pause(2)
    .exec(http("Create product 1")
      .post("/api/product")
      .headers(authorizedHeader)
      .body(RawFileBody("gatlingdemostoreapi/demostoreapisimulation/create-product-1.json")))
    .pause(2)
    .exec(http("Create product 2")
      .post("/api/product")
      .headers(authorizedHeader)
      .body(RawFileBody("gatlingdemostoreapi/demostoreapisimulation/create-product-2.json")))
    .pause(2)
    .exec(http("Create product 3")
      .post("/api/product")
      .headers(authorizedHeader)
      .body(RawFileBody("gatlingdemostoreapi/demostoreapisimulation/create-product-3.json")))
    .pause(2)
    .exec(Categories.update);

  {
	  setUp(scn.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
  }
}
