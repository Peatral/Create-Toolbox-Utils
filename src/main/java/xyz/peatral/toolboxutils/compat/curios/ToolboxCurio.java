package xyz.peatral.toolboxutils.compat.curios;

import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import xyz.peatral.toolboxutils.proxy.ToolboxProxyHandler;

public record ToolboxCurio(ItemStack itemStack) implements ICurio {
    @Override
    public ItemStack getStack() {
        return itemStack;
    }

    @Override
    public void curioTick(SlotContext slotContext) {
        ToolboxProxyHandler.tickToolbox(slotContext.entity(), itemStack);
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack) {
        ToolboxProxyHandler.removeToolbox(slotContext.entity(), itemStack);
    }
}