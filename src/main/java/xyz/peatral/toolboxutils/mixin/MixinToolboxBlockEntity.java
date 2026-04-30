package xyz.peatral.toolboxutils.mixin;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlock;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import com.simibubi.create.content.equipment.toolbox.ToolboxInventory;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.peatral.toolboxutils.IEnchantableToolbox;

import javax.annotation.Nullable;
import java.util.Optional;

@Mixin(ToolboxBlockEntity.class)
public abstract class MixinToolboxBlockEntity extends SmartBlockEntity implements IEnchantableToolbox {
    @Shadow
    ToolboxInventory inventory;

    @Unique
    public ItemEnchantments create_toolbox_utils$enchantments = null;

    @Unique
    @Nullable
    private GameProfile create_toolbox_utils$owner;

    public MixinToolboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public ToolboxInventory create_toolbox_utils$getInventory() {
        return inventory;
    }

    @Override
    public ItemEnchantments create_toolbox_utils$getEnchantments() {
        return create_toolbox_utils$enchantments;
    }

    @Override
    public void create_toolbox_utils$setEnchantments(ItemEnchantments enchantments) {
        create_toolbox_utils$enchantments = enchantments;
    }

    @Override
    public void create_toolbox_utils$setOwner(GameProfile gameProfile) {
        synchronized (this) {
            create_toolbox_utils$owner = gameProfile;
        }
        this.setChanged();
    }

    @Override
    public boolean create_toolbox_utils$isOwner(Player player) {
        return create_toolbox_utils$owner == null || player.getUUID().equals(create_toolbox_utils$owner.getId());
    }

    @Inject(method = "lazyTick", at = @At("RETURN"), remap = false)
    public void lazyTick(CallbackInfo ci) {
        if (create_toolbox_utils$owner == null || create_toolbox_utils$owner.getId() == null || level == null || create_toolbox_utils$getLoyaltyLevel(level.registryAccess()) < 2) {
            return;
        }
        Player player = level.getPlayerByUUID(create_toolbox_utils$owner.getId());
        if (player == null) {
            return;
        }
        double maxRange = ToolboxHandler.getMaxRange(player);
        if (getBlockPos().getCenter().distanceTo(player.position()) > maxRange) {
            Block block = getBlockState().getBlock();
            if (block instanceof ToolboxBlock tb) {
                tb.attack(getBlockState(), level, getBlockPos(), player);
            }
        }
    }

    @Inject(method = "read", at = @At("RETURN"), remap = false)
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        Optional.ofNullable(compound.get("Enchantments"))
                .flatMap(tag -> CatnipCodecUtils.decode(ItemEnchantments.CODEC, registries, tag))
                .ifPresent(enchantments -> create_toolbox_utils$enchantments = enchantments);
        Optional.ofNullable(compound.get("Owner"))
                .flatMap(tag -> CatnipCodecUtils.decode(ExtraCodecs.GAME_PROFILE, registries, tag))
                .ifPresent(this::create_toolbox_utils$setOwner);
    }

    @Inject(method = "write", at = @At("RETURN"), remap = false)
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        Optional.ofNullable(create_toolbox_utils$enchantments)
                .flatMap(enchantments -> CatnipCodecUtils.encode(ItemEnchantments.CODEC, registries, enchantments))
                .ifPresent(tag -> compound.put("Enchantments", tag));
        Optional.ofNullable(create_toolbox_utils$owner)
                .flatMap(profile -> CatnipCodecUtils.encode(ExtraCodecs.GAME_PROFILE, registries, profile))
                .ifPresent(tag -> compound.put("Owner", tag));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
    }
}
