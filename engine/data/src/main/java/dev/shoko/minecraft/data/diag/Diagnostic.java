package dev.shoko.minecraft.data.diag;

/**
 * One problem found while loading: severity, cause, file, place in the file and a message for
 * humans (output list of the spec). A problem that concerns a whole file, such as an unreadable
 * {@code pack.mcmeta}, uses {@link JsonPath#root()}.
 *
 * @param severity how serious it is
 * @param code the stable cause
 * @param origin the file concerned
 * @param path the place in the file
 * @param message a description for humans, not specified
 */
public record Diagnostic(
    Severity severity, DiagnosticCode code, FileOrigin origin, JsonPath path, String message) {}
