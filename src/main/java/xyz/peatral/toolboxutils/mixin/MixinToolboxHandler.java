package xyz.peatral.toolboxutils.mixin;

import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.peatral.toolboxutils.IEnchantableToolbox;
import xyz.peatral.toolboxutils.proxy.ToolboxProxy;
import xyz.peatral.toolboxutils.proxy.ToolboxProxyHandler;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.simibubi.create.content.equipment.toolbox.ToolboxHandler.syncData;

@Mixin(ToolboxHandler.class)
public class MixinToolboxHandler {

    @Unique
    private static final int create_toolbox_utils$validationTimer = 20;

    @Inject(method = "getNearest", at = @At("HEAD"), remap = false, cancellable = true)
    private static void getNearest(LevelAccessor world, Player player, int maxAmount, CallbackInfoReturnable<List<ToolboxBlockEntity>> cir) {
        Vec3 location = player.position();
        double maxRange = ToolboxHandler.getMaxRange(player);
        List<ToolboxBlockEntity> returnValue = ToolboxHandler.toolboxes.get(world)
                .keySet()
                .stream()
                .filter(p -> ToolboxHandler.distance(location, p) < maxRange * maxRange)
                .sorted(Comparator.comparingDouble(p -> ToolboxHandler.distance(location, p)))
                .limit(maxAmount)
                .map(ToolboxHandler.toolboxes.get(world)::get)
                .filter(ToolboxBlockEntity::isFullyInitialized)
                .filter(toolboxBlockEntity -> {
                    if (toolboxBlockEntity instanceof IEnchantableToolbox tb) {
                        return tb.create_toolbox_utils$getLoyaltyLevel(world.registryAccess()) < 1
                                || tb.create_toolbox_utils$isOwner(player);
                    }
                    return true;
                })
                .collect(Collectors.toList());
        cir.setReturnValue(returnValue);
    }

    @Inject(method = "withinRange", at = @At("RETURN"), remap = false, cancellable = true)
    private static void withinRange(Player player, ToolboxBlockEntity box, CallbackInfoReturnable<Boolean> cir) {
        if (!(box instanceof IEnchantableToolbox tb) || tb.create_toolbox_utils$getLoyaltyLevel(player.level().registryAccess()) < 3 || !tb.create_toolbox_utils$isOwner(player)) {
            return;
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "entityTick", at = @At(value = "HEAD"))
    private static void entityTick(Entity entity, Level world, CallbackInfo ci) {
        if (world.isClientSide)
            return;
        if (!(world instanceof ServerLevel))
            return;
        if (!(entity instanceof ServerPlayer player))
            return;
        if (entity.tickCount % create_toolbox_utils$validationTimer != 0)
            return;

        if (!player.getPersistentData()
                .contains("CreateToolboxProxyData"))
            return;

        boolean sendData = false;
        CompoundTag compound = player.getPersistentData()
                .getCompound("CreateToolboxProxyData");
        for (int i = 0; i < 9; i++) {
            String key = String.valueOf(i);
            if (!compound.contains(key))
                continue;

            CompoundTag data = compound.getCompound(key);
            Tag uuidTag = data.get("UUID");
            UUID uuid = NbtUtils.loadUUID(uuidTag);
            int slot = data.getInt("Slot");

            // TODO: Not sure how to handle unloaded entities
            if (!ToolboxProxyHandler.hasProxy(world, uuid)) {
                compound.remove(key);
                sendData = true;
                continue;
            }

            ToolboxProxyHandler.getProxy(world, uuid).connectPlayer(slot, player, i);
        }

        // TODO: Duplicate sync?????
        if (sendData)
            syncData(player);
    }

    @Inject(method = "playerLogin", at = @At(value = "HEAD"))
    private static void playerLogin(Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer))
            return;
        if (player.getPersistentData()
                .contains("CreateToolboxProxyData")
                && !player.getPersistentData()
                .getCompound("CreateToolboxProxyData")
                .isEmpty()) {
            // TODO: Duplicate sync?????
            syncData(player);
        }
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
        Tag uuidTag = prevData.get("UUID");
        UUID prevUUID = NbtUtils.loadUUID(uuidTag);
        int prevSlot = prevData.getInt("Slot");

        ToolboxProxy prevProxy = ToolboxProxyHandler.getProxy(world, prevUUID);
        if (prevProxy != null) {
            prevProxy.unequip(prevSlot, player, hotbarSlot, keepItems || !ToolboxHandler.withinRange(player, prevProxy));
        }
        compound.remove(key);
    }
}
