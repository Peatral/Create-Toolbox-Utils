package xyz.peatral.toolboxutils.mixin;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlock;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.peatral.toolboxutils.IEnchantableToolbox;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@Mixin(ToolboxBlockEntity.class)
public abstract class MixinToolboxBlockEntity extends SmartBlockEntity implements IEnchantableToolbox {
    @Unique
    public Map<Enchantment, Integer> create_toolbox_utils$enchantments = new HashMap<>();

    @Unique
    @Nullable
    private GameProfile create_toolbox_utils$owner;

    public MixinToolboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Map<Enchantment, Integer> create_toolbox_utils$getEnchantments() {
        return create_toolbox_utils$enchantments;
    }

    @Override
    public void create_toolbox_utils$setEnchantments(Map<Enchantment, Integer> enchantments) {
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

    @Inject(method = "createMenu", at = @At("RETURN"), remap = false, cancellable = true)
    public void createMenu(int id, Inventory inv, Player player, CallbackInfoReturnable<AbstractContainerMenu> cir) {
        if (create_toolbox_utils$isOwner(player) || create_toolbox_utils$getLoyaltyLevel() < 2) {
            return;
        }
        cir.setReturnValue(null);
    }

    @Inject(method = "lazyTick", at = @At("RETURN"), remap = false)
    public void lazyTick(CallbackInfo ci) {
        if (create_toolbox_utils$owner == null || create_toolbox_utils$owner.getId() == null || level == null || create_toolbox_utils$getLoyaltyLevel() < 3) {
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
    protected void read(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
        if (compound.contains(ItemStack.TAG_ENCH, CompoundTag.TAG_LIST)) {
            create_toolbox_utils$enchantments = EnchantmentHelper.deserializeEnchantments(compound.getList(ItemStack.TAG_ENCH, CompoundTag.TAG_COMPOUND));
        }

        if (compound.contains("Owner", CompoundTag.TAG_COMPOUND)) {
            this.create_toolbox_utils$setOwner(NbtUtils.readGameProfile(compound.getCompound("Owner")));
        }
    }

    @Inject(method = "write", at = @At("RETURN"), remap = false)
    protected void write(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
        ListTag listtag = new ListTag();
        for(Map.Entry<Enchantment, Integer> entry : create_toolbox_utils$enchantments.entrySet()) {
            Enchantment enchantment = entry.getKey();
            if (enchantment != null) {
                listtag.add(EnchantmentHelper.storeEnchantment(EnchantmentHelper.getEnchantmentId(enchantment), entry.getValue()));
            }
        }
        compound.put(ItemStack.TAG_ENCH, listtag);

        if (create_toolbox_utils$owner != null) {
            CompoundTag ownerTag = new CompoundTag();
            NbtUtils.writeGameProfile(ownerTag, create_toolbox_utils$owner);
            compound.put("Owner", ownerTag);
        }
    }
}
