package dev.shoko.minecraft.data.pack;

import dev.shoko.minecraft.data.diag.Diagnostic;
import dev.shoko.minecraft.data.diag.DiagnosticCode;
import dev.shoko.minecraft.data.diag.FileOrigin;
import dev.shoko.minecraft.data.diag.JsonPath;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.json.JsonNode;
import dev.shoko.minecraft.data.json.JsonReader;
import dev.shoko.minecraft.data.pack.meta.PackMeta;
import dev.shoko.minecraft.data.pack.meta.PackMetaReader;
import dev.shoko.minecraft.data.pack.meta.PackType;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;

/** One pack: an identifier, an origin, its files and its metadata. Immutable. */
public final class Pack {

  static final String META_FILE = "pack.mcmeta";

  private final String id;
  private final PackOrigin origin;
  private final PackType type;
  private final PackMeta meta;
  private final PackFiles files;

  private Pack(String id, PackOrigin origin, PackType type, PackMeta meta, PackFiles files) {
    this.id = id;
    this.origin = origin;
    this.type = type;
    this.meta = meta;
    this.files = files;
  }

  /**
   * Opens a pack by reading its {@code pack.mcmeta} (R4, R7).
   *
   * @param id the pack identifier shown by the game, such as {@code minecart_improvements} or
   *     {@code file/mypack} (R40)
   * @param origin where the pack comes from
   * @param type the kind of pack
   * @param files the files of the pack
   * @return the pack, or empty when there is no usable {@code pack.mcmeta} (R7: the pack is
   *     ignored, with a {@code PACK_INVALID} warning on the origin {@code (id, pack.mcmeta)}, plus
   *     the {@code JSON_*} error when the file is not valid JSON). A pack whose format fields are
   *     unusable is present and broken (R4, R5), with the diagnostics of {@code PackMetaReader}
   * @throws IOException when {@code pack.mcmeta} cannot be read from the files
   */
  public static Outcome<Pack> open(String id, PackOrigin origin, PackType type, PackFiles files)
      throws IOException {
    FileOrigin metaOrigin = new FileOrigin(id, META_FILE);
    byte[] bytes;
    try {
      bytes = files.read(META_FILE);
    } catch (NoSuchFileException e) {
      return notAPack(metaOrigin, List.of(), "no pack.mcmeta");
    }
    Outcome<JsonNode> json = JsonReader.read(bytes, metaOrigin);
    if (json.value().isEmpty()) {
      return notAPack(metaOrigin, json.diagnostics(), "pack.mcmeta is not valid JSON");
    }
    Outcome<PackMeta> meta = PackMetaReader.read(json.value().get());
    List<Diagnostic> diagnostics = new ArrayList<>(json.diagnostics());
    diagnostics.addAll(meta.diagnostics());
    if (meta.value().isEmpty()) {
      return Outcome.failure(diagnostics);
    }
    return Outcome.success(new Pack(id, origin, type, meta.value().get(), files), diagnostics);
  }

  /**
   * Builds a pack that has no {@code pack.mcmeta}, such as the implicit vanilla pack (R3), with
   * {@link PackMeta#implicit(String, PackType)} as metadata.
   *
   * @param id the pack identifier
   * @param origin where the pack comes from
   * @param type the kind of pack
   * @param files the files of the pack
   * @return the pack
   */
  public static Pack implicit(String id, PackOrigin origin, PackType type, PackFiles files) {
    return new Pack(id, origin, type, PackMeta.implicit(id, type), files);
  }

  private static Outcome<Pack> notAPack(
      FileOrigin metaOrigin, List<Diagnostic> found, String reason) {
    List<Diagnostic> diagnostics = new ArrayList<>(found);
    diagnostics.add(
        new Diagnostic(DiagnosticCode.PACK_INVALID, metaOrigin, JsonPath.root(), reason));
    return Outcome.failure(diagnostics);
  }

  /**
   * Returns the identifier.
   *
   * @return the identifier
   */
  public String id() {
    return id;
  }

  /**
   * Returns where the pack comes from.
   *
   * @return the origin
   */
  public PackOrigin origin() {
    return origin;
  }

  /**
   * Returns the kind of pack.
   *
   * @return the type
   */
  public PackType type() {
    return type;
  }

  /**
   * Returns the metadata.
   *
   * @return the content of {@code pack.mcmeta}, or the implicit metadata
   */
  public PackMeta meta() {
    return meta;
  }

  /**
   * Returns the files, root of the pack.
   *
   * @return the file access
   */
  public PackFiles files() {
    return files;
  }
}
