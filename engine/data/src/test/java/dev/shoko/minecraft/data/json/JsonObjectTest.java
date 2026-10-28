package dev.shoko.minecraft.data.json;

import static dev.shoko.minecraft.data.testing.Nodes.arr;
import static dev.shoko.minecraft.data.testing.Nodes.bool;
import static dev.shoko.minecraft.data.testing.Nodes.nul;
import static dev.shoko.minecraft.data.testing.Nodes.num;
import static dev.shoko.minecraft.data.testing.Nodes.obj;
import static dev.shoko.minecraft.data.testing.Nodes.str;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R58, "Arbre JSON" (JsonObject, JsonArray, JsonString, JsonBoolean and
// JsonNull nodes, built without the reader)
class JsonObjectTest {

  @Test
  void getReturnsTheMemberOrEmpty() {
    JsonObject object = obj("a", num("1"), "b", str("x"));
    assertThat(object.get("a")).hasValue(num("1"));
    assertThat(object.get("b")).hasValue(str("x"));
    assertThat(object.get("c")).isEmpty();
    assertThat(object.get("A")).isEmpty();
  }

  @Test
  void typesAreReported() {
    assertThat(obj().type()).isEqualTo(JsonType.OBJECT);
    assertThat(arr().type()).isEqualTo(JsonType.ARRAY);
    assertThat(str("x").type()).isEqualTo(JsonType.STRING);
    assertThat(bool(true).type()).isEqualTo(JsonType.BOOLEAN);
    assertThat(nul().type()).isEqualTo(JsonType.NULL);
  }

  @Test
  void objectsAreEqualWhateverTheMemberOrder() {
    // spec: JsonNode.sameValueAs ("ignoring the order of object members")
    JsonObject ab = obj("a", num("1"), "b", num("2"));
    JsonObject ba = obj("b", num("2"), "a", num("1"));
    assertThat(ab.sameValueAs(ba)).isTrue();
    assertThat(ba.sameValueAs(ab)).isTrue();
    assertThat(ab.members().keySet()).containsExactly("a", "b");
    assertThat(ba.members().keySet()).containsExactly("b", "a");
  }

  @Test
  void objectsDifferByKeyByValueAndBySize() {
    JsonObject base = obj("a", num("1"), "b", num("2"));
    assertThat(base.sameValueAs(obj("a", num("1")))).isFalse();
    assertThat(obj("a", num("1")).sameValueAs(base)).isFalse();
    assertThat(base.sameValueAs(obj("a", num("1"), "c", num("2")))).isFalse();
    assertThat(base.sameValueAs(obj("a", num("1"), "b", num("3")))).isFalse();
    assertThat(base.sameValueAs(obj("a", num("1"), "b", num("2.0")))).isFalse();
    assertThat(obj().sameValueAs(obj())).isTrue();
  }

  @Test
  void comparisonIsDeep() {
    JsonObject left = obj("a", arr(num("1"), obj("x", bool(true))), "b", nul());
    JsonObject same = obj("b", nul(), "a", arr(num("1"), obj("x", bool(true))));
    JsonObject other = obj("b", nul(), "a", arr(num("1"), obj("x", bool(false))));
    assertThat(left.sameValueAs(same)).isTrue();
    assertThat(left.sameValueAs(other)).isFalse();
  }

  @Test
  void arraysCompareElementsInOrder() {
    assertThat(arr(num("1"), num("2")).sameValueAs(arr(num("1"), num("2")))).isTrue();
    assertThat(arr(num("1"), num("2")).sameValueAs(arr(num("2"), num("1")))).isFalse();
    assertThat(arr(num("1")).sameValueAs(arr(num("1"), num("2")))).isFalse();
    assertThat(arr().sameValueAs(arr())).isTrue();
    assertThat(arr(num("1")).sameValueAs(arr(num("1.0")))).isFalse();
    assertThat(arr().sameValueAs(obj())).isFalse();
  }

  @Test
  void stringsBooleansAndNullsCompareTheirValue() {
    assertThat(str("a").sameValueAs(str("a"))).isTrue();
    assertThat(str("a").sameValueAs(str("A"))).isFalse();
    assertThat(str("").sameValueAs(str(""))).isTrue();
    assertThat(str("\ud800").sameValueAs(str("\ud800"))).isTrue();
    assertThat(bool(true).sameValueAs(bool(true))).isTrue();
    assertThat(bool(true).sameValueAs(bool(false))).isFalse();
    assertThat(nul().sameValueAs(nul())).isTrue();
    assertThat(nul().sameValueAs(bool(false))).isFalse();
    assertThat(str("true").sameValueAs(bool(true))).isFalse();
    assertThat(nul().sameValueAs(str("null"))).isFalse();
  }
}
