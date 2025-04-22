package xyz.peatral.toolboxutils.mixin;

import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.peatral.toolboxutils.IEnchantableToolbox;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Mixin(ToolboxHandler.class)
public class MixinToolboxHandler {
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
}
