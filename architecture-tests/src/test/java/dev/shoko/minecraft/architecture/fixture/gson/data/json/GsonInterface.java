package dev.shoko.minecraft.architecture.fixture.gson.data.json;

import com.google.gson.InstanceCreator;

/** Violation: a public interface extends a Gson type. */
public interface GsonInterface extends InstanceCreator<String> {}
