package xyz.peatral.toolboxutils.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.peatral.toolboxutils.toolbox.IExtendedToolbox;
import xyz.peatral.toolboxutils.toolbox.ToolboxProxyHandler;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.simibubi.create.content.equipment.toolbox.ToolboxHandler.syncData;

@Mixin(ToolboxHandler.class)
public class MixinToolboxHandler {

    @WrapOperation(
            method = "getNearest",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;",
                    ordinal = 1
            ),
            remap = false
    )
    private static Stream<ToolboxBlockEntity> getNearest(
            Stream<ToolboxBlockEntity> instance,
            Predicate<ToolboxBlockEntity> predicate,
            Operation<Stream<ToolboxBlockEntity>> original,
            @Local(argsOnly = true) LevelAccessor world,
            @Local(argsOnly = true) Player player
    ) {
        return original.call(instance, predicate.and(toolboxBlockEntity -> {
            if (toolboxBlockEntity instanceof IExtendedToolbox tb) {
                return tb.create_toolbox_utils$getLoyaltyLevel(world.registryAccess()) < 1
                        || tb.create_toolbox_utils$isOwner(player);
            }
            return true;
        }));
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
        if (persistentData.contains("CreateToolboxProxyData")) {
            CompoundTag compound = persistentData.getCompound("CreateToolboxProxyData");
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

                ToolboxBlockEntity proxy = ToolboxProxyHandler.getProxy(world, uuid);
                if (proxy != null) {
                    proxy.connectPlayer(slot, player, i);
                }
            }
        }

        boolean originalHasData = original.call(persistentData, persistentDataKey);

        if (proxyChangedData && !originalHasData) {
            syncData(player);
        }

        return originalHasData;
    }

    @ModifyExpressionValue(
            method = "playerLogin",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/CompoundTag;isEmpty()Z"
            )
    )
    private static boolean wrapPlayerLoginSyncCheck(boolean originalCondition, Player player) {
        if (originalCondition) {
            return true;
        }

        CompoundTag persistentData = player.getPersistentData();
        return persistentData.contains("CreateToolboxProxyData")
                && !persistentData.getCompound("CreateToolboxProxyData").isEmpty();
    }

    @Inject(method = "unequip", at = @At(value = "HEAD"))
    private static void unequip(Player player, int hotbarSlot, boolean keepItems, CallbackInfo ci) {
        CompoundTag compound = player.getPersistentData()
                .getCompound("CreateToolboxProxyData");
        Level world = player.level();
        String key = String.valueOf(hotbarSlot);
        if (!compound.contains(key))
            return;

        CompoundTag prevData = compound.getCompound(key);
        UUID prevUUID = NbtUtils.loadUUID(NBTHelper.getINBT(prevData, "UUID"));
        int prevSlot = prevData.getInt("Slot");

        ToolboxBlockEntity prevProxy = ToolboxProxyHandler.getProxy(world, prevUUID);
        if (prevProxy != null) {
            prevProxy.unequip(prevSlot, player, hotbarSlot, keepItems || !ToolboxHandler.withinRange(player, prevProxy));
        }
        compound.remove(key);
    }
}
