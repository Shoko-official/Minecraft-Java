package dev.shoko.minecraft.data.catalog;

import dev.shoko.minecraft.data.id.Identifier;

/**
 * What a file of a pack is, once stacked: an element of a registry, a tag, an opaque content file,
 * pack metadata, a file skipped silently, or a file ignored with a diagnostic.
 */
public sealed interface FileAttribution
    permits FileAttribution.Element,
        FileAttribution.Tag,
        FileAttribution.Content,
        FileAttribution.Metadata,
        FileAttribution.Skipped,
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
   * A file of another extension than the folder takes, ignored without any diagnostic (R12,
   * provisional: the R61 table lists no diagnostic for it).
   */
  record Skipped() implements FileAttribution {}

  /**
   * A file the loader does not use, reported with the {@code FILE_IGNORED} information diagnostic
   * (R15, R16, R24): a file in an unknown folder or in a static registry folder, a tag under a
   * registry that takes none, or a namespace or path that breaks the identifier syntax.
   *
   * @param message a description for humans, naming the cause
   */
  record Ignored(String message) implements FileAttribution {}
}
