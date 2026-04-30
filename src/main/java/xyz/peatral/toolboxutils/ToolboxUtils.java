package xyz.peatral.toolboxutils;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(ToolboxUtils.ID)
@EventBusSubscriber
public class ToolboxUtils {
    public static final String ID = "toolboxutils";

    public ToolboxUtils() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                RemoveToolboxProxyPayload.TYPE,
                RemoveToolboxProxyPayload.STREAM_CODEC,
                RemoveToolboxProxyPayload::handle
        );
    }
}
