package xyz.peatral.toolboxutils.mixin;

import com.simibubi.create.AllTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class MixinItem {
    @Inject(method = "isEnchantable", at = @At("RETURN"), cancellable = true)
    private void isEnchantable(ItemStack stack, CallbackInfoReturnable<Boolean> cir){
        if (stack.is(AllTags.AllItemTags.TOOLBOXES.tag)) {
            cir.setReturnValue(true);
        }
    }
}
