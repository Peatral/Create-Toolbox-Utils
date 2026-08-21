package xyz.peatral.toolboxutils.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xyz.peatral.toolboxutils.ToolboxUtils;
import xyz.peatral.toolboxutils.toolbox.proxy.ToolboxProxyController;
import xyz.peatral.toolboxutils.toolbox.proxy.ToolboxProxyHandler;

import java.util.UUID;

public record RemoveToolboxProxyPacket(UUID uuid) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ToolboxUtils.ID, "remove_toolbox_proxy");
    public static final CustomPacketPayload.Type<RemoveToolboxProxyPacket> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveToolboxProxyPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            RemoveToolboxProxyPacket::uuid,
            RemoveToolboxProxyPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        Level level = context.player().level();
        ToolboxProxyController proxy = ToolboxProxyHandler.getProxy(level, uuid);
        if (proxy != null) {
            ToolboxProxyHandler.onUnload(proxy);
        }
    }
}
