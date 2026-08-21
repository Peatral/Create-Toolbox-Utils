package xyz.peatral.toolboxutils.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Dynamic;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.peatral.toolboxutils.ToolboxActivities;
import xyz.peatral.toolboxutils.ToolboxRegionTickets;
import xyz.peatral.toolboxutils.toolbox.concierge.AttributeModifiers;
import xyz.peatral.toolboxutils.toolbox.concierge.ConciergeAi;
import xyz.peatral.toolboxutils.toolbox.concierge.IConcierge;
import xyz.peatral.toolboxutils.toolbox.proxy.ToolboxProxyController;
import xyz.peatral.toolboxutils.toolbox.proxy.ToolboxProxyHandler;

import java.util.Optional;
import java.util.UUID;

@Mixin(Allay.class)
public abstract class MixinAllay extends PathfinderMob implements InventoryCarrier, VibrationSystem, IConcierge {
    protected MixinAllay(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
    }


    @Inject(method = "tick", at = @At("RETURN"))
    private void tick(CallbackInfo ci) {
        ItemStack stack = getItemInHand(InteractionHand.MAIN_HAND);

        if (AllTags.AllItemTags.TOOLBOXES.matches(stack)) {
            ToolboxProxyHandler.tickProxy(level(), stack, blockPosition());

            if (level() instanceof ServerLevel from && create_toolbox_utils$getOwner().isEmpty()) {
                MinecraftServer server = from.getServer();
                for (ServerLevel to : server.getAllLevels()) {
                    Optional<Player> optionalPlayer = brain.getMemory(MemoryModuleType.LIKED_PLAYER)
                            .map(to::getPlayerByUUID);
                    if (optionalPlayer.isPresent() && to != from) {
                        ChunkPos previousPos = chunkPosition();
                        UUID ticketUuid = getUUID();
                        changeDimension(new DimensionTransition(to, this, entity -> {
                            from.getChunkSource().removeRegionTicket(
                                    ToolboxRegionTickets.TRAVELLING_CONCIERGE,
                                    previousPos,
                                    2,
                                    ticketUuid
                            );
                            if (entity instanceof IConcierge concierge) {
                                concierge.create_toolbox_utils$tryToTeleportToOwner();
                            }
                            ToolboxProxyHandler.changeProxyDimension(from, to, stack, entity.blockPosition());
                        }));
                        break;
                    }
                }
            }

            if (!this.create_toolbox_utils$unableToMoveToOwner() && this.create_toolbox_utils$shouldTryTeleportToOwner()) {
                    this.create_toolbox_utils$tryToTeleportToOwner();
            }
        }
    }

    @WrapOperation(
            method = "customServerAiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/allay/AllayAi;updateActivity(Lnet/minecraft/world/entity/animal/allay/Allay;)V"
            )
    )
    public void customServerAiStep(Allay allay, Operation<Void> original) {
        ItemStack stack = getItemInHand(InteractionHand.MAIN_HAND);
        if (AllTags.AllItemTags.TOOLBOXES.matches(stack)) {
            brain.setActiveActivityIfPossible(ToolboxActivities.HELPING_PLAYER.get());
        } else {
            original.call(allay);
        }
    }

    @WrapMethod(method = "makeBrain")
    public Brain<?> makeBrain(Dynamic<?> dynamic, Operation<Brain<?>> original) {
        return ConciergeAi.makeBrain((Brain<Allay>) original.call(dynamic));
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            if (AllTags.AllItemTags.TOOLBOXES.matches(stack)) {
                getPersistentData().putBoolean("ConciergeHat", true);
                getAttributes().addTransientAttributeModifiers(AttributeModifiers.conciergeModifier);
            } else if (getPersistentData().contains("ConciergeHat")) {
                getPersistentData().remove("ConciergeHat");
                getAttributes().removeAttributeModifiers(AttributeModifiers.conciergeModifier);
            }
        }
        ItemStack handStack = getItemBySlot(EquipmentSlot.MAINHAND);
        if (slot == EquipmentSlot.MAINHAND && AllTags.AllItemTags.TOOLBOXES.matches(handStack)) {
            UUID oldUuid = handStack.get(AllDataComponents.TOOLBOX_UUID);
            UUID newUuid = stack.get(AllDataComponents.TOOLBOX_UUID);
            if (oldUuid == null || !oldUuid.equals(newUuid)) {
                ToolboxProxyController proxy = ToolboxProxyHandler.getProxy(level(), oldUuid);
                if (proxy != null) {
                    proxy.setRemoved();
                }
            }
        }
        super.setItemSlot(slot, stack);
    }

    @Override
    protected void handlePortal() {
        if (!brain.isActive(ToolboxActivities.HELPING_PLAYER.get())) {
            super.handlePortal();
        }
    }

    @Override
    public void create_toolbox_utils$tryToTeleportToOwner() {
        create_toolbox_utils$getOwner()
                .ifPresent(player -> this.create_toolbox_utils$teleportToAroundBlockPos(player.blockPosition()));
    }

    @Unique
    private Optional<Player> create_toolbox_utils$getOwner() {
        return this.brain.getMemory(MemoryModuleType.LIKED_PLAYER)
                .flatMap(uuid -> Optional.ofNullable(level().getPlayerByUUID(uuid)));
    }

    @Unique
    public boolean create_toolbox_utils$shouldTryTeleportToOwner() {
        return create_toolbox_utils$getOwner()
                .map(player -> this.distanceToSqr(player) >= 32 * 32)
                .orElse(false);
    }

    @Unique
    private void create_toolbox_utils$teleportToAroundBlockPos(BlockPos pos) {
        for (int i = 0; i < 10; i++) {
            int j = this.random.nextIntBetweenInclusive(-3, 3);
            int k = this.random.nextIntBetweenInclusive(-3, 3);
            if (Math.abs(j) >= 2 || Math.abs(k) >= 2) {
                int l = this.random.nextIntBetweenInclusive(-1, 1);
                if (this.create_toolbox_utils$maybeTeleportTo(pos.getX() + j, pos.getY() + l, pos.getZ() + k)) {
                    return;
                }
            }
        }
    }

    @Unique
    private boolean create_toolbox_utils$maybeTeleportTo(int x, int y, int z) {
        if (!this.create_toolbox_utils$canTeleportTo(new BlockPos(x, y, z))) {
            return false;
        } else {
            this.moveTo((double)x + 0.5, (double)y, (double)z + 0.5, this.getYRot(), this.getXRot());
            this.navigation.stop();
            return true;
        }
    }

    @Unique
    private boolean create_toolbox_utils$canTeleportTo(BlockPos pos) {
        BlockState blockstate = this.level().getBlockState(pos.below());
        if (blockstate.getBlock() instanceof LeavesBlock) {
            return false;
        } else {
            BlockPos blockpos = pos.subtract(this.blockPosition());
            return this.level().noCollision(this, this.getBoundingBox().move(blockpos));
        }
    }

    @Unique
    public final boolean create_toolbox_utils$unableToMoveToOwner() {
        return this.isPassenger()
                || this.mayBeLeashed()
                || create_toolbox_utils$getOwner()
                        .map(Player::isSpectator).orElse(false);
    }
}
