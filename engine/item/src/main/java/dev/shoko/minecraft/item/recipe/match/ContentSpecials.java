package dev.shoko.minecraft.item.recipe.match;

import dev.shoko.minecraft.core.nbt.ByteTag;
import dev.shoko.minecraft.core.nbt.CompoundTag;
import dev.shoko.minecraft.core.nbt.IntTag;
import dev.shoko.minecraft.core.nbt.ListTag;
import dev.shoko.minecraft.core.nbt.NbtTag;
import dev.shoko.minecraft.core.nbt.StringTag;
import dev.shoko.minecraft.data.id.Identifier;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.recipe.SpecialRecipe;
import dev.shoko.minecraft.item.recipe.ingredient.RecipeResult;
import dev.shoko.minecraft.item.stack.ItemStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * Recognition of the fireworks and of the dyeing recipes (RC-R45 to RC-R47, RC-R49d, provisional
 * level S). Stacks are sorted by the role they play; the colors of the dyes are not in the data, so
 * the explosion of a star keeps its shape and options and the dyed result keeps the stack of the
 * file: mixing colors is left to {@code game}.
 */
final class ContentSpecials {

  private static final Identifier FIREWORKS = Identifier.minecraft("fireworks");
  private static final Identifier EXPLOSION = Identifier.minecraft("firework_explosion");
  private static final int MAX_GUNPOWDER = 3;
  private static final int MAX_STARS = 7;
  private static final List<String> SHAPES = List.of("burst", "creeper", "large_ball", "star");

  private ContentSpecials() {}

  static Optional<ItemStack> match(SpecialRecipe recipe, List<ItemStack> cells) {
    return switch (recipe.kind()) {
      case FIREWORK_ROCKET -> rocket(recipe, cells);
      case FIREWORK_STAR -> star(recipe, cells);
      case FIREWORK_STAR_FADE, DYE -> dyed(recipe, cells);
      default -> Optional.empty();
    };
  }

  /**
   * Sorts the stacks by role (the first role whose ingredient suits one); empty if one fits none.
   */
  private static Optional<Map<String, List<ItemStack>>> byRole(
      SpecialRecipe recipe, List<ItemStack> cells, List<String> roles) {
    Map<String, List<ItemStack>> sorted = new LinkedHashMap<>();
    roles.forEach(role -> sorted.put(role, new ArrayList<>()));
    for (ItemStack stack : cells) {
      Optional<String> role =
          roles.stream().filter(r -> SpecialMatcher.part(recipe, r).matches(stack)).findFirst();
      if (role.isEmpty()) {
        return Optional.empty();
      }
      sorted.get(role.get()).add(stack);
    }
    return Optional.of(sorted);
  }

  /** RC-R45: a paper, one to three gunpowder and up to seven stars. */
  private static Optional<ItemStack> rocket(SpecialRecipe recipe, List<ItemStack> cells) {
    Optional<Map<String, List<ItemStack>>> roles =
        byRole(recipe, cells, List.of("shell", "fuel", "star"));
    if (roles.isEmpty() || !(recipe.result() instanceof RecipeResult.Fixed fixed)) {
      return Optional.empty();
    }
    int gunpowder = roles.get().get("fuel").size();
    List<ItemStack> stars = roles.get().get("star");
    if (roles.get().get("shell").size() != 1 || gunpowder < 1 || gunpowder > MAX_GUNPOWDER) {
      return Optional.empty();
    }
    if (stars.size() > MAX_STARS) {
      return Optional.empty();
    }
    List<NbtTag> explosions = new ArrayList<>();
    stars.forEach(star -> star.component(EXPLOSION).ifPresent(e -> explosions.add(e.tag())));
    SequencedMap<String, NbtTag> fireworks = new LinkedHashMap<>();
    fireworks.put("flight_duration", new IntTag(gunpowder));
    fireworks.put("explosions", ListTag.of(explosions));
    return Optional.of(
        fixed.stack().withComponent(FIREWORKS, new ComponentValue(CompoundTag.of(fireworks))));
  }

  /** RC-R46: gunpowder, dyes, at most one shape, one trail and one twinkle. */
  private static Optional<ItemStack> star(SpecialRecipe recipe, List<ItemStack> cells) {
    List<String> roles = new ArrayList<>(List.of("fuel", "dye", "trail", "twinkle"));
    SHAPES.forEach(shape -> roles.add("shapes." + shape));
    Optional<Map<String, List<ItemStack>>> sorted = byRole(recipe, cells, roles);
    if (sorted.isEmpty() || !(recipe.result() instanceof RecipeResult.Fixed fixed)) {
      return Optional.empty();
    }
    Map<String, List<ItemStack>> found = sorted.get();
    String shape = "small_ball";
    int shapes = 0;
    for (String name : SHAPES) {
      if (!found.get("shapes." + name).isEmpty()) {
        shape = name;
        shapes += found.get("shapes." + name).size();
      }
    }
    boolean valid =
        found.get("fuel").size() == 1
            && !found.get("dye").isEmpty()
            && shapes <= 1
            && found.get("trail").size() <= 1
            && found.get("twinkle").size() <= 1;
    if (!valid) {
      return Optional.empty();
    }
    SequencedMap<String, NbtTag> explosion = new LinkedHashMap<>();
    explosion.put("shape", new StringTag(shape));
    explosion.put("has_trail", new ByteTag((byte) found.get("trail").size()));
    explosion.put("has_twinkle", new ByteTag((byte) found.get("twinkle").size()));
    return Optional.of(
        fixed.stack().withComponent(EXPLOSION, new ComponentValue(CompoundTag.of(explosion))));
  }

  /** RC-R47, RC-R49d: one target and one or more dyes. */
  private static Optional<ItemStack> dyed(SpecialRecipe recipe, List<ItemStack> cells) {
    Optional<Map<String, List<ItemStack>>> sorted = byRole(recipe, cells, List.of("target", "dye"));
    if (sorted.isEmpty()
        || sorted.get().get("target").size() != 1
        || sorted.get().get("dye").isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(SpecialMatcher.resultOrCopy(recipe, sorted.get().get("target").getFirst()));
  }
}
