package xyz.peatral.toolboxutils;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.schedule.Activity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ToolboxActivities {
    public static final DeferredRegister<Activity> REGISTRY = DeferredRegister.create(Registries.ACTIVITY, ToolboxUtils.ID);

    public static final DeferredHolder<Activity, Activity> HELPING_PLAYER = registerActivity("helping_player");

    private static DeferredHolder<Activity, Activity> registerActivity(String id) {
        return REGISTRY.register(id, () -> new Activity(id));
    }

    public static void register(IEventBus modEventBus) {
        REGISTRY.register(modEventBus);
    }
}
