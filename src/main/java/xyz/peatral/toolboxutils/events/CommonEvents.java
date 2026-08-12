package xyz.peatral.toolboxutils.events;

import com.simibubi.create.AllTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import xyz.peatral.toolboxutils.toolbox.ToolboxProxyHandler;

@EventBusSubscriber
public class CommonEvents {
    @SubscribeEvent
    public static void onEntityTravelToDimension(final EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof Allay allay && event.getEntity().level() instanceof ServerLevel from)) return;

        MinecraftServer server = from.getServer();

        Level to = server.getLevel(event.getDimension());

        ItemStack itemStack = allay.getMainHandItem();
        if (itemStack.is(AllTags.AllItemTags.TOOLBOXES.tag)) {
            ToolboxProxyHandler.changeProxyDimension(from, to, itemStack, allay.blockPosition(), true);
        }
    }
}
