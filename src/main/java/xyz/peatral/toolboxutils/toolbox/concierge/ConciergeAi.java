package xyz.peatral.toolboxutils.toolbox.concierge;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.level.Level;
import xyz.peatral.toolboxutils.ToolboxActivities;

import java.util.Optional;
import java.util.UUID;

public class ConciergeAi {

    public static Brain<?> makeBrain(Brain<Allay> brain) {
        initConciergeActivity(brain);
        return brain;
    }

    private static void initConciergeActivity(Brain<Allay> brain) {
        brain.addActivityWithConditions(
            ToolboxActivities.HELPING_PLAYER.get(),
            ImmutableList.of(
                    Pair.of(0, StayCloseToTarget.create(ConciergeAi::getLikedPlayerPositionTracker, entity -> true, 4, 10, 2.25F)),
                    Pair.of(1, SetEntityLookTargetSometimes.create(6.0F, UniformInt.of(30, 60))),
                    Pair.of(
                            2,
                            new RunOne<>(
                                    ImmutableList.of(
                                            Pair.of(RandomStroll.fly(1.0F), 2),
                                            Pair.of(SetWalkTargetFromLookTarget.create(1.0F, 3), 2),
                                            Pair.of(new DoNothing(20, 40), 1)
                                    )
                            )
                    )
            ),
            ImmutableSet.of()
        );
    }

    private static Optional<PositionTracker> getLikedPlayerPositionTracker(LivingEntity entity) {
        return ConciergeAi.getLikedPlayer(entity).map(serverPlayer -> new EntityTracker(serverPlayer, true));
    }

    public static Optional<ServerPlayer> getLikedPlayer(LivingEntity entity) {
        Level level = entity.level();
        if (!level.isClientSide() && level instanceof ServerLevel serverlevel) {
            Optional<UUID> optional = entity.getBrain().getMemory(MemoryModuleType.LIKED_PLAYER);
            if (optional.isPresent()) {
                if (serverlevel.getEntity(optional.get()) instanceof ServerPlayer serverplayer
                        && (serverplayer.gameMode.isSurvival() || serverplayer.gameMode.isCreative())
                        && serverplayer.closerThan(entity, 256.0)) {
                    return Optional.of(serverplayer);
                }

                return Optional.empty();
            }
        }

        return Optional.empty();
    }
}
