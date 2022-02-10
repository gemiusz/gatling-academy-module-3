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

  private ChainBuilder initSession = exec(session -> session.set("authenticated", false));

  private static class Authentication {
    public static ChainBuilder authenticate =
      doIf(session -> !session.getBoolean("authenticated")).then(
        exec(http("Authenticate")
          .post("/api/authenticate")
          .headers(jsonContentHeader)
          .body(StringBody("{\"username\": \"admin\",\"password\": \"admin\"}"))
          .check(status().is(200))
          .check(jmesPath("token").saveAs("jwt")))
          .exec(session -> session.set("authenticated", true))
      );
  }

  private static class Categories {

    private static FeederBuilder.Batchable<String> categoriesFeeder =
      csv("data/categories.csv").random();

    public static ChainBuilder list =
      exec(http("List categories")
        .get("/api/category")
        .check(jmesPath("[? id == `6`].name").ofList().is(List.of("For Her"))));

    public static ChainBuilder update =
      feed(categoriesFeeder)
        .exec(Authentication.authenticate)
        .exec(http("Update category")
          .put("/api/category/#{categoryId}")
          .headers(authorizedHeader)
          .body(StringBody("{\"name\": \"#{categoryName}\"}"))
          .check(jmesPath("name").is("#{categoryName}")));
  }

  private static class Products {

    private static FeederBuilder.Batchable<String> productsFeeder =
      csv("data/products.csv").circular();

    public static ChainBuilder list =
      exec(http("List products")
        .get("/api/product?category=7")
        .check(jmesPath("[? categoryId != '7']").ofList().is(Collections.emptyList())));

    public static ChainBuilder get =
      exec(http("Get product")
        .get("/api/product/34")
        .check(jmesPath("id").ofInt().is(34)));

    public static ChainBuilder update =
      exec(Authentication.authenticate)
        .exec(http("Update product")
          .put("/api/product/34")
          .headers(authorizedHeader)
          .body(RawFileBody("gatlingdemostoreapi/demostoreapisimulation/update-product.json"))
          .check(jmesPath("price").is("15.99")));

    public static ChainBuilder create =
      exec(Authentication.authenticate)
        .feed(productsFeeder)
        .exec(
          http("Create product #{productName}")
            .post("/api/product")
            .headers(authorizedHeader)
            .body(ElFileBody("gatlingdemostoreapi/demostoreapisimulation/create-product.json"))
        );
  }

  private ScenarioBuilder scn = scenario("DemostoreApiSimulation")
    .exec(initSession)
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
    .repeat(3).on(exec(Products.create))
    .pause(2)
    .exec(Categories.update);

  {
	  setUp(scn.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
  }
}
