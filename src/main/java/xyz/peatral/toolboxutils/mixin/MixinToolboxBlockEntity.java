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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.peatral.toolboxutils.IEnchantableToolbox;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@Mixin(ToolboxBlockEntity.class)
public abstract class MixinToolboxBlockEntity extends SmartBlockEntity implements IEnchantableToolbox {
    @Unique
    public Map<Enchantment, Integer> toolboxutils$enchantments = new HashMap<>();

    @Nullable
    private GameProfile toolboxutils$owner;

    public MixinToolboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Map<Enchantment, Integer> getEnchantments() {
        return toolboxutils$enchantments;
    }

    @Override
    public void setEnchantments(Map<Enchantment, Integer> enchantments) {
        toolboxutils$enchantments = enchantments;
    }

    @Override
    public void setOwner(GameProfile gameProfile) {
        synchronized (this) {
            toolboxutils$owner = gameProfile;
        }

        this.setChanged();
    }

    @Nullable
    @Override
    public GameProfile getOwnerProfile() {
        return toolboxutils$owner;
    }

    @Inject(method = "lazyTick", at = @At("RETURN"), remap = false)
    public void lazyTick(CallbackInfo ci) {
        if (toolboxutils$enchantments == null || toolboxutils$enchantments.isEmpty() || toolboxutils$enchantments.getOrDefault(Enchantments.LOYALTY, 0) < 1) {
            return;
        }
        if (toolboxutils$owner == null || toolboxutils$owner.getId() == null || level == null) {
            return;
        }
        Player player = level.getPlayerByUUID(toolboxutils$owner.getId());
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
            toolboxutils$enchantments = EnchantmentHelper.deserializeEnchantments(compound.getList(ItemStack.TAG_ENCH, CompoundTag.TAG_COMPOUND));
        }

        if (compound.contains("Owner", 10)) {
            this.setOwner(NbtUtils.readGameProfile(compound.getCompound("Owner")));
        }
    }

    @Inject(method = "write", at = @At("RETURN"), remap = false)
    protected void write(CompoundTag compound, boolean clientPacket, CallbackInfo ci) {
        ListTag listtag = new ListTag();
        for(Map.Entry<Enchantment, Integer> entry : toolboxutils$enchantments.entrySet()) {
            Enchantment enchantment = entry.getKey();
            if (enchantment != null) {
                listtag.add(EnchantmentHelper.storeEnchantment(EnchantmentHelper.getEnchantmentId(enchantment), entry.getValue()));
            }
        }
        compound.put(ItemStack.TAG_ENCH, listtag);

        if (toolboxutils$owner != null) {
            CompoundTag ownerTag = new CompoundTag();
            NbtUtils.writeGameProfile(ownerTag, toolboxutils$owner);
            compound.put("Owner", ownerTag);
        }
    }
}
