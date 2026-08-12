package xyz.peatral.toolboxutils.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;


@EventBusSubscriber
public class ToolboxUtilsPackets {

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                RemoveToolboxProxyPacket.TYPE,
                RemoveToolboxProxyPacket.STREAM_CODEC,
                RemoveToolboxProxyPacket::handle
        );
        registrar.playToClient(
                SyncToolboxProxyPacket.TYPE,
                SyncToolboxProxyPacket.STREAM_CODEC,
                SyncToolboxProxyPacket::handle
        );

        registrar.playToServer(
                ToolboxProxyDisposeAllPacket.TYPE,
                ToolboxProxyDisposeAllPacket.STREAM_CODEC,
                ToolboxProxyDisposeAllPacket::handle
        );
        registrar.playToServer(
                ToolboxProxyEquipPacket.TYPE,
                ToolboxProxyEquipPacket.STREAM_CODEC,
                ToolboxProxyEquipPacket::handle
        );
    }
}
