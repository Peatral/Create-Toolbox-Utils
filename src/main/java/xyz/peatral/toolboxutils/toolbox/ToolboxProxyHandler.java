package xyz.peatral.toolboxutils.toolbox;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import net.createmod.catnip.data.WorldAttached;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import xyz.peatral.toolboxutils.ToolboxDataComponents;
import xyz.peatral.toolboxutils.network.RemoveToolboxProxyPacket;

import java.util.List;
import java.util.UUID;
import java.util.WeakHashMap;

public class ToolboxProxyHandler {
    private static final WorldAttached<WeakHashMap<UUID, ToolboxBlockEntity>> ACTIVE_PROXIES = new WorldAttached<>(w -> new WeakHashMap<>());

    public static ToolboxBlockEntity getProxy(Level level, UUID uuid) {
        return uuid == null ? null : ACTIVE_PROXIES.get(level).get(uuid);
    }

    public static boolean hasProxy(Level level, UUID uuid) {
        return uuid != null && ACTIVE_PROXIES.get(level).containsKey(uuid);
    }

    /**
     * Creates a toolbox proxy and returns the created proxy BE. The BE is not actually in the world but needs to be manually removed from the proxy handler.
     * @param stack the toolbox item stack
     * @param level the dimension the toolbox is located in
     * @param pos the position the toolbox at
     * @return the proxy that was created
     */
    public static ToolboxBlockEntity createProxy(ItemStack stack, Level level, BlockPos pos) {
        ToolboxBlockEntity be = new ToolboxBlockEntity(AllBlockEntityTypes.TOOLBOX.get(), pos,
                ((BlockItem) stack.getItem()).getBlock().defaultBlockState());

        if (!(be instanceof IExtendedToolbox extendedToolbox)) {
            return null;
        }

        extendedToolbox.create_toolbox_utils$setSource(stack);
        be.setLevel(level);

        be.setUniqueId(stack.getOrDefault(AllDataComponents.TOOLBOX_UUID, UUID.randomUUID()));
        be.readInventory(stack.getOrDefault(AllDataComponents.TOOLBOX_INVENTORY, ItemContainerContents.EMPTY));

        if (stack.has(DataComponents.CUSTOM_NAME)) {
            be.setCustomName(stack.getHoverName());
        }

        extendedToolbox.create_toolbox_utils$setProxy(true);

        ItemContainerContents loadedFilters = stack.get(ToolboxDataComponents.TOOLBOX_FILTERS);
        if (loadedFilters != null && extendedToolbox.create_toolbox_utils$getInventory() instanceof IFilterable filterable) {
            List<ItemStack> filters = filterable.create_toolbox_utils$getFilters();
            for (int i = 0; i < filters.size(); i++) {
                if (i < loadedFilters.getSlots()) {
                    filters.set(i, loadedFilters.getStackInSlot(i));
                }
            }
        }
        be.initialize();
        ACTIVE_PROXIES.get(level).put(be.getUniqueId(), be);
        return be;
    }

    /**
     * Removes a proxy from the level. Does not unequip the proxy
     * @param level the dimension where the proxy is located
     * @param uuid the UUID of the proxy
     */
    public static void removeProxy(Level level, UUID uuid) {
        if (uuid == null) return;
        ToolboxBlockEntity be = ACTIVE_PROXIES.get(level).remove(uuid);
        if (be != null) {
            be.invalidate();
        }
    }

    /**
     * Unequips a proxy from players
     * @param level the dimension where the proxy is located
     * @param uuid the UUID of the proxy
     */
    public static void unequipProxy(Level level, UUID uuid) {
        if (uuid == null) return;

        if (level instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.players()) {
                CompoundTag compound = player.getPersistentData().getCompound("CreateToolboxProxyData");
                boolean sendData = false;
                for (int i = 0; i < 9; i++) {
                    String key = String.valueOf(i);
                    if (compound.contains(key) && NbtUtils.loadUUID(NBTHelper.getINBT(compound.getCompound(key), "UUID")).equals(uuid)) {
                        ToolboxHandler.unequip(player, i, false);
                        sendData = true;
                    }
                }
                if (sendData) {
                    ToolboxHandler.syncData(player);
                }
            }
        }
    }

    /**
     * Ticks the proxy BE for the toolbox item at the position of the entity
     * @param entity the entity the proxy is located at
     * @param itemStack the toolbox item stack
     */
    public static void tickToolbox(Entity entity, ItemStack itemStack) {
        UUID uuid = itemStack.get(AllDataComponents.TOOLBOX_UUID);
        if (uuid == null) return;

        Level level = entity.level();
        BlockPos currentPos = entity.blockPosition();

        ToolboxBlockEntity proxy = ToolboxProxyHandler.getProxy(level, uuid);

        if (proxy != null) {
            if (!proxy.getBlockPos().equals(currentPos) || proxy.getLevel() != level) {
                ToolboxProxyHandler.removeProxy(proxy.getLevel(), uuid);
                proxy = null;
            }
        }

        if (proxy == null) {
            proxy = ToolboxProxyHandler.createProxy(itemStack, level, currentPos);
        }

        proxy.tick();
    }

    /**
     * Removes the proxy and sync it to clients. Optionally also unequips the proxy from the players in the dimension
     * @param level the dimension where the proxy is located
     * @param uuid the UUID of the proxy
     * @param unequip whether the proxy should be unequipped from the players
     */
    public static void removeProxySynced(Level level, UUID uuid, boolean unequip) {
        if (uuid != null) {
            if (!unequip) {
                ToolboxProxyHandler.unequipProxy(level, uuid);
            }
            ToolboxProxyHandler.removeProxy(level, uuid);
            if (level instanceof ServerLevel serverLevel) {
                for (ServerLevel serverLevelIt : serverLevel.getServer().getAllLevels()) {
                    PacketDistributor.sendToPlayersInDimension(serverLevelIt, new RemoveToolboxProxyPacket(uuid, unequip && serverLevelIt == serverLevel));
                }
            }
        }
    }

    /**
     * Changes the dimension a proxy is located in
     * @param from the dimension the proxy comes from
     * @param to the dimension the proxy goes to
     * @param itemStack the toolbox itemstack
     * @param pos the new position the toolbox is located at
     * @param unequip whether the proxy should be unequipped from the players
     */
    public static void changeProxyDimension(Level from, Level to, ItemStack itemStack, BlockPos pos, boolean unequip) {
        UUID uuid = itemStack.get(AllDataComponents.TOOLBOX_UUID);

        if (!hasProxy(from, uuid)) {
            return;
        }

        ToolboxProxyHandler.removeProxySynced(from, uuid, unequip);
        ToolboxProxyHandler.createProxy(itemStack, to, pos);
    }
}