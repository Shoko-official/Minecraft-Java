package dev.shoko.minecraft.data.pack.meta;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * One entry of {@code filter.block} (R47): files of lower-priority packs whose namespace and path
 * match both regular expressions are treated as absent. Immutable.
 *
 * <p>Provisional (R47): the whole text must match. The path tested is the path under {@code
 * data/<namespace>/}, extension included, such as {@code loot_table/blocks/dirt.json}.
 */
public final class FilterRule {

  /** Expression standing for an expression absent from {@code pack.mcmeta}: it matches any text. */
  public static final String MATCH_ALL = ".*";

  private final String namespacePattern;
  private final String pathPattern;
  private final Pattern namespace;
  private final Pattern path;

  private FilterRule(String namespacePattern, String pathPattern) {
    this.namespacePattern = namespacePattern;
    this.pathPattern = pathPattern;
    this.namespace = Pattern.compile(namespacePattern, Pattern.DOTALL);
    this.path = Pattern.compile(pathPattern, Pattern.DOTALL);
  }

  /**
   * Builds a rule from the two expressions, using {@link #MATCH_ALL} for an absent one (R47).
   *
   * @param namespacePattern the expression for the namespace
   * @param pathPattern the expression for the path
   * @return the rule
   * @throws PatternSyntaxException when an expression is not a valid regular expression
   */
  public static FilterRule of(String namespacePattern, String pathPattern) {
    return new FilterRule(namespacePattern, pathPattern);
  }

  /**
   * Returns the expression for the namespace.
   *
   * @return the expression text
   */
  public String namespacePattern() {
    return namespacePattern;
  }

  /**
   * Returns the expression for the path.
   *
   * @return the expression text
   */
  public String pathPattern() {
    return pathPattern;
  }

  /**
   * Tells whether a file is hidden by this rule.
   *
   * @param namespace the namespace of the file
   * @param path the path of the file under {@code data/<namespace>/}
   * @return whether both expressions match the whole text
   */
  public boolean matches(String namespace, String path) {
    return this.namespace.matcher(namespace).matches() && this.path.matcher(path).matches();
  }
}
