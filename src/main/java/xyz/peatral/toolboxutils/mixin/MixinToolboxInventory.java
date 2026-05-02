package xyz.peatral.toolboxutils.mixin;

import com.simibubi.create.content.equipment.toolbox.ToolboxInventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xyz.peatral.toolboxutils.toolbox.IFilterable;

import java.util.List;

@Mixin(ToolboxInventory.class)
public class MixinToolboxInventory implements IFilterable {
    @Shadow
    List<ItemStack> filters;

    @Override
    public List<ItemStack> create_toolbox_utils$getFilters() {
        return filters;
    }
}
