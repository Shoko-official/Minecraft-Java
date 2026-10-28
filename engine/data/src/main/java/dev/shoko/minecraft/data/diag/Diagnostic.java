package dev.shoko.minecraft.data.diag;

/**
 * One problem found while loading: cause, file, place in the file and a message for humans (output
 * list of the spec). The severity is the one of the code, {@code code().severity()} (R61). A
 * problem that concerns a whole file or a whole pack uses {@link JsonPath#root()}.
 *
 * @param code the stable cause
 * @param origin the file concerned
 * @param path the place in the file
 * @param message a description for humans, not specified and never checked by tests
 */
public record Diagnostic(DiagnosticCode code, FileOrigin origin, JsonPath path, String message) {}
