package xyz.peatral.toolboxutils.network;

import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xyz.peatral.toolboxutils.ToolboxUtils;
import xyz.peatral.toolboxutils.toolbox.IExtendedToolbox;
import xyz.peatral.toolboxutils.toolbox.ToolboxProxyHandler;

import java.util.UUID;

public record SyncToolboxProxyPacket(UUID uuid, CompoundTag data) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ToolboxUtils.ID, "sync_toolbox_proxy");
    public static final CustomPacketPayload.Type<SyncToolboxProxyPacket> TYPE = new CustomPacketPayload.Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncToolboxProxyPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            SyncToolboxProxyPacket::uuid,
            ByteBufCodecs.COMPOUND_TAG,
            SyncToolboxProxyPacket::data,
            SyncToolboxProxyPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        Level level = context.player().level();

        ToolboxBlockEntity toolbox = ToolboxProxyHandler.getProxy(level, uuid);

        if (toolbox instanceof IExtendedToolbox extendedToolbox) {
            extendedToolbox.create_toolbox_utils$handleProxySyncData(data, level.registryAccess());
        }
    }
}
