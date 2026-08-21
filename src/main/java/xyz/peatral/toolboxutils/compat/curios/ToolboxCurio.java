package xyz.peatral.toolboxutils.compat.curios;

import com.simibubi.create.AllDataComponents;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import xyz.peatral.toolboxutils.toolbox.proxy.ToolboxProxyHandler;

import java.util.UUID;

public record ToolboxCurio(ItemStack itemStack) implements ICurio {
    @Override
    public ItemStack getStack() {
        return itemStack;
    }

    @Override
    public void curioTick(SlotContext slotContext) {
        ToolboxProxyHandler.tickProxy(slotContext.entity().level(), itemStack, slotContext.entity().blockPosition());
    }

    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack) {
        UUID oldUuid = itemStack.get(AllDataComponents.TOOLBOX_UUID);
        UUID newUuid = newStack.get(AllDataComponents.TOOLBOX_UUID);
        if (oldUuid != null && oldUuid.equals(newUuid)) {
            return;
        }

        ToolboxProxyHandler.removeProxy(slotContext.entity().level(), oldUuid);
    }
}
