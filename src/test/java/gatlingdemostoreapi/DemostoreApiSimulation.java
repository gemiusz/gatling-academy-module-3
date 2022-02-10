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

  private static String getProperty(String propertyName, String defaultValue) {
    String envValue = System.getenv(propertyName);
    return envValue != null ? envValue : System.getProperty(propertyName, defaultValue);
  }

  private static int userCount = Integer.parseInt(getProperty("USERS", "5"));
  private static int rampDuration = Integer.parseInt(getProperty("RAMP_DURATION", "10"));
  private static int testDuration = Integer.parseInt(getProperty("DURATION", "60"));

  private static ChainBuilder initSession = exec(session -> session.set("authenticated", false));

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
