package dev.shoko.minecraft.data.loader;

import dev.shoko.minecraft.data.pack.Pack;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps the files of a list of packs open for the length of a load, so that an archive is opened
 * once instead of once per file. Closing releases them all, even when one of them fails to close.
 */
final class OpenFiles implements Closeable {

  private final List<Closeable> handles = new ArrayList<>();

  OpenFiles(List<Pack> packs) throws IOException {
    try {
      for (Pack pack : packs) {
        handles.add(pack.files().keepOpen());
      }
    } catch (IOException | RuntimeException e) {
      closeAll(e);
      throw e;
    }
  }

  @Override
  public void close() throws IOException {
    IOException failure = closeAll(null);
    if (failure != null) {
      throw failure;
    }
  }

  private IOException closeAll(Exception primary) {
    IOException failure = null;
    for (Closeable handle : handles) {
      try {
        handle.close();
      } catch (IOException e) {
        if (primary != null) {
          primary.addSuppressed(e);
        } else if (failure == null) {
          failure = e;
        } else {
          failure.addSuppressed(e);
        }
      }
    }
    handles.clear();
    return failure;
  }
}
