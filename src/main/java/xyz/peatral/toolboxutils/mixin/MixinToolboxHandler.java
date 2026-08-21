package xyz.peatral.toolboxutils.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.peatral.toolboxutils.toolbox.IExtendedToolbox;
import xyz.peatral.toolboxutils.toolbox.proxy.ToolboxProxyController;
import xyz.peatral.toolboxutils.toolbox.proxy.ToolboxProxyHandler;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.simibubi.create.content.equipment.toolbox.ToolboxHandler.syncData;
import static com.simibubi.create.content.equipment.toolbox.ToolboxHandler.toolboxes;
import static xyz.peatral.toolboxutils.toolbox.proxy.ToolboxProxyHandler.proxies;

@Mixin(ToolboxHandler.class)
public class MixinToolboxHandler {
    @WrapMethod(method = "getNearest")
    private static List<ToolboxBlockEntity> getNearest(LevelAccessor world, Player player, int maxAmount, Operation<List<ToolboxBlockEntity>> original) {
        // TODO: Yeah i dont call the original at all lmao
        Vec3 location = player.position();
        double maxRange = ToolboxHandler.getMaxRange(player);
        return Stream
                .concat(
                        toolboxes.get(world).values().stream(),
                        proxies.get(world).values().stream().map(ToolboxProxyController::getToolbox)
                )
                .filter((p) -> {
                    if (p instanceof IExtendedToolbox tb) {
                        return tb.create_toolbox_utils$getLoyaltyLevel(world.registryAccess()) < 1
                                || tb.create_toolbox_utils$isOwner(player);
                    }
                    return true;
                })
                .filter((p) -> ToolboxHandler.distance(location, p.getBlockPos()) < maxRange * maxRange)
                .limit(maxAmount)
                .filter(ToolboxBlockEntity::isFullyInitialized)
                .collect(Collectors.toList());
    }

    @WrapMethod(method = "withinRange", remap = false)
    private static boolean withinRange(Player player, ToolboxBlockEntity box, Operation<Boolean> original) {
        boolean isOwnedByPlayer = box instanceof IExtendedToolbox tb
                && tb.create_toolbox_utils$getLoyaltyLevel(player.level().registryAccess()) >= 3
                && tb.create_toolbox_utils$isOwner(player);
        return isOwnedByPlayer || original.call(player, box);
    }

    @WrapOperation(
            method = "entityTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/CompoundTag;contains(Ljava/lang/String;)Z",
                    ordinal = 0
            )
    )
    private static boolean wrapEntityTickValidation(
            CompoundTag persistentData,
            String persistentDataKey,
            Operation<Boolean> original,
            @Local(name = "player") ServerPlayer player,
            @Local(argsOnly = true) Level world
    ) {
        boolean proxyChangedData = false;
        if (persistentData.contains(ToolboxProxyHandler.PERSISTENT_KEY)) {
            CompoundTag compound = persistentData.getCompound(ToolboxProxyHandler.PERSISTENT_KEY);
            for (int i = 0; i < 9; i++) {
                String key = String.valueOf(i);
                if (!compound.contains(key))
                    continue;

                CompoundTag data = compound.getCompound(key);
                UUID uuid = NbtUtils.loadUUID(NBTHelper.getINBT(data, "UUID"));
                int slot = data.getInt("Slot");

                // TODO: Not sure how to handle unloaded entities
                if (!ToolboxProxyHandler.hasProxy(world, uuid)) {
                    compound.remove(key);
                    proxyChangedData = true;
                    continue;
                }

                ToolboxProxyController proxy = ToolboxProxyHandler.getProxy(world, uuid);
                if (proxy != null) {
                    ToolboxBlockEntity be = proxy.getToolbox();
                    be.connectPlayer(slot, player, i);
                }
            }
        }

        boolean originalHasData = original.call(persistentData, persistentDataKey);

        if (proxyChangedData && !originalHasData) {
            syncData(player);
        }

        return originalHasData;
    }

    @WrapMethod(method = "playerLogin")
    private static void wrapPlayerLoginSyncCheck(Player player, Operation<Void> original) {
        if (!(player instanceof ServerPlayer)) {
            original.call(player);
            return;
        }

        CompoundTag persistentData = player.getPersistentData();

        // simply wrapping the call will not work as it will be synced twice
        // so i need to evaluate whether the original function will sync
        // not very clean but at least it dodges all edge cases
        boolean originalWillSync = persistentData.contains("CreateToolboxData")
                && !persistentData.getCompound("CreateToolboxData").isEmpty();

        original.call(player);

        // This way, we can run the sync when we know the original wont run it
        if (!originalWillSync) {
            boolean hasValidProxyData = persistentData.contains(ToolboxProxyHandler.PERSISTENT_KEY)
                    && !persistentData.getCompound(ToolboxProxyHandler.PERSISTENT_KEY).isEmpty();

            if (hasValidProxyData) {
                syncData(player);
            }
        }
    }

    @Inject(method = "unequip", at = @At(value = "HEAD"))
    private static void unequip(Player player, int hotbarSlot, boolean keepItems, CallbackInfo ci) {
        CompoundTag compound = player.getPersistentData()
                .getCompound(ToolboxProxyHandler.PERSISTENT_KEY);
        Level world = player.level();
        String key = String.valueOf(hotbarSlot);
        if (!compound.contains(key))
            return;

        CompoundTag prevData = compound.getCompound(key);
        UUID prevUUID = NbtUtils.loadUUID(NBTHelper.getINBT(prevData, "UUID"));
        int prevSlot = prevData.getInt("Slot");

        ToolboxProxyController prevProxy = ToolboxProxyHandler.getProxy(world, prevUUID);
        if (prevProxy != null) {
            ToolboxBlockEntity prevBlockEntity = prevProxy.getToolbox();
            prevBlockEntity.unequip(prevSlot, player, hotbarSlot, keepItems || !ToolboxHandler.withinRange(player, prevBlockEntity));
        }
        compound.remove(key);
    }
}
