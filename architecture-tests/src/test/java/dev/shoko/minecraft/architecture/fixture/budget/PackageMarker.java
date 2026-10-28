package dev.shoko.minecraft.architecture.fixture.budget;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/** Makes javac emit a package-info class, which the budget must not count. */
@Target(ElementType.PACKAGE)
public @interface PackageMarker {}
