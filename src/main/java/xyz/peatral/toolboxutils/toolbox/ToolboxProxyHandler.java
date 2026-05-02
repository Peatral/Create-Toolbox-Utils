package xyz.peatral.toolboxutils.toolbox;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import net.createmod.catnip.data.WorldAttached;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
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

    public static void removeProxy(Level level, UUID uuid) {
        if (uuid == null) return;
        ToolboxBlockEntity be = ACTIVE_PROXIES.get(level).remove(uuid);
        if (be != null) {
            be.invalidate();
        }
    }

    public static  void tickToolbox(Entity entity, ItemStack itemStack) {
        UUID uuid = itemStack.get(AllDataComponents.TOOLBOX_UUID);
        if (uuid == null) return;

        Level level = entity.level();
        BlockPos currentPos = entity.blockPosition();

        ToolboxBlockEntity proxy = ToolboxProxyHandler.getProxy(level, uuid);

        if (proxy != null) {
            if (!proxy.getBlockPos().equals(currentPos) || proxy.getLevel() != level) {
                ToolboxProxyHandler.removeProxy(level, uuid);
                proxy = null;
            }
        }

        if (proxy == null) {
            proxy = ToolboxProxyHandler.createProxy(itemStack, level, currentPos);
        }

        proxy.tick();
    }

    public static void removeToolbox(Entity entity, ItemStack itemStack) {
        UUID uuid = itemStack.get(AllDataComponents.TOOLBOX_UUID);
        Level level = entity.level();
        if (uuid != null) {
            ToolboxProxyHandler.removeProxy(level, uuid);
            if (level instanceof ServerLevel serverLevel) {
                PacketDistributor.sendToPlayersInDimension(serverLevel, new RemoveToolboxProxyPacket(uuid));
            }
        }
    }
}