package xyz.peatral.toolboxutils.mixin;

import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.peatral.toolboxutils.IEnchantableToolbox;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(ToolboxHandler.class)
public class MixinToolboxHandler {
    @Inject(method = "getNearest", at = @At("RETURN"), remap = false, cancellable = true)
    private static void getNearest(LevelAccessor world, Player player, int maxAmount, CallbackInfoReturnable<List<ToolboxBlockEntity>> ci) {
        ci.setReturnValue(ci.getReturnValue().stream().filter(toolboxBlockEntity -> {
            if (toolboxBlockEntity instanceof IEnchantableToolbox tb) {
                return tb.getEnchantments().getOrDefault(Enchantments.LOYALTY, 0) < 1
                        || tb.getOwnerProfile() != null && player.getUUID().equals(tb.getOwnerProfile().getId());
            } else {
                return true;
            }
        }).collect(Collectors.toList()));
    }
}
