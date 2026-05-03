package xyz.peatral.toolboxutils.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Dynamic;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.peatral.toolboxutils.toolbox.ToolboxProxyHandler;
import xyz.peatral.toolboxutils.toolbox.concierge.AttributeModifiers;
import xyz.peatral.toolboxutils.toolbox.concierge.ConciergeAi;

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

    @WrapOperation(
            method = "customServerAiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/allay/AllayAi;updateActivity(Lnet/minecraft/world/entity/animal/allay/Allay;)V"
            )
    )
    public void customServerAiStep(Allay allay, Operation<Void> original) {
        ItemStack stack = getItemInHand(InteractionHand.MAIN_HAND);
        if (AllTags.AllItemTags.TOOLBOXES.matches(stack)) {
            brain.setActiveActivityIfPossible(Activity.WORK);
        } else {
            original.call(allay);
        }
    }

    @WrapMethod(method = "makeBrain")
    public Brain<?> makeBrain(Dynamic<?> dynamic, Operation<Brain<?>> original) {
        return ConciergeAi.makeBrain((Brain<Allay>) original.call(dynamic));
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            if (AllTags.AllItemTags.TOOLBOXES.matches(stack)) {
                getPersistentData().putBoolean("ConciergeHat", true);
                getAttributes().addTransientAttributeModifiers(AttributeModifiers.conciergeModifier);
            } else if (getPersistentData().contains("ConciergeHat")) {
                getPersistentData().remove("ConciergeHat");
                getAttributes().removeAttributeModifiers(AttributeModifiers.conciergeModifier);
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
