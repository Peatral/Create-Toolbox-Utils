package xyz.peatral.toolboxutils.network;

import com.simibubi.create.content.equipment.toolbox.ItemReturnInvWrapper;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import com.simibubi.create.content.equipment.toolbox.ToolboxInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xyz.peatral.toolboxutils.IEnchantableToolbox;
import xyz.peatral.toolboxutils.IFilterable;
import xyz.peatral.toolboxutils.ToolboxUtils;
import xyz.peatral.toolboxutils.proxy.ToolboxProxy;
import xyz.peatral.toolboxutils.proxy.ToolboxProxyHandler;

import java.util.UUID;

public record ToolboxProxyEquipPacket(UUID uuid, int slot, int hotbarSlot) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ToolboxUtils.ID, "toolbox_proxy_equip");
    public static final CustomPacketPayload.Type<ToolboxProxyEquipPacket> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolboxProxyEquipPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            ToolboxProxyEquipPacket::uuid,
            ByteBufCodecs.VAR_INT,
            ToolboxProxyEquipPacket::slot,
            ByteBufCodecs.VAR_INT,
            ToolboxProxyEquipPacket::hotbarSlot,
            ToolboxProxyEquipPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (uuid == null) {
            ToolboxHandler.unequip(player, hotbarSlot, false);
            ToolboxHandler.syncData(player);
            return;
        }

        ToolboxProxy blockEntity = ToolboxProxyHandler.getProxy(player.level(), uuid);
        BlockPos toolboxPos = blockEntity.getBlockPos();

        double maxRange = ToolboxHandler.getMaxRange(player);
        if (player.distanceToSqr(toolboxPos.getX() + 0.5, toolboxPos.getY(), toolboxPos.getZ() + 0.5) > maxRange
                * maxRange)
            return;
        if (!(blockEntity instanceof IEnchantableToolbox toolboxBlockEntity))
            return;

        ToolboxHandler.unequip(player, hotbarSlot, false);

        if (slot < 0 || slot >= 8) {
            ToolboxHandler.syncData(player);
            return;
        }

        ToolboxInventory toolboxInventory = toolboxBlockEntity.create_toolbox_utils$getInventory();
        ItemStack playerStack = player.getInventory().getItem(hotbarSlot);
        if (!playerStack.isEmpty() && toolboxInventory instanceof IFilterable filterable && !ToolboxInventory.canItemsShareCompartment(playerStack,
                filterable.create_toolbox_utils$getFilters().get(slot))) {
            toolboxInventory.inLimitedMode(inventory -> {
                ItemStack remainder = ItemHandlerHelper.insertItemStacked(inventory, playerStack, false);
                if (!remainder.isEmpty())
                    remainder = ItemHandlerHelper.insertItemStacked(new ItemReturnInvWrapper(player.getInventory()),
                            remainder, false);
                if (remainder.getCount() != playerStack.getCount())
                    player.getInventory().setItem(hotbarSlot, remainder);
            });
        }

        CompoundTag compound = player.getPersistentData()
                .getCompound("CreateToolboxProxyData");
        String key = String.valueOf(hotbarSlot);

        CompoundTag data = new CompoundTag();
        data.putInt("Slot", slot);
        data.put("UUID", NbtUtils.createUUID(uuid));
        compound.put(key, data);

        player.getPersistentData()
                .put("CreateToolboxProxyData", compound);

        blockEntity.connectPlayer(slot, player, hotbarSlot);
        ToolboxHandler.syncData(player);
    }
}
