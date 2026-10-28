package dev.shoko.minecraft.data.pack;

/** The views shared by every {@link PackFiles} implementation of this package. */
abstract class BaseFiles implements PackFiles {

  @Override
  public PackFiles subtree(String directory) {
    return new SubtreeFiles(this, directory);
  }

  @Override
  public PackFiles without(String directory) {
    return new WithoutFiles(this, directory);
  }
}
