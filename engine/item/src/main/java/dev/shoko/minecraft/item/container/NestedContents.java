package dev.shoko.minecraft.item.container;

import dev.shoko.minecraft.item.Decoding;
import dev.shoko.minecraft.item.component.ComponentValue;
import dev.shoko.minecraft.item.stack.StackCodec;
import java.util.List;

/**
 * Typed view of the {@code container} component: a list of {@code {slot, item}} whose items are
 * stacks that follow IC-R14 to IC-R20 (IC-R23). The nesting depth limit and the treatment of a
 * bundle in a bundle are not specified (probe PI4): the view reads one level and reports {@code
 * NESTING_TOO_DEEP} only for a limit the caller chooses.
 */
public final class NestedContents {

  private NestedContents() {}

  /**
   * Creates a view.
   *
   * @param stacks the codec of one stack
   * @param maxDepth the deepest nesting accepted, counting the stacks of the value as depth 1
   * @return the view
   */
  public static NestedContents of(StackCodec stacks, int maxDepth) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Reads the stacks of a {@code container} value.
   *
   * @param value the component value
   * @return the nested stacks in list order, or the failure
   */
  public Decoding<List<SlottedStack>> read(ComponentValue value) {
    return Skeleton.throwNotImplemented();
  }

  /**
   * Builds a {@code container} value.
   *
   * @param stacks the nested stacks
   * @return the value
   */
  public ComponentValue write(List<SlottedStack> stacks) {
    return Skeleton.throwNotImplemented();
  }
}
