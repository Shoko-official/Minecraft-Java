package dev.shoko.minecraft.item;

import java.io.Serial;
import java.util.List;

/**
 * Internal control flow of the decoders: carries the findings of a refused input up to the public
 * entry point, which turns it into a {@link Decoding.Failure}. It has no stack trace and never
 * leaves this module's public methods.
 */
public final class FindingException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final transient List<Finding> findings;

  /**
   * Creates the exception for one finding.
   *
   * @param finding the reason of the refusal
   */
  public FindingException(Finding finding) {
    this(List.of(finding));
  }

  /**
   * Creates the exception for several findings.
   *
   * @param findings the reasons of the refusal, at least one
   */
  public FindingException(List<Finding> findings) {
    super(findings.getFirst().message(), null, false, false);
    this.findings = List.copyOf(findings);
  }

  /**
   * Creates the exception for a code, a path and a message.
   *
   * @param code the cause
   * @param path where
   * @param message for humans
   */
  public FindingException(FindingCode code, String path, String message) {
    this(new Finding(code, path, message));
  }

  /**
   * Returns the findings.
   *
   * @return the reasons of the refusal
   */
  public List<Finding> findings() {
    return findings;
  }
}
