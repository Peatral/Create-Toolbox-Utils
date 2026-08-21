package xyz.peatral.toolboxutils.compat.curios;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import xyz.peatral.toolboxutils.toolbox.proxy.ToolboxProxyHandler;

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

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!ModList.get().isLoaded("curios") || !(event.getEntity() instanceof ServerPlayer player)) return;

        MinecraftServer server = player.getServer();
        if (server == null) return;

        Level from = server.getLevel(event.getFrom());
        Level to = server.getLevel(event.getTo());

        CuriosApi.getCuriosInventory(player)
                .map(iCuriosItemHandler -> iCuriosItemHandler.findCurios(itemStack -> itemStack.is(AllTags.AllItemTags.TOOLBOXES.tag)))
                .ifPresent(slots -> {
                    slots.forEach(result -> {
                        ToolboxProxyHandler.changeProxyDimension(from, to, result.stack(), player.blockPosition());
                    });
                });
    }
}
