package xyz.peatral.toolboxutils;

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

import java.util.UUID;
import java.util.WeakHashMap;

public class ToolboxProxyHandler {
    private static final WorldAttached<WeakHashMap<UUID, ToolboxBlockEntity>> ACTIVE_PROXIES = new WorldAttached<>(w -> new WeakHashMap<>());

    public static ToolboxBlockEntity getProxy(Level level, UUID uuid) {
        return uuid == null ? null : ACTIVE_PROXIES.get(level).get(uuid);
    }

    public static ToolboxBlockEntity createProxy(ItemStack stack, Level level, BlockPos pos, UUID uuid) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return null;

        ToolboxBlockEntity be = new ToolboxBlockEntity(
                AllBlockEntityTypes.TOOLBOX.get(),
                pos,
                blockItem.getBlock().defaultBlockState()
        );

        be.setLevel(level);
        be.readInventory(stack.getOrDefault(AllDataComponents.TOOLBOX_INVENTORY, ItemContainerContents.EMPTY));
        be.setUniqueId(uuid);

        if (stack.has(DataComponents.CUSTOM_NAME))
            be.setCustomName(stack.getHoverName());

        be.initialize();
        ACTIVE_PROXIES.get(level).put(uuid, be);
        return be;
    }

    public static void removeProxy(Level level, UUID uuid) {
        if (uuid == null) return;
        ToolboxBlockEntity be = ACTIVE_PROXIES.get(level).remove(uuid);
        if (be != null) {
            be.invalidate();
            be.clearRemoved();
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
            proxy = ToolboxProxyHandler.createProxy(itemStack, level, currentPos, uuid);
        }

        if (proxy != null) {
            proxy.tick();
        }
    }

    public static void removeToolbox(Entity entity, ItemStack itemStack) {
        UUID uuid = itemStack.get(AllDataComponents.TOOLBOX_UUID);
        Level level = entity.level();
        if (uuid != null) {
            ToolboxProxyHandler.removeProxy(level, uuid);
            if (level instanceof ServerLevel serverLevel) {
                PacketDistributor.sendToPlayersInDimension(serverLevel, new RemoveToolboxProxyPayload(uuid));
            }
        }
    }
}