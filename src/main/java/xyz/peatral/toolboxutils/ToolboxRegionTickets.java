package xyz.peatral.toolboxutils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.TicketType;

import java.util.UUID;

public class ToolboxRegionTickets {
    public static final TicketType<UUID> TRAVELLING_CONCIERGE = TicketType.create(
            ResourceLocation.fromNamespaceAndPath(ToolboxUtils.ID, "travelling_concierge").toString(),
            UUID::compareTo,
            200
    );
}
