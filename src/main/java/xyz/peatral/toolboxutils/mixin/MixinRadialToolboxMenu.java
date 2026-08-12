package xyz.peatral.toolboxutils.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.equipment.toolbox.*;
import net.createmod.catnip.platform.services.NetworkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import xyz.peatral.toolboxutils.network.ToolboxProxyDisposeAllPacket;
import xyz.peatral.toolboxutils.network.ToolboxProxyEquipPacket;
import xyz.peatral.toolboxutils.toolbox.IExtendedToolbox;

import java.util.List;
import java.util.UUID;

@Mixin(RadialToolboxMenu.class)
public class MixinRadialToolboxMenu {
    @Shadow
    private ToolboxBlockEntity selectedBox;
    @Shadow
    private List<ToolboxBlockEntity> toolboxes;

    @WrapOperation(
            method = "removed",
            at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/platform/services/NetworkHelper;sendToServer(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V")
    )
    private void sendToServer(NetworkHelper instance, CustomPacketPayload customPacketPayload, Operation<Void> original) {
        if (IExtendedToolbox.isProxy(selectedBox)) {
            Level level = selectedBox.getLevel();
            if (customPacketPayload instanceof ToolboxDisposeAllPacket(BlockPos pos) && level != null) {
                toolboxes.stream()
                        .filter(box -> box.getBlockPos().equals(pos))
                        .findFirst()
                        .ifPresent(be -> PacketDistributor.sendToServer(new ToolboxProxyDisposeAllPacket(be.getUniqueId())));
                return;
            }
            if (customPacketPayload instanceof ToolboxEquipPacket(
                    BlockPos toolboxPos, int slot, int hotbarSlot
            )) {
                UUID uuid = toolboxPos == null ? null : selectedBox.getUniqueId();
                PacketDistributor.sendToServer(new ToolboxProxyEquipPacket(uuid, slot, hotbarSlot));
                return;
            }
        }
        original.call(instance, customPacketPayload);
    }
}
