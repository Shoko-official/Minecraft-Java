package dev.shoko.minecraft.world.block;

import java.util.List;

/**
 * One block of the report, as read (R1, R2).
 *
 * @param id block identifier with namespace
 * @param properties properties in the order of the report, which is not the order of identifiers
 *     for 12 blocks (R8); empty when the report has no {@code properties} key
 * @param states every state of the report with its identifier, in report order
 */
public record BlockReportEntry(
    String id, List<BlockProperty> properties, List<ReportedState> states) {}
