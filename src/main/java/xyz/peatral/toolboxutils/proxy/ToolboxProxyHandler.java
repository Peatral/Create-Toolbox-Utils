package xyz.peatral.toolboxutils.proxy;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import net.createmod.catnip.data.WorldAttached;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import xyz.peatral.toolboxutils.network.RemoveToolboxProxyPacket;

import java.util.UUID;
import java.util.WeakHashMap;

public class ToolboxProxyHandler {
    private static final WorldAttached<WeakHashMap<UUID, ToolboxProxy>> ACTIVE_PROXIES = new WorldAttached<>(w -> new WeakHashMap<>());

    public static ToolboxProxy getProxy(Level level, UUID uuid) {
        return uuid == null ? null : ACTIVE_PROXIES.get(level).get(uuid);
    }

    public static boolean hasProxy(Level level, UUID uuid) {
        return uuid != null && ACTIVE_PROXIES.get(level).containsKey(uuid);
    }

    public static ToolboxProxy createProxy(ItemStack stack, Level level, BlockPos pos) {
        ToolboxProxy be = new ToolboxProxy(stack, level, pos);
        be.initialize();
        ACTIVE_PROXIES.get(level).put(be.getUniqueId(), be);
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

        ToolboxProxy proxy = ToolboxProxyHandler.getProxy(level, uuid);

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