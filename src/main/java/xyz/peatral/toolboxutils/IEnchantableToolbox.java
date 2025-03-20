package xyz.peatral.toolboxutils;

import com.mojang.authlib.GameProfile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Map;

public interface IEnchantableToolbox {
    Map<Enchantment, Integer> create_toolbox_utils$getEnchantments();
    void create_toolbox_utils$setEnchantments(Map<Enchantment, Integer> enchantments);

    void create_toolbox_utils$setOwner(GameProfile gameProfile);
    boolean create_toolbox_utils$isOwner(Player player);

    default int create_toolbox_utils$getLoyaltyLevel() {
        return create_toolbox_utils$getEnchantments().getOrDefault(Enchantments.LOYALTY, 0);
    }
}
