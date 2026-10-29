package dev.shoko.minecraft.data.hunt;

import dev.shoko.minecraft.data.codec.Codec;
import dev.shoko.minecraft.data.codec.Codecs;
import dev.shoko.minecraft.data.codec.UnknownFields;
import dev.shoko.minecraft.data.diag.Outcome;
import dev.shoko.minecraft.data.tag.TagFile;
import dev.shoko.minecraft.data.tag.TagFileReader;
import dev.shoko.minecraft.data.testing.Cx;
import dev.shoko.minecraft.data.testing.Diags;
import dev.shoko.minecraft.data.testing.Jx;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

// spec: data-datapack-loading R62 g (all the diagnostics of a file, in the order of the text) and
// R63 b (unknown fields of a strict object, in the order of the text).
class DiagnosticOrderHuntTest {

  private static Codec<Integer> sumOfAAndB() {
    return Codecs.object(
        reader -> {
          Supplier<Integer> a = reader.required("a", Codecs.integer());
          Supplier<Integer> b = reader.required("b", Codecs.integer());
          return () -> a.get() + b.get();
        });
  }

  @Test
  void objectDiagnosticsFollowTheTextOrderNotTheDeclarationOrder() {
    Diags.assertSummary(
        Cx.decode(sumOfAAndB(), "{\"b\":\"x\",\"a\":\"y\"}"),
        "CODEC_TYPE_MISMATCH@$.b",
        "CODEC_TYPE_MISMATCH@$.a");
    Diags.assertSummary(
        Cx.decode(sumOfAAndB(), "{\"a\":\"y\",\"b\":\"x\"}"),
        "CODEC_TYPE_MISMATCH@$.a",
        "CODEC_TYPE_MISMATCH@$.b");
  }

  @Test
  void strictUnknownFieldsAreInterleavedWithOtherErrorsInTextOrder() {
    Codec<Integer> strict =
        Codecs.object(UnknownFields.REJECT, reader -> reader.required("a", Codecs.integer()));
    Diags.assertSummary(
        Cx.decode(strict, "{\"zzz\":1,\"a\":\"y\"}"),
        "CODEC_UNKNOWN_FIELD@$.zzz",
        "CODEC_TYPE_MISMATCH@$.a");
    Diags.assertSummary(
        Cx.decode(strict, "{\"a\":\"y\",\"zzz\":1}"),
        "CODEC_TYPE_MISMATCH@$.a",
        "CODEC_UNKNOWN_FIELD@$.zzz");
  }

  @Test
  void aTagEntryObjectReportsItsFieldsInTextOrder() {
    Outcome<TagFile> idFirst =
        TagFileReader.read(Jx.parse("{\"values\":[{\"id\":5,\"required\":1}]}"));
    Diags.assertSummary(
        idFirst, "CODEC_TYPE_MISMATCH@$.values[0].id", "CODEC_TYPE_MISMATCH@$.values[0].required");
    Outcome<TagFile> requiredFirst =
        TagFileReader.read(Jx.parse("{\"values\":[{\"required\":1,\"id\":5}]}"));
    Diags.assertSummary(
        requiredFirst,
        "CODEC_TYPE_MISMATCH@$.values[0].required",
        "CODEC_TYPE_MISMATCH@$.values[0].id");
  }

  @Test
  void aTagFileReportsItsTopLevelFieldsInTextOrder() {
    Diags.assertSummary(
        TagFileReader.read(Jx.parse("{\"values\":5,\"replace\":1}")),
        "CODEC_TYPE_MISMATCH@$.values",
        "CODEC_TYPE_MISMATCH@$.replace");
    Diags.assertSummary(
        TagFileReader.read(Jx.parse("{\"replace\":1,\"values\":5}")),
        "CODEC_TYPE_MISMATCH@$.replace",
        "CODEC_TYPE_MISMATCH@$.values");
  }
}
