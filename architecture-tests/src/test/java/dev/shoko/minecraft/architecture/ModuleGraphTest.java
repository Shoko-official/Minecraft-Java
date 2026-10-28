package dev.shoko.minecraft.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The module graph of the rules is the one of the documentation and of the Gradle build. */
class ModuleGraphTest {

  /** Relative to the working directory of the test task, the architecture-tests project. */
  private static final Path ARCHITECTURE_DOC = Path.of("..", "docs", "ARCHITECTURE.md");

  private static final Set<String> NO_DEPENDENCY = Set.of("rien", "aucun module du moteur");

  @Test
  void documentedModuleTableMatchesTheRules() throws IOException {
    assertThat(documentedDependencies(Files.readAllLines(ARCHITECTURE_DOC)))
        .isEqualTo(ModuleRules.DIRECT);
  }

  @Test
  void gradleModulesMatchTheRules() {
    assertThat(BuildModules.all()).isEqualTo(ModuleRules.DIRECT.keySet());
  }

  @Test
  void worldAndItemDoNotKnowEachOther() {
    assertThat(ModuleRules.reachable("world")).doesNotContain("item");
    assertThat(ModuleRules.reachable("item")).doesNotContain("world");
  }

  @Test
  void toolsOracleCannotReachTheEngine() {
    assertThat(ModuleRules.reachable("tools.oracle")).isEmpty();
  }

  /** Parses the "Modules" table: module path, responsibility, direct dependencies. */
  static Map<String, Set<String>> documentedDependencies(List<String> lines) {
    Map<String, Set<String>> table = new LinkedHashMap<>();
    boolean inTable = false;
    for (String line : lines) {
      if (line.startsWith("| Module |")) {
        inTable = true;
      } else if (inTable && !line.startsWith("|")) {
        break;
      } else if (inTable && !line.startsWith("|---")) {
        String[] cells = line.split("\\|", -1);
        if (cells.length != 5) {
          throw new IllegalStateException("unexpected module table row: " + line);
        }
        if (table.put(moduleName(cells[1]), dependencies(cells[3])) != null) {
          throw new IllegalStateException("module listed twice: " + line);
        }
      }
    }
    return table;
  }

  private static Set<String> dependencies(String cell) {
    String text = cell.strip();
    Set<String> result = new LinkedHashSet<>();
    if (!NO_DEPENDENCY.contains(text)) {
      Arrays.stream(text.split(",")).map(ModuleGraphTest::moduleName).forEach(result::add);
    }
    return result;
  }

  /** {@code engine/core} is {@code core}, {@code tools/oracle} is {@code tools.oracle}. */
  private static String moduleName(String cell) {
    String path = cell.replace("`", "").strip();
    return (path.startsWith("engine/") ? path.substring("engine/".length()) : path)
        .replace('/', '.');
  }
}
