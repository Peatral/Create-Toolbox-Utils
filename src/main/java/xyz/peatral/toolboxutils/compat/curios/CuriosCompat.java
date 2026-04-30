package xyz.peatral.toolboxutils.compat.curios;

import com.simibubi.create.AllBlocks;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import top.theillusivec4.curios.api.CuriosCapability;

import java.util.Arrays;

@EventBusSubscriber
public class CuriosCompat {
    @SubscribeEvent
    public static void registerCaps(final RegisterCapabilitiesEvent evt) {
        if (!ModList.get().isLoaded("curios")) return;

        ItemLike[] items = Arrays.stream(AllBlocks.TOOLBOXES.toArray()).map(BlockEntry::get).toArray(com.simibubi.create.content.equipment.toolbox.ToolboxBlock[]::new);
        evt.registerItem(
                CuriosCapability.ITEM,
                (stack, context) -> new ToolboxCurio(stack),
                items
        );
    }
}
