package xyz.peatral.toolboxutils.mixin;

import com.simibubi.create.content.equipment.toolbox.ToolboxBlock;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.peatral.toolboxutils.IEnchantableToolbox;

import java.util.Optional;

@Mixin(ToolboxBlock.class)
public abstract class MixinToolboxBlock extends HorizontalDirectionalBlock implements IBE<ToolboxBlockEntity> {

    protected MixinToolboxBlock(Properties pProperties) {
        super(pProperties);
    }

    @Inject(method = "setPlacedBy", at = @At("RETURN"), remap = false)
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack, CallbackInfo ci) {
        if (worldIn.isClientSide || stack == null) {
            return;
        }

        this.withBlockEntityDo(worldIn, pos, (be) -> {
            if (be instanceof IEnchantableToolbox tbe) {
                tbe.setEnchantments(EnchantmentHelper.getEnchantments(stack));

                if (placer instanceof Player player) {
                    tbe.setOwner(player.getGameProfile());
                }
            }
        });
    }

    @Inject(method = "getCloneItemStack", at = @At("RETURN"), remap = false)
    public void getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state, CallbackInfoReturnable<ItemStack> cir) {
        Optional<ToolboxBlockEntity> blockEntityOptional = this.getBlockEntityOptional(world, pos);
        if (blockEntityOptional.isPresent() && blockEntityOptional.get() instanceof IEnchantableToolbox tbe) {
            EnchantmentHelper.setEnchantments(tbe.getEnchantments(), cir.getReturnValue());
        }
    }
}
