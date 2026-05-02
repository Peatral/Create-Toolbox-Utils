package xyz.peatral.toolboxutils.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.equipment.toolbox.RadialToolboxMenu;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxDisposeAllPacket;
import com.simibubi.create.content.equipment.toolbox.ToolboxEquipPacket;
import net.createmod.catnip.platform.services.NetworkHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import xyz.peatral.toolboxutils.network.ToolboxProxyDisposeAllPacket;
import xyz.peatral.toolboxutils.network.ToolboxProxyEquipPacket;
import xyz.peatral.toolboxutils.toolbox.IExtendedToolbox;

import java.util.UUID;

@Mixin(RadialToolboxMenu.class)
public class MixinRadialToolboxMenu {
    @Shadow
    private ToolboxBlockEntity selectedBox;

    @WrapOperation(
            method = "removed",
            at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/platform/services/NetworkHelper;sendToServer(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V")
    )
    private void sendToServer(NetworkHelper instance, CustomPacketPayload customPacketPayload, Operation<Void> original) {
        if (IExtendedToolbox.isProxy(selectedBox)) {
            if (customPacketPayload instanceof ToolboxDisposeAllPacket) {
                PacketDistributor.sendToServer(new ToolboxProxyDisposeAllPacket(selectedBox.getUniqueId()));
                return;
            }
            if (customPacketPayload instanceof ToolboxEquipPacket(
                    net.minecraft.core.BlockPos toolboxPos, int slot, int hotbarSlot
            )) {
                UUID uuid = toolboxPos == null ? null : selectedBox.getUniqueId();
                PacketDistributor.sendToServer(new ToolboxProxyEquipPacket(uuid, slot, hotbarSlot));
                return;
            }
        }
        original.call(instance, customPacketPayload);
    }
}
