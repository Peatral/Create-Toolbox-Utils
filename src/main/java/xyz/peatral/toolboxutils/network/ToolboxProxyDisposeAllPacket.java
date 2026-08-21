package xyz.peatral.toolboxutils.network;

import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import com.simibubi.create.content.equipment.toolbox.ToolboxInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.commons.lang3.mutable.MutableBoolean;
import xyz.peatral.toolboxutils.toolbox.IExtendedToolbox;
import xyz.peatral.toolboxutils.ToolboxUtils;
import xyz.peatral.toolboxutils.toolbox.proxy.ToolboxProxyHandler;
import xyz.peatral.toolboxutils.toolbox.proxy.ToolboxProxyController;

import java.util.UUID;

public record ToolboxProxyDisposeAllPacket(UUID uuid) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ToolboxUtils.ID, "toolbox_proxy_dispose_all");
    public static final CustomPacketPayload.Type<ToolboxProxyDisposeAllPacket> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolboxProxyDisposeAllPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            ToolboxProxyDisposeAllPacket::uuid,
            ToolboxProxyDisposeAllPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        Level world = player.level();
        ToolboxProxyController proxy = ToolboxProxyHandler.getProxy(world, uuid);
        BlockPos toolboxPos = proxy.getBlockPos();
        ToolboxBlockEntity blockEntity = proxy.getToolbox();

        double maxRange = ToolboxHandler.getMaxRange(player);
        if (player.distanceToSqr(toolboxPos.getX() + 0.5, toolboxPos.getY(), toolboxPos.getZ() + 0.5) > maxRange
                * maxRange)
            return;
        if (!(blockEntity instanceof IExtendedToolbox toolbox))
            return;

        CompoundTag compound = player.getPersistentData()
                .getCompound(ToolboxProxyHandler.PERSISTENT_KEY);
        MutableBoolean sendData = new MutableBoolean(false);

        ToolboxInventory toolboxInventory = toolbox.create_toolbox_utils$getInventory();
        toolboxInventory.inLimitedMode(inventory -> {
            for (int i = 0; i < 36; i++) {
                String key = String.valueOf(i);
                if (compound.contains(key) && NbtUtils.loadUUID(compound.getCompound(key).get("UUID"))
                        .equals(uuid)) {
                    ToolboxProxyHandler.unequip(player, i, true);
                    sendData.setTrue();
                }

                ItemStack itemStack = player.getInventory().getItem(i);
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(inventory, itemStack, false);
                if (remainder.getCount() != itemStack.getCount())
                    player.getInventory().setItem(i, remainder);
            }
        });

        if (sendData.booleanValue())
            ToolboxHandler.syncData(player);
    }
}
