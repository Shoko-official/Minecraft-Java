package dev.shoko.minecraft.data.catalog;

import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.Severity;
import dev.shoko.minecraft.data.id.Identifier;

/**
 * What a file of a pack is, once stacked: an element of a registry, a tag, an opaque content file,
 * pack metadata, or something ignored.
 */
public sealed interface FileAttribution
    permits FileAttribution.Element,
        FileAttribution.Tag,
        FileAttribution.Content,
        FileAttribution.Metadata,
        FileAttribution.Ignored {

  /**
   * A JSON element of a registry loaded from files (R8).
   *
   * @param registry the registry, such as {@code minecraft:loot_table}
   * @param id the identifier of the element, such as {@code sonde:a/b}
   */
  record Element(Identifier registry, Identifier id) implements FileAttribution {}

  /**
   * A tag file (R9).
   *
   * @param registry the registry the tag belongs to
   * @param id the identifier of the tag, without the {@code #}
   */
  record Tag(Identifier registry, Identifier id) implements FileAttribution {}

  /**
   * An opaque function or structure file (R10).
   *
   * @param registry {@code minecraft:function} or {@code minecraft:structure}
   * @param id the identifier of the content
   */
  record Content(Identifier registry, Identifier id) implements FileAttribution {}

  /**
   * A file that is part of the pack format and takes no part in loading, with no diagnostic: {@code
   * pack.mcmeta}, {@code pack.png} and the {@code .mcassetsroot} marker (R13).
   */
  record Metadata() implements FileAttribution {}

  /**
   * A file the loader does not use, with the diagnostic to report for it (R12, R15, R16, R24).
   *
   * @param severity how serious it is, {@link Severity#INFO} for rules that ignore by design
   * @param code the cause, such as {@code UNKNOWN_FOLDER} or {@code INVALID_IDENTIFIER}
   * @param message a description for humans
   */
  record Ignored(Severity severity, DiagnosticCode code, String message)
      implements FileAttribution {}
}
