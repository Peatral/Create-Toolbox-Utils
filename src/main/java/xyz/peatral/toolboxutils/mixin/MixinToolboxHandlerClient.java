package xyz.peatral.toolboxutils.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandlerClient;
import com.simibubi.create.foundation.gui.AllGuiTextures;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.peatral.toolboxutils.network.ToolboxProxyEquipPacket;
import xyz.peatral.toolboxutils.proxy.ToolboxProxy;
import xyz.peatral.toolboxutils.proxy.ToolboxProxyHandler;

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
        if (toolboxBlockEntity instanceof ToolboxProxy proxy) {
            instance.sendToServer(new ToolboxProxyEquipPacket(
                    proxy.getUniqueId(),
                    comp,
                    player.getInventory().selected
            ));
        } else {
            original.call(instance, packet);
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
        if (!persistentData.contains("CreateToolboxProxyData"))
            return;

        CompoundTag compound = player.getPersistentData()
                .getCompound("CreateToolboxProxyData");

        if (compound.isEmpty())
            return;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        for (int slot = 0; slot < 9; slot++) {
            String key = String.valueOf(slot);
            if (!compound.contains(key))
                continue;
            UUID uuid = NbtUtils.loadUUID(NBTHelper.getINBT(compound.getCompound(key), "UUID"));
            ToolboxProxy proxy = ToolboxProxyHandler.getProxy(player.level(), uuid);
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
