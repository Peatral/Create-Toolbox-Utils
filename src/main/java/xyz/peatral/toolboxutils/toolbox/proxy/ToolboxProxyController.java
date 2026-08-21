package xyz.peatral.toolboxutils.toolbox.proxy;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import xyz.peatral.toolboxutils.ToolboxDataComponents;
import xyz.peatral.toolboxutils.network.SyncToolboxProxyPacket;
import xyz.peatral.toolboxutils.toolbox.IExtendedToolbox;
import xyz.peatral.toolboxutils.toolbox.IFilterable;

import java.util.*;

public class ToolboxProxyController implements IToolboxProxyCallbacks {
    @NotNull private ItemStack toolboxItemStack;
    @NotNull private ToolboxBlockEntity toolbox;


    private Level level;
    @NotNull private BlockPos blockPos;

    public ToolboxProxyController(@NotNull ItemStack toolboxItemStack, @NotNull ToolboxBlockEntity toolbox) {
        this.toolbox = toolbox;
        blockPos = toolbox.getBlockPos();
        this.toolboxItemStack = toolboxItemStack;
        level = toolbox.getLevel();
    }

    public @NotNull ToolboxBlockEntity getToolbox() {
        return toolbox;
    }

    public Level getLevel() {
        return level;
    }

    public @NotNull BlockPos getBlockPos() {
        return blockPos;
    }

    public void setBlockPos(BlockPos blockPos) {
        if (this.blockPos.equals(blockPos) || level == null) {
            return;
        }
        this.blockPos = blockPos;
        recreateToolbox();
    }

    public void setLevel(Level level) {
        if (level == null) {
            return;
        }
        this.level = level;
        recreateToolbox();
    }

    /**
     * Recreates the toolbox block entity at the current position in the current level
     */
    public void recreateToolbox() {
        toolbox.invalidate();
        Map<Integer, WeakHashMap<Player, Integer>> connectedPlayers = null;

        if (toolbox instanceof IExtendedToolbox extendedToolbox) {
            connectedPlayers = extendedToolbox.create_toolbox_utils$getConnectedPlayers();
        }
        Optional<ToolboxBlockEntity> optionalToolboxBlockEntity = createToolboxBEFromStack(toolboxItemStack, level, blockPos);
        if (optionalToolboxBlockEntity.isEmpty()) {
            return;
        }
        toolbox.invalidate();
        toolbox = optionalToolboxBlockEntity.get();
        if (toolbox instanceof IExtendedToolbox extendedToolbox && connectedPlayers != null) {
            // Simply relying on the entityTick inside of ToolboxProxyHandler will take too long
            // so i manually link this up again
            extendedToolbox.create_toolbox_utils$setConnectedPlayers(connectedPlayers);
        }
        setToolboxBEProxyCallback(toolbox, this);
        toolbox.initialize();
    }

    public void setItemStack(ItemStack stack) {
        toolboxItemStack = stack;
    }

    public UUID getUniqueId() {
        return toolbox.getUniqueId();
    }

    public void initialize() {
        toolbox.initialize();
        ToolboxProxyHandler.onLoad(this);
    }

    public void invalidate() {
        toolbox.invalidate();
        ToolboxProxyHandler.onUnload(this);
    }

    public void tick() {
        toolbox.tick();
    }

    public void onSetChanged() {
        sync();
    }

    public void onSendData() {
        sync();
    }

    public void onLazyTick() {
        ToolboxProxyHandler.onLoad(this);
    }

    private void persistToItemStack() {
        if (toolbox instanceof IExtendedToolbox extendedToolbox) {
            extendedToolbox.create_toolbox_utils$persistToItemStack(toolboxItemStack);
        }
    }

    /**
     * Starts a sync from server to clients
     */
    public void sync() {
        persistToItemStack();
        CompoundTag compoundTag = new CompoundTag();
        toolbox.writeClient(compoundTag, level.registryAccess());
        ToolboxProxyHandler.syncProxy(level, getUniqueId());
    }

    /**
     * Creates the sync packet for a proxy. Always assumes this will only be sent to players in the same dimension
     * @return the packet
     */
    public Optional<SyncToolboxProxyPacket> getSyncPacket() {
        Level level = getLevel();
        if (level == null) {
            return Optional.empty();
        }
        CompoundTag compoundTag = new CompoundTag();
        toolbox.writeClient(compoundTag, level.registryAccess());
        return Optional.of(new SyncToolboxProxyPacket(getUniqueId(), compoundTag));
    }

    /**
     * Handles a sync on the client
     * @param data the block entity data
     */
    public void handleSync(CompoundTag data) {
        toolbox.readClient(data, getLevel().registryAccess());
        persistToItemStack();
    }

    /**
     * Creates a {@link ToolboxBlockEntity} for a given {@link ItemStack}
     * @param stack the toolbox item stack
     * @param level the level the toolbox is in
     * @param pos the position the toolbox is at
     * @return the toolbox block entity
     */
    private static Optional<ToolboxBlockEntity> createToolboxBEFromStack(ItemStack stack, Level level, BlockPos pos) {
        Item item = stack.getItem();
        if (!(item instanceof BlockItem blockItem)) return Optional.empty();
        ToolboxBlockEntity be = new ToolboxBlockEntity(AllBlockEntityTypes.TOOLBOX.get(), pos,
                (blockItem).getBlock().defaultBlockState());

        be.setLevel(level);

        be.setUniqueId(stack.getOrDefault(AllDataComponents.TOOLBOX_UUID, UUID.randomUUID()));
        be.readInventory(stack.getOrDefault(AllDataComponents.TOOLBOX_INVENTORY, ItemContainerContents.EMPTY));

        if (stack.has(DataComponents.CUSTOM_NAME)) {
            be.setCustomName(stack.getHoverName());
        }

        if (!(be instanceof IExtendedToolbox extendedToolbox)) {
            return Optional.empty();
        }

        ItemContainerContents loadedFilters = stack.get(ToolboxDataComponents.TOOLBOX_FILTERS);
        if (loadedFilters != null && extendedToolbox.create_toolbox_utils$getInventory() instanceof IFilterable filterable) {
            List<ItemStack> filters = filterable.create_toolbox_utils$getFilters();
            for (int i = 0; i < filters.size(); i++) {
                if (i < loadedFilters.getSlots()) {
                    filters.set(i, loadedFilters.getStackInSlot(i));
                }
            }
        }
        return Optional.of(be);
    }

    /**
     * Links together a {@link ToolboxBlockEntity} with a {@link ToolboxProxyController}
     * @param toolboxBlockEntity the block entity
     * @param toolboxProxyController the proxy controller
     */
    private static void setToolboxBEProxyCallback(ToolboxBlockEntity toolboxBlockEntity, ToolboxProxyController toolboxProxyController) {
        if (toolboxBlockEntity instanceof IExtendedToolbox extendedToolbox) {
            extendedToolbox.create_toolbox_utils$setProxyCallbacks(toolboxProxyController);
        }
    }

    /**
     * Creates a {@link ToolboxProxyController} with a {@link ToolboxBlockEntity} for a given {@link ItemStack}
     * @param stack the toolbox item stack
     * @param level the level the toolbox is in
     * @param pos the position the toolbox is at
     * @return the proxy controller
     */
    public static Optional<ToolboxProxyController> create(ItemStack stack, Level level, BlockPos pos) {
        Optional<ToolboxBlockEntity> be = createToolboxBEFromStack(stack, level, pos);
        if (be.isEmpty()) return Optional.empty();
        ToolboxProxyController controller = new ToolboxProxyController(stack, be.get());
        setToolboxBEProxyCallback(be.get(), controller);
        return Optional.of(controller);
    }
}
