package dev.shoko.minecraft.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import java.util.Map;
import java.util.TreeMap;

/** Package size budget of docs/CONVENTIONS.md: top-level classes per package. */
final class PackageBudget {

  static final int MAX_CLASSES = 25;

  private PackageBudget() {}

  /** Packages holding more than {@code max} top-level classes, with their count. */
  static Map<String, Integer> oversized(JavaClasses classes, int max) {
    Map<String, Integer> counts = new TreeMap<>();
    for (JavaClass javaClass : classes) {
      if (javaClass.isTopLevelClass() && !javaClass.getSimpleName().equals("package-info")) {
        counts.merge(javaClass.getPackageName(), 1, Integer::sum);
      }
    }
    counts.values().removeIf(count -> count <= max);
    return counts;
  }
}
