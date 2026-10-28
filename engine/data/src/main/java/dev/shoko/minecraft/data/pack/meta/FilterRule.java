package dev.shoko.minecraft.data.pack.meta;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * One entry of {@code filter.block} (R47): files of lower-priority packs whose namespace and path
 * match both regular expressions are treated as absent. Immutable.
 *
 * <p>Provisional (R47): the whole text must match. The path tested is the path under {@code
 * data/<namespace>/}, extension included, such as {@code loot_table/blocks/dirt.json}.
 *
 * <p>The expressions come from third-party packs, so a match is bounded: it may read a number of
 * characters proportional to the length of the name (see {@link #matches(String, String)}), and a
 * name so long that the matcher overflows its stack is retried on a thread with a large stack.
 * Beyond that the match gives up with a {@link FilterBudgetException}. Two rules are equal when
 * their expression texts are.
 */
public final class FilterRule {

  /** Expression standing for an expression absent from {@code pack.mcmeta}: it matches any text. */
  public static final String MATCH_ALL = ".*";

  // Characters a match may read: a fixed part plus a part per character of the name. Real
  // expressions read a few times the length of a path of at most 139 characters (R21).
  private static final long BUDGET_BASE = 100_000;
  private static final long BUDGET_PER_CHAR = 50;
  private static final long LARGE_STACK_BYTES = 256L * 1024 * 1024;

  private final String namespacePattern;
  private final String pathPattern;
  private final Pattern namespace;
  private final Pattern path;

  private FilterRule(String namespacePattern, String pathPattern) {
    this.namespacePattern = namespacePattern;
    this.pathPattern = pathPattern;
    this.namespace = compile(namespacePattern);
    this.path = compile(pathPattern);
  }

  /**
   * Builds a rule from the two expressions, using {@link #MATCH_ALL} for an absent one (R47).
   *
   * @param namespacePattern the expression for the namespace
   * @param pathPattern the expression for the path
   * @return the rule
   * @throws PatternSyntaxException when an expression is not a valid regular expression, or is so
   *     deeply nested that compiling it overflows the stack
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
   * Tells whether a file is hidden by this rule. Each expression may read at most {@code 100000 +
   * 50 * length} characters of its text.
   *
   * @param namespace the namespace of the file
   * @param path the path of the file under {@code data/<namespace>/}
   * @return whether both expressions match the whole text
   * @throws FilterBudgetException when an expression exceeds the budget or the large stack
   */
  public boolean matches(String namespace, String path) {
    return matchesWhole(this.namespacePattern, this.namespace, namespace)
        && matchesWhole(this.pathPattern, this.path, path);
  }

  /**
   * Compares the expression texts.
   *
   * @param other any object
   * @return whether {@code other} is a rule with the same two expressions
   */
  @Override
  public boolean equals(Object other) {
    return other instanceof FilterRule that
        && namespacePattern.equals(that.namespacePattern)
        && pathPattern.equals(that.pathPattern);
  }

  /**
   * Returns a hash consistent with {@link #equals(Object)}.
   *
   * @return the hash
   */
  @Override
  public int hashCode() {
    return Objects.hash(namespacePattern, pathPattern);
  }

  /** Compiles an expression; a nesting that overflows the compiler's stack is a syntax error. */
  static Pattern compile(String expression) {
    try {
      return Pattern.compile(expression, Pattern.DOTALL);
    } catch (StackOverflowError e) {
      throw new PatternSyntaxException("expression nested too deeply", expression, -1);
    }
  }

  private static boolean matchesWhole(String expression, Pattern pattern, String text) {
    if (expression.equals(MATCH_ALL)) {
      return true;
    }
    try {
      return matchWithinBudget(pattern, text);
    } catch (StackOverflowError e) {
      return matchOnLargeStack(pattern, text);
    }
  }

  private static boolean matchWithinBudget(Pattern pattern, String text) {
    long budget = BUDGET_BASE + BUDGET_PER_CHAR * text.length();
    return pattern.matcher(new CountedChars(text, budget)).matches();
  }

  private static boolean matchOnLargeStack(Pattern pattern, String text) {
    FutureTask<Boolean> task = new FutureTask<>(() -> matchWithinBudget(pattern, text));
    Thread worker = new Thread(null, task, "filter-rule", LARGE_STACK_BYTES);
    worker.setDaemon(true);
    worker.start();
    try {
      return task.get();
    } catch (ExecutionException e) {
      if (e.getCause() instanceof FilterBudgetException budget) {
        throw budget;
      }
      throw new FilterBudgetException("the expression needs more stack than available");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new FilterBudgetException("interrupted while evaluating an expression");
    }
  }

  /** A text that counts the characters the matcher reads and gives up past a budget. */
  private static final class CountedChars implements CharSequence {

    private final String text;
    private long remaining;

    CountedChars(String text, long budget) {
      this.text = text;
      this.remaining = budget;
    }

    @Override
    public int length() {
      return text.length();
    }

    @Override
    public char charAt(int index) {
      if (--remaining < 0) {
        throw new FilterBudgetException("the expression reads too many characters");
      }
      return text.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
      return text.subSequence(start, end);
    }

    @Override
    public String toString() {
      return text;
    }
  }
}
