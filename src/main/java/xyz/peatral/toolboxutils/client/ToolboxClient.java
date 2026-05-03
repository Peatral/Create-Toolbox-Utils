package xyz.peatral.toolboxutils.client;

import net.minecraft.client.renderer.entity.AllayRenderer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import xyz.peatral.toolboxutils.ToolboxPartialModels;

@EventBusSubscriber
public class ToolboxClient {

    @SubscribeEvent
    public static void clientInit(final FMLClientSetupEvent event) {
        ToolboxPartialModels.init();
    }

    @SubscribeEvent
    public static void addEntityRendererLayers(EntityRenderersEvent.AddLayers event) {
       AllayRenderer renderer = event.getRenderer(EntityType.ALLAY);
       if (renderer != null) {
           renderer.addLayer(new ConciergeHatArmorLayer(renderer));
       }
    }
}
