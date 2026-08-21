package xyz.peatral.toolboxutils.toolbox.proxy;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import net.createmod.catnip.data.WorldAttached;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import xyz.peatral.toolboxutils.network.RemoveToolboxProxyPacket;

import java.util.*;

/**
 * Mirrors {@link com.simibubi.create.content.equipment.toolbox.ToolboxHandler} but for {@link ToolboxProxyController}
 */
public class ToolboxProxyHandler {
    public static final String PERSISTENT_KEY = "CreateToolboxProxyData";

    public static final WorldAttached<WeakHashMap<UUID, ToolboxProxyController>> proxies = new WorldAttached<>(w -> new WeakHashMap<>());
    static int validationTimer = 20;

    public static ToolboxProxyController getProxy(Level level, UUID uuid) {
        return uuid == null ? null : proxies.get(level).get(uuid);
    }

    public static boolean hasProxy(Level level, UUID uuid) {
        return uuid != null && proxies.get(level).containsKey(uuid);
    }

    public static void onLoad(ToolboxProxyController controller) {
        proxies.get(controller.getLevel()).put(controller.getUniqueId(), controller);
    }

    public static void onUnload(ToolboxProxyController controller) {
        proxies.get(controller.getLevel()).remove(controller.getUniqueId());
    }

    public static void entityTick(Entity entity, Level world) {
        if (!world.isClientSide) {
            if (world instanceof ServerLevel) {
                if (entity instanceof ServerPlayer) {
                    ServerPlayer player = (ServerPlayer)entity;
                    if (entity.tickCount % validationTimer == 0) {
                        if (player.getPersistentData().contains(PERSISTENT_KEY)) {
                            boolean sendData = false;
                            CompoundTag compound = player.getPersistentData().getCompound(PERSISTENT_KEY);

                            for(int i = 0; i < 9; ++i) {
                                String key = String.valueOf(i);
                                if (compound.contains(key)) {
                                    CompoundTag data = compound.getCompound(key);
                                    UUID uuid = NbtUtils.loadUUID(NBTHelper.getINBT(data, "UUID"));
                                    int slot = data.getInt("Slot");
                                    if (hasProxy(world, uuid)) {
                                        ToolboxProxyController proxy = getProxy(world, uuid);
                                        ToolboxBlockEntity toolboxBlockEntity = proxy.getToolbox();
                                        toolboxBlockEntity.connectPlayer(slot, player, i);
                                    } else {
                                        compound.remove(key);
                                        sendData = true;
                                    }
                                }
                            }

                            if (sendData) {
                                ToolboxHandler.syncData(player);
                            }

                        }
                    }
                }
            }
        }
    }

    public static void playerLogin(Player player) {
        if (player instanceof ServerPlayer) {
            if (player.getPersistentData().contains(PERSISTENT_KEY) && !player.getPersistentData().getCompound(PERSISTENT_KEY).isEmpty()) {
                ToolboxHandler.syncData(player);
            }
        }


        //if (player instanceof ServerPlayer serverPlayer) {
        //    ToolboxProxyHandler.syncAllProxiesToPlayer(serverPlayer.level(), serverPlayer);
        //}
    }

    public static void unequip(Player player, int hotbarSlot, boolean keepItems) {
        CompoundTag compound = player.getPersistentData().getCompound(PERSISTENT_KEY);
        Level world = player.level();
        String key = String.valueOf(hotbarSlot);
        if (compound.contains(key)) {
            CompoundTag prevData = compound.getCompound(key);
            UUID prevUUID = NbtUtils.loadUUID(NBTHelper.getINBT(prevData, "UUID"));
            int prevSlot = prevData.getInt("Slot");
            ToolboxProxyController proxy = getProxy(world, prevUUID);
            ToolboxBlockEntity toolbox = proxy.getToolbox();
            toolbox.unequip(prevSlot, player, hotbarSlot, keepItems || !ToolboxHandler.withinRange(player, toolbox));

            compound.remove(key);
        }
    }

    // ----- Custom Stuff -----
    public static void syncProxy(Level level, UUID uuid) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (uuid == null) return;

        ToolboxProxyController proxy = getProxy(level, uuid);
        proxy.getSyncPacket().ifPresent(packet -> PacketDistributor.sendToPlayersInDimension(serverLevel, packet));
    }

    public static void handleSync(Level level, UUID uuid, CompoundTag data) {
        if (!hasProxy(level, uuid)) {
            return;
        }
        ToolboxProxyController proxy = getProxy(level, uuid);
        proxy.handleSync(data);
    }

    /**
     * Removes the proxy and syncs it to clients
     * @param level the dimension where the proxy is located
     * @param uuid the UUID of the proxy
     */
    public static void removeProxy(Level level, UUID uuid) {
        if (uuid == null) return;
        if (!hasProxy(level, uuid)) return;

        ToolboxProxyController proxy = getProxy(level, uuid);

        proxy.invalidate();
        proxies.get(level).remove(uuid);

        if (level instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersInDimension(serverLevel, new RemoveToolboxProxyPacket(uuid));
        }
    }

    /**
     * Changes the dimension a proxy is located in
     * @param from the dimension the proxy comes from
     * @param to the dimension the proxy goes to
     * @param itemStack the toolbox itemstack
     * @param pos the new position the toolbox is located at
     */
    public static void changeProxyDimension(Level from, Level to, ItemStack itemStack, BlockPos pos) {
        UUID uuid = itemStack.get(AllDataComponents.TOOLBOX_UUID);
        ToolboxProxyController proxy = proxies.get(from).remove(uuid);
        if (proxy == null) {
            ToolboxProxyController.create(itemStack, to, pos).ifPresent(ToolboxProxyController::initialize);
        } else {
            proxy.setLevel(to);
            proxy.setBlockPos(pos);
            proxy.setItemStack(itemStack);
            proxies.get(to).put(uuid, proxy);
        }
    }

    /**
     * Ticks the proxy BE for the toolbox item at a position in a level
     * @param level the level the toolbox is located in
     * @param stack the toolbox item stack
     * @param blockPos the location the toolbox is at
     */
    public static void tickProxy(Level level, ItemStack stack, BlockPos blockPos) {
        UUID toolboxUuid = stack.get(AllDataComponents.TOOLBOX_UUID);
        ToolboxProxyController proxy = ToolboxProxyHandler.getProxy(level, toolboxUuid);
        if (proxy == null) {
            ToolboxProxyController.create(stack, level, blockPos).ifPresent(p -> {
                p.initialize();
                p.tick();
            });
        } else {
            proxy.setBlockPos(blockPos);
            proxy.tick();
        }
    }
}
