package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.component.ComponentCodec;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.registry.ItemType;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.stack.StackCodec;
import dev.shoko.minecraft.item.testing.Json;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Tag("oracle")
class RealComponentsHuntTest {

  @Test
  @Timeout(300)
  void everyPrototypeComponentSurvivesNbtJsonAndTextRoundTrips() {
    RealData.Loaded data = RealData.get();
    ComponentCodec codec = ComponentCodec.of(data.items().components());
    StackCodec stacks = StackCodec.of(data.items());
    ItemType stone = data.type("stone");
    List<String> problems = new ArrayList<>();
    for (Identifier itemId : data.items().registry().ids()) {
      ItemType type = data.items().find(itemId).orElseThrow();
      ItemStack stack = ItemStack.of(stone, 1);
      for (Map.Entry<Identifier, ComponentValue> entry : type.prototype().entries().entrySet()) {
        Identifier id = entry.getKey();
        ComponentValue value = entry.getValue();
        if (!(codec.valueFromNbt(id, value.tag()) instanceof Decoding.Success<ComponentValue> n)
            || !n.value().equals(value)) {
          problems.add("nbt " + type.id() + " " + id);
        }
        String json = codec.valueToJson(id, value);
        if (!(codec.valueFromJson(id, Json.parseStrict(json))
                instanceof Decoding.Success<ComponentValue> j)
            || !j.value().equals(value)) {
          problems.add("json " + type.id() + " " + id + " " + json);
        }
        stack = stack.withComponent(id, value);
      }
      stack = stack.withCount(3);
      if (!(stacks.fromNbt(stacks.toNbt(stack)) instanceof Decoding.Success<ItemStack> a)
          || !a.value().equals(stack)) {
        problems.add("stack nbt " + type.id());
      }
      if (!(stacks.fromJson(Json.parseStrict(stacks.toJson(stack)))
              instanceof Decoding.Success<ItemStack> b)
          || !b.value().equals(stack)) {
        problems.add("stack json " + type.id());
      }
      ItemStack one = stack.withCount(1);
      if (!(stacks.fromCommandText(stacks.toCommandText(one))
              instanceof Decoding.Success<ItemStack> c)
          || !c.value().equals(one)) {
        problems.add("stack text " + type.id());
      }
    }
    assertThat(problems).isEmpty();
  }
}
