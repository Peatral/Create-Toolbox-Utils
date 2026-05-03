package xyz.peatral.toolboxutils.toolbox;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.equipment.toolbox.ToolboxInventory;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Optional;

// TODO: Concierge Hat
// TODO: Allay AI Overhaul
// TODO: Render toolbox better
// TODO: Curios renderer
// TODO: think about how curios can be improved upon (to make it not op)

public interface IExtendedToolbox {
    ItemEnchantments create_toolbox_utils$getEnchantments();
    void create_toolbox_utils$setEnchantments(ItemEnchantments enchantments);

    void create_toolbox_utils$setOwner(GameProfile gameProfile);
    boolean create_toolbox_utils$isOwner(Player player);

    default int create_toolbox_utils$getLoyaltyLevel(RegistryAccess registryAccess) {
        return Optional.ofNullable(create_toolbox_utils$getEnchantments())
                .flatMap(toolboxEnchantments -> registryAccess.registry(Registries.ENCHANTMENT)
                        .flatMap(enchantments -> enchantments
                                .asLookup()
                                .get(Enchantments.LOYALTY)
                                .map(toolboxEnchantments::getLevel)
                        )
                ).orElse(0);
    }

    ToolboxInventory create_toolbox_utils$getInventory();
    boolean create_toolbox_utils$isProxy();
    void create_toolbox_utils$setProxy(boolean isProxy);
    void create_toolbox_utils$setSource(ItemStack stack);

    static boolean isProxy(Object object) {
        return object instanceof IExtendedToolbox tb && tb.create_toolbox_utils$isProxy();
    }
}
