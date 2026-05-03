package xyz.peatral.toolboxutils.mixin;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.peatral.toolboxutils.toolbox.ToolboxProxyHandler;

import java.util.UUID;

@Mixin(Allay.class)
public abstract class MixinAllay extends PathfinderMob implements InventoryCarrier, VibrationSystem {
    protected MixinAllay(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void tick(CallbackInfo ci) {
        ItemStack stack = getItemInHand(InteractionHand.MAIN_HAND);
        if (AllTags.AllItemTags.TOOLBOXES.matches(stack)) {
            ToolboxProxyHandler.tickToolbox(this, stack);
        }
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            if (AllTags.AllItemTags.TOOLBOXES.matches(stack)) {
                getPersistentData().putBoolean("ConciergeHat", true);
            } else if (getPersistentData().contains("ConciergeHat")) {
                getPersistentData().remove("ConciergeHat");
            }
        }
        ItemStack handStack = getItemBySlot(EquipmentSlot.MAINHAND);
        if (slot == EquipmentSlot.MAINHAND && AllTags.AllItemTags.TOOLBOXES.matches(handStack)) {
            UUID oldUuid = handStack.get(AllDataComponents.TOOLBOX_UUID);
            UUID newUuid = stack.get(AllDataComponents.TOOLBOX_UUID);
            if (oldUuid == null || !oldUuid.equals(newUuid)) {
                ToolboxProxyHandler.removeToolbox(this, handStack);
            }
        }
        super.setItemSlot(slot, stack);
    }
}
