package dev.shoko.minecraft.architecture.fixture.determinism.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Violation: Collections.shuffle without a seeded source. */
public final class UsesCollectionsShuffle {
  public List<Integer> value() {
    List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
    Collections.shuffle(list);
    return list;
  }
}
