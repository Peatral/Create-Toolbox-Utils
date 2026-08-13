package xyz.peatral.toolboxutils.events;

import com.simibubi.create.AllTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import xyz.peatral.toolboxutils.ToolboxRegionTickets;

import java.util.List;

@EventBusSubscriber
public class CommonEvents {
    @SubscribeEvent
    public static void onEntityTravelToDimension(final EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ServerLevel from = serverPlayer.serverLevel();
            MinecraftServer server = from.getServer();
            ServerLevel to = server.getLevel(event.getDimension());

            if (to == null) {
                return;
            }
            double searchRadius = 32;
            AABB searchArea = serverPlayer.getBoundingBox().inflate(searchRadius);
            List<Allay> allays = serverPlayer.level().getEntitiesOfClass(Allay.class, searchArea);
            for (Allay allay : allays) {
                ItemStack itemStack = allay.getMainHandItem();
                boolean isConcierging = itemStack.is(AllTags.AllItemTags.TOOLBOXES.tag);
                boolean isFollowingPlayer = allay.getBrain()
                        .getMemory(MemoryModuleType.LIKED_PLAYER)
                        .map(uuid -> uuid.equals(serverPlayer.getUUID()))
                        .orElse(false);
                if (isConcierging && isFollowingPlayer) {
                    // Each concierge will handle their own teleportation once the player has travelled to the new dimension
                    from.getChunkSource().addRegionTicket(
                            ToolboxRegionTickets.TRAVELLING_CONCIERGE,
                            allay.chunkPosition(),
                            2,
                            allay.getUUID()
                    );
                }
            }
        }
    }
}
