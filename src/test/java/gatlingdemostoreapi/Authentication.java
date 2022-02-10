package gatlingdemostoreapi;

import io.gatling.javaapi.core.ChainBuilder;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class Authentication {
  public static ChainBuilder authenticate =
    doIf(session -> !session.getBoolean("authenticated")).then(
      exec(http("Authenticate")
        .post("/api/authenticate")
        .headers(Headers.jsonContent)
        .body(StringBody("{\"username\": \"admin\",\"password\": \"admin\"}"))
        .check(status().is(200))
        .check(jmesPath("token").saveAs("jwt")))
        .exec(session -> session.set("authenticated", true))
    );
}
