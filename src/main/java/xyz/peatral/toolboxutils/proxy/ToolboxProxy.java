package xyz.peatral.toolboxutils.proxy;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class ToolboxProxy extends ToolboxBlockEntity {

    private final ItemStack sourceStack;

    public ToolboxProxy(ItemStack stack, Level level, BlockPos blockPos) {
        super(AllBlockEntityTypes.TOOLBOX.get(), blockPos,
                ((BlockItem) stack.getItem()).getBlock().defaultBlockState());

        this.sourceStack = stack;
        this.level = level;

        this.setUniqueId(stack.getOrDefault(AllDataComponents.TOOLBOX_UUID, UUID.randomUUID()));
        readInventory(stack.getOrDefault(AllDataComponents.TOOLBOX_INVENTORY, ItemContainerContents.EMPTY));

        if (stack.has(DataComponents.CUSTOM_NAME)) {
            setCustomName(stack.getHoverName());
        }
    }

    public void persistToItem() {
        DataComponentMap.Builder builder = DataComponentMap.builder();

        this.collectImplicitComponents(builder);

        DataComponentMap results = builder.build();
        sourceStack.applyComponents(results);

        if (hasCustomName()) {
            sourceStack.set(DataComponents.CUSTOM_NAME, getName());
        }
    }

    @Override
    public void sendData() {
        persistToItem();
    }

    @Override
    public void setChanged() {
        persistToItem();
    }
}
