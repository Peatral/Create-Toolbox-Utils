package xyz.peatral.toolboxutils;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

public class ToolboxPartialModels {
    public static final PartialModel CONCIERGE_HAT = entity("concierge_hat");

    private static PartialModel entity(String path) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(ToolboxUtils.ID, "entity/" + path));
    }

    public static void init() {
        // init static fields
    }
}
