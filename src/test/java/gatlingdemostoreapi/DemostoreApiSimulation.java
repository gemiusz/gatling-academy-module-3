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
        .check(jmesPath("token").saveAs("jwt")));
  }

  private static class Categories {
    public static ChainBuilder list =
      exec(http("List categories")
        .get("/api/category")
        .check(jmesPath("[? id == `6`].name").ofList().is(List.of("For Her"))));

    public static ChainBuilder update =
      exec(http("Update category")
        .put("/api/category/7")
        .headers(authorizedHeader)
        .body(StringBody("{\"name\": \"Everyone\"}"))
        .check(jmesPath("name").is("Everyone")));
  }

  private static class Products {
    public static ChainBuilder list =
      exec(http("List products")
        .get("/api/product?category=7")
        .check(jmesPath("[? categoryId != '7']").ofList().is(Collections.emptyList())));

    public static ChainBuilder get =
      exec(http("Get product")
        .get("/api/product/34")
        .check(jmesPath("id").ofInt().is(34)));

    public static ChainBuilder update =
      exec(http("Update product")
        .put("/api/product/34")
        .headers(authorizedHeader)
        .body(RawFileBody("gatlingdemostoreapi/demostoreapisimulation/update-product.json"))
        .check(jmesPath("price").is("15.99")));

    public static ChainBuilder create =
      repeat(3, "productCount").on(
        exec(
          http("Create product #{productCount}")
            .post("/api/product")
            .headers(authorizedHeader)
            .body(RawFileBody("gatlingdemostoreapi/demostoreapisimulation/create-product-#{productCount}.json"))
        )
      );
  }

  private ScenarioBuilder scn = scenario("DemostoreApiSimulation")
    .exec(Categories.list)
    .pause(2)
    .exec(Products.list)
    .pause(2)
    .exec(Products.get)
    .pause(2)
    .exec(Authentication.authenticate)
    .pause(2)
    .exec(Products.update)
    .pause(2)
    .exec(Products.create)
    .pause(2)
    .exec(Categories.update);

  {
	  setUp(scn.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
  }
}
