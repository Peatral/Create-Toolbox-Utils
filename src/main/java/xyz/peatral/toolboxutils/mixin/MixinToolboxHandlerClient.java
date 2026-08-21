package xyz.peatral.toolboxutils.mixin;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.equipment.toolbox.RadialToolboxMenu;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandlerClient;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.platform.services.NetworkHelper;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.peatral.toolboxutils.network.ToolboxProxyEquipPacket;
import xyz.peatral.toolboxutils.toolbox.IExtendedToolbox;
import xyz.peatral.toolboxutils.toolbox.proxy.ToolboxProxyController;
import xyz.peatral.toolboxutils.toolbox.proxy.ToolboxProxyHandler;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.simibubi.create.foundation.gui.AllGuiTextures.*;

@Mixin(ToolboxHandlerClient.class)
public class MixinToolboxHandlerClient {
    @WrapOperation(
            method = "onPickItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/createmod/catnip/platform/services/NetworkHelper;sendToServer(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V"
            )
    )
    private static void wrapEquipPacket(
            NetworkHelper instance,
            CustomPacketPayload packet,
            Operation<Void> original,
            @Local(name = "toolboxBlockEntity") ToolboxBlockEntity toolboxBlockEntity,
            @Local(name = "comp") int comp,
            @Local(name = "player") LocalPlayer player
    ) {
        if (IExtendedToolbox.isProxy(toolboxBlockEntity)) {
            instance.sendToServer(new ToolboxProxyEquipPacket(
                    toolboxBlockEntity.getUniqueId(),
                    comp,
                    player.getInventory().selected
            ));
        } else {
            original.call(instance, packet);
        }
    }

    @Inject(
            method = "onKeyInput",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/CompoundTag;getCompound(Ljava/lang/String;)Lnet/minecraft/nbt/CompoundTag;",
                    ordinal = 0
            ),
            cancellable = true
    )
    private static void onKeyInput(
            int key,
            boolean pressed,
            CallbackInfo ci,
            @Local(name = "player") LocalPlayer player,
            @Local(name = "level") Level level
    ) {
        List<ToolboxBlockEntity> toolboxes = ToolboxHandler.getNearest(level, player, 8);
        toolboxes.sort(Comparator.comparing(ToolboxBlockEntity::getUniqueId));

        CompoundTag compound = player.getPersistentData()
                .getCompound(ToolboxProxyHandler.PERSISTENT_KEY);

        String slotKey = String.valueOf(player.getInventory().selected);
        boolean equipped = compound.contains(slotKey);

        if (equipped) {
            UUID uuid = NbtUtils.loadUUID(NBTHelper.getINBT(compound.getCompound(slotKey), "UUID"));
            ToolboxProxyController proxy = ToolboxProxyHandler.getProxy(level, uuid);
            if (proxy == null) {
                ci.cancel();
                return;
            }
            BlockPos pos = proxy.getBlockPos();
            double max = ToolboxHandler.getMaxRange(player);
            boolean canReachToolbox = ToolboxHandler.distance(player.position(), pos) < max * max;

            if (canReachToolbox) {
                RadialToolboxMenu screen = new RadialToolboxMenu(toolboxes,
                        RadialToolboxMenu.State.SELECT_ITEM_UNEQUIP, proxy.getToolbox());
                screen.prevSlot(compound.getCompound(slotKey)
                        .getInt("Slot"));
                ScreenOpener.open(screen);
                ci.cancel();
                return;
            }

            ScreenOpener.open(new RadialToolboxMenu(ImmutableList.of(), RadialToolboxMenu.State.DETACH, null));
            ci.cancel();
        }
    }

    @Inject(
            method = "renderOverlay",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;getPersistentData()Lnet/minecraft/nbt/CompoundTag;",
                    ordinal = 0
            )
    )
    private static void renderOverlay(
            GuiGraphics guiGraphics,
            DeltaTracker deltaTracker,
            CallbackInfo ci,
            @Local(name = "x") int x,
            @Local(name = "y") int y,
            @Local(name = "player") Player player
    ) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(ToolboxProxyHandler.PERSISTENT_KEY)) {
            return;

        CompoundTag compound = player.getPersistentData()
                .getCompound(ToolboxProxyHandler.PERSISTENT_KEY);

        if (compound.isEmpty())
            return;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        for (int slot = 0; slot < 9; slot++) {
            String key = String.valueOf(slot);
            if (!compound.contains(key))
                continue;
            UUID uuid = NbtUtils.loadUUID(NBTHelper.getINBT(compound.getCompound(key), "UUID"));
            ToolboxProxyController proxy = ToolboxProxyHandler.getProxy(player.level(), uuid);
            if (proxy == null) {
                continue;
            }
            BlockPos pos = proxy.getBlockPos();
            double max = ToolboxHandler.getMaxRange(player);
            boolean selected = player.getInventory().selected == slot;
            int offset = selected ? 1 : 0;
            AllGuiTextures texture = ToolboxHandler.distance(player.position(), pos) < max * max
                    ? selected ? TOOLBELT_SELECTED_ON : TOOLBELT_HOTBAR_ON
                    : selected ? TOOLBELT_SELECTED_OFF : TOOLBELT_HOTBAR_OFF;
            texture.render(guiGraphics, x + 20 * slot - offset, y + offset);
        }
        poseStack.popPose();
    }

}
