package xyz.peatral.toolboxutils.forge;

import net.minecraftforge.fml.common.Mod;

import xyz.peatral.toolboxutils.ToolboxUtils;

@Mod(ToolboxUtils.MOD_ID)
public final class ToolboxUtilsForge {
    public ToolboxUtilsForge() {
        ToolboxUtils.init();
    }
}
