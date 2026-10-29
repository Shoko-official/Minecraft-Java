package dev.shoko.minecraft.item.hunt;

import static org.assertj.core.api.Assertions.assertThat;

import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.stack.ItemStack;
import dev.shoko.minecraft.item.stack.StackCodec;
import dev.shoko.minecraft.item.testing.TestItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class CommandTextFuzzHuntTest {

  private static final String[] PIECES = {
    "stone",
    "minecraft:",
    "[",
    "]",
    "=",
    ",",
    "!",
    "{",
    "}",
    "\"",
    "'",
    "\\",
    "rarity",
    "custom_data",
    "damage",
    "1",
    "-1",
    "99999999999",
    "1.5f",
    "0b",
    "epic",
    "max_stack_size",
    " ",
    ":",
    "#",
    "(",
    ")",
    "enchantments",
    "unbreakable",
    "{}",
    "[]",
    "\"x\"",
    "'y'",
    "container",
    "food",
    "NaN",
    "1e999"
  };

  @Test
  @Timeout(120)
  void arbitraryCommandTextIsAFindingNeverAnException() {
    StackCodec codec = TestItems.stackCodec();
    Random random = new Random(777);
    List<String> crashes = new ArrayList<>();
    int accepted = 0;
    for (int i = 0; i < 20000; i++) {
      StringBuilder text = new StringBuilder(random.nextInt(4) == 0 ? "" : "stone");
      int n = random.nextInt(14);
      for (int k = 0; k < n; k++) {
        text.append(PIECES[random.nextInt(PIECES.length)]);
      }
      String input = text.toString();
      try {
        Decoding<ItemStack> decoded = codec.fromCommandText(input);
        if (decoded instanceof Decoding.Success<ItemStack> ok && !ok.value().isEmpty()) {
          accepted++;
          String again = codec.toCommandText(ok.value());
          Decoding<ItemStack> reread = codec.fromCommandText(again);
          if (!(reread instanceof Decoding.Success<ItemStack> r) || !r.value().equals(ok.value())) {
            crashes.add("round trip " + input + " -> " + again);
          }
        }
      } catch (RuntimeException | StackOverflowError e) {
        crashes.add(input + " -> " + e);
      }
      if (crashes.size() > 10) {
        break;
      }
    }
    assertThat(accepted).isGreaterThan(100);
    assertThat(crashes).isEmpty();
  }
}
