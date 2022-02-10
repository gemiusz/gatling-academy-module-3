package gatlingdemostoreapi;

import java.time.Duration;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

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

  private static String getProperty(String propertyName, String defaultValue) {
    String envValue = System.getenv(propertyName);
    return envValue != null ? envValue : System.getProperty(propertyName, defaultValue);
  }

  private static int userCount = Integer.parseInt(getProperty("USERS", "5"));
  private static int rampDuration = Integer.parseInt(getProperty("RAMP_DURATION", "10"));
  private static int testDuration = Integer.parseInt(getProperty("DURATION", "60"));

  private static ChainBuilder initSession = exec(session -> session.set("authenticated", false));

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
          .check(jmesPath("name").isEL("#{categoryName}")));
  }

  private static class Products {

    private static FeederBuilder.Batchable<String> productsFeeder =
      csv("data/products.csv").circular();

    public static ChainBuilder listAll =
      exec(http("List all products")
        .get("/api/product")
        .check(jmesPath("[*]").ofList().saveAs("allProducts")));

    public static ChainBuilder list =
      exec(http("List products")
        .get("/api/product?category=7")
        .check(jmesPath("[? categoryId != '7']").ofList().is(Collections.emptyList()))
        .check(jmesPath("[*].id").ofList().saveAs("allProductIds")));

    public static ChainBuilder get =
      exec(session -> {
        List<Integer> allProductIds = session.getList("allProductIds");
        return session.set("productId", allProductIds.get(new Random().nextInt(allProductIds.size())));
      })
				.exec(http("Get product")
					.get("/api/product/#{productId}")
					.check(jmesPath("id").ofInt().isEL("#{productId}"))
          .check(jmesPath("@").ofMap().saveAs("product")));

    public static ChainBuilder update =
      exec(Authentication.authenticate)
        .exec( session -> {
          Map<String, Object> product = session.getMap("product");
          String description = (String) product.get("description");
              String price = new BigDecimal((String) product.get("price"))
            .divide(new BigDecimal(2), RoundingMode.DOWN)
            .setScale(2, RoundingMode.DOWN)
            .toString();
          return session
            .set("productCategoryId", product.get("categoryId"))
            .set("productName", product.get("name"))
            .set("productDescription", description)
            .set("productImage", product.get("image"))
            .set("productPrice", price)
            .set("productId", product.get("id"));
        })
				.exec(http("Update product #{productName}")
					.put("/api/product/#{productId}")
					.headers(authorizedHeader)
					.body(ElFileBody("gatlingdemostoreapi/demostoreapisimulation/create-product.json"))
          .check(jmesPath("price").isEL("#{productPrice}")));

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

  private static class UserJourneys {
    private static Duration minPause = Duration.ofMillis(200);
    private static Duration maxPause = Duration.ofSeconds(3);

    public static ChainBuilder admin =
      exec(initSession)
        .exec(Categories.list)
        .pause(minPause, maxPause)
        .exec(Products.list)
        .pause(minPause, maxPause)
        .exec(Products.get)
        .pause(minPause, maxPause)
        .exec(Products.update)
        .pause(minPause, maxPause)
        .repeat(3).on(exec(Products.create))
        .pause(minPause, maxPause)
				.exec(Categories.update);

    public static ChainBuilder priceScrapper =
      exec(
        Categories.list,
        Products.listAll
      );

    public static ChainBuilder priceUpdater =
      exec(initSession)
        .exec(Products.listAll)
        .repeat("#{allProducts.size()}", "productIndex").on(
          exec(session -> {
            int index = session.getInt("productIndex");
            List<Object> allProducts = session.getList("allProducts");
            return session.set("product", allProducts.get(index));
          })
            .exec(Products.update)
				);
  }

  private static class Scenarios {
    public static ScenarioBuilder defaultScn = scenario("Default load test")
      .during(Duration.ofSeconds(testDuration))
      .on(
        randomSwitch().on(
          Choice.withWeight(20d, exec(UserJourneys.admin)),
          Choice.withWeight(40d, exec(UserJourneys.priceScrapper)),
          Choice.withWeight(40d, exec(UserJourneys.priceUpdater))
        )
      );

    public static ScenarioBuilder noAdminsScn = scenario("Load test without admin users")
      .during(Duration.ofSeconds(60))
      .on(
        randomSwitch().on(
          Choice.withWeight(60d, exec(UserJourneys.priceScrapper)),
          Choice.withWeight(40d, exec(UserJourneys.priceUpdater))
        )
      );
  }

  {
    setUp(
      Scenarios.defaultScn
        .injectOpen(rampUsers(userCount).during(Duration.ofSeconds(rampDuration)))
        .protocols(httpProtocol),
      Scenarios.noAdminsScn
        .injectOpen(rampUsers(5).during(Duration.ofSeconds(10)))
        .protocols(httpProtocol)
    );
  }
}
