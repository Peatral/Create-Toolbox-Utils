package xyz.peatral.toolboxutils;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Optional;

public interface IEnchantableToolbox {
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
}
