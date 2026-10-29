package dev.shoko.minecraft.architecture.fixture.gson.data.json;

import com.google.gson.TypeAdapter;

/** Violation: a public class extends a Gson type. */
public abstract class GsonSuperclass extends TypeAdapter<String> {}
