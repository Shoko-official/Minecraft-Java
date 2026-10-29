package dev.shoko.minecraft.architecture.fixture.gson.data.json;

import com.google.gson.JsonObject;

/**
 * A public method of a package-private class cannot be called from outside its package, yet the
 * rule reports it: the check looks at the modifiers of the member only, which errs on the side of
 * caution. This fixture pins that behavior.
 */
final class GsonInPackagePrivateClass {
  public JsonObject tree() {
    return new JsonObject();
  }
}
