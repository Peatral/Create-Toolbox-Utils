package xyz.peatral.toolboxutils.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlock;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import com.simibubi.create.content.equipment.toolbox.ToolboxInventory;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.peatral.toolboxutils.ToolboxDataComponents;
import xyz.peatral.toolboxutils.toolbox.IExtendedToolbox;
import xyz.peatral.toolboxutils.toolbox.IFilterable;
import xyz.peatral.toolboxutils.toolbox.proxy.IToolboxProxyCallbacks;
import xyz.peatral.toolboxutils.toolbox.proxy.ToolboxProxyHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

@Mixin(ToolboxBlockEntity.class)
public abstract class MixinToolboxBlockEntity extends SmartBlockEntity implements Nameable, IExtendedToolbox {
    @Shadow
    ToolboxInventory inventory;

    @Shadow
    public abstract boolean hasCustomName();

    @Shadow
    private Map<Integer, WeakHashMap<Player, Integer>> connectedPlayers;
    @Unique
    public ItemEnchantments create_toolbox_utils$enchantments = null;

    @Unique
    @Nullable
    private GameProfile create_toolbox_utils$owner;

    @Unique
    private IToolboxProxyCallbacks create_toolbox_utils$proxyCallbacks;

    public MixinToolboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public ToolboxInventory create_toolbox_utils$getInventory() {
        return inventory;
    }

    @Override
    public ItemEnchantments create_toolbox_utils$getEnchantments() {
        return create_toolbox_utils$enchantments;
    }

    @Override
    public void create_toolbox_utils$setEnchantments(ItemEnchantments enchantments) {
        create_toolbox_utils$enchantments = enchantments;
    }

    @Override
    public void create_toolbox_utils$setOwner(GameProfile gameProfile) {
        synchronized (this) {
            create_toolbox_utils$owner = gameProfile;
        }
        this.setChanged();
    }

    @Override
    public boolean create_toolbox_utils$isOwner(Player player) {
        return create_toolbox_utils$owner == null || player.getUUID().equals(create_toolbox_utils$owner.getId());
    }

    @Override
    public boolean create_toolbox_utils$isProxy() {
        return create_toolbox_utils$proxyCallbacks != null;
    }

    @Override
    public void create_toolbox_utils$setProxyCallbacks(IToolboxProxyCallbacks callbacks) {
        this.create_toolbox_utils$proxyCallbacks = callbacks;
    }

    @WrapOperation(method = "tickPlayers", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;getCompound(Ljava/lang/String;)Lnet/minecraft/nbt/CompoundTag;"))
    public CompoundTag tickPlayers(CompoundTag instance, String key, Operation<CompoundTag> original) {
        if (create_toolbox_utils$isProxy() && key.equals("CreateToolboxData")) {
            return instance.getCompound(ToolboxProxyHandler.PERSISTENT_KEY);
        }
        return original.call(instance, key);
    }

    @WrapMethod(method = "initialize")
    public void initialize(Operation<Void> original) {
        if (create_toolbox_utils$isProxy()) {
            super.initialize();
            create_toolbox_utils$proxyCallbacks.onInitialize();
        } else {
            original.call();
        }
    }

    @WrapMethod(method = "invalidate")
    public void invalidate(Operation<Void> original) {
        if (create_toolbox_utils$isProxy()) {
            super.invalidate();
            create_toolbox_utils$proxyCallbacks.onInvalidate();
        } else {
            original.call();
        }
    }

    @WrapMethod(method = "lazyTick")
    public void lazyTick(Operation<Void> original) {
        if (create_toolbox_utils$isProxy()) {
            create_toolbox_utils$proxyCallbacks.onLazyTick();
        } else {
            original.call();
        }
        if (create_toolbox_utils$owner == null || create_toolbox_utils$owner.getId() == null || level == null || create_toolbox_utils$getLoyaltyLevel(level.registryAccess()) < 2) {
            return;
        }
        Player player = level.getPlayerByUUID(create_toolbox_utils$owner.getId());
        if (player == null) {
            return;
        }
        double maxRange = ToolboxHandler.getMaxRange(player);
        if (getBlockPos().getCenter().distanceTo(player.position()) > maxRange) {
            Block block = getBlockState().getBlock();
            if (block instanceof ToolboxBlock tb) {
                tb.attack(getBlockState(), level, getBlockPos(), player);
            }
        }
    }

    @Inject(method = "read", at = @At("RETURN"), remap = false)
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        Optional.ofNullable(compound.get("Enchantments"))
                .flatMap(tag -> CatnipCodecUtils.decode(ItemEnchantments.CODEC, registries, tag))
                .ifPresent(enchantments -> create_toolbox_utils$enchantments = enchantments);
        Optional.ofNullable(compound.get("Owner"))
                .flatMap(tag -> CatnipCodecUtils.decode(ExtraCodecs.GAME_PROFILE, registries, tag))
                .ifPresent(this::create_toolbox_utils$setOwner);
    }

    @Inject(method = "write", at = @At("RETURN"), remap = false)
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        Optional.ofNullable(create_toolbox_utils$enchantments)
                .flatMap(enchantments -> CatnipCodecUtils.encode(ItemEnchantments.CODEC, registries, enchantments))
                .ifPresent(tag -> compound.put("Enchantments", tag));
        Optional.ofNullable(create_toolbox_utils$owner)
                .flatMap(profile -> CatnipCodecUtils.encode(ExtraCodecs.GAME_PROFILE, registries, profile))
                .ifPresent(tag -> compound.put("Owner", tag));
    }

    @Override
    public void sendData() {
        if (create_toolbox_utils$isProxy()) {
            create_toolbox_utils$proxyCallbacks.onSendData();
        } else {
            super.sendData();
        }
    }

    @Override
    public void setChanged() {
        if (create_toolbox_utils$isProxy()) {
            create_toolbox_utils$proxyCallbacks.onSetChanged();
        } else {
            super.setChanged();
        }
    }

    @Override
    public void create_toolbox_utils$persistToItemStack(ItemStack itemStack) {
        DataComponentMap.Builder builder = DataComponentMap.builder();

        collectImplicitComponents(builder);

        DataComponentMap results = builder.build();
        itemStack.applyComponents(results);

        if (hasCustomName()) {
            itemStack.set(DataComponents.CUSTOM_NAME, getName());
        }

        if (inventory instanceof IFilterable filterable) {
            List<ItemStack> filters = filterable.create_toolbox_utils$getFilters();
            itemStack.set(ToolboxDataComponents.TOOLBOX_FILTERS, ItemContainerContents.fromItems(filters));
        }
    }
    
    @Override
    public Map<Integer, WeakHashMap<Player, Integer>> create_toolbox_utils$getConnectedPlayers() {
        return connectedPlayers;
    }

    @Override
    public void create_toolbox_utils$setConnectedPlayers(Map<Integer, WeakHashMap<Player, Integer>> connectedPlayers) {
        this.connectedPlayers = connectedPlayers;
    }
}
