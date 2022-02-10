package gatlingdemostoreapi;

import java.util.Map;

public class Headers {
  public static Map<CharSequence, String> jsonContent =
    Map.of("Content-Type", "application/json");

  public static Map<CharSequence, String> authorized = Map.ofEntries(
    Map.entry("Content-Type", "application/json"),
    Map.entry("authorization", "Bearer #{jwt}")
  );
}
