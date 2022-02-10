package gatlingdemostoreapi;

import java.util.List;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class Categories {
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
        .headers(Headers.authorized)
        .body(StringBody("{\"name\": \"#{categoryName}\"}"))
        .check(jmesPath("name").isEL("#{categoryName}")));
}
