package xyz.peatral.toolboxutils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(ToolboxUtils.ID)
public class ToolboxUtils {
    public static final String ID = "toolboxutils";

    public ToolboxUtils(IEventBus modEventBus) {
        ToolboxDataComponents.register(modEventBus);
    }
}
