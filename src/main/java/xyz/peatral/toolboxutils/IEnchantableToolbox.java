package xyz.peatral.toolboxutils;

import com.mojang.authlib.GameProfile;
import net.minecraft.world.item.enchantment.Enchantment;

import javax.annotation.Nullable;
import java.util.Map;

public interface IEnchantableToolbox {
    Map<Enchantment, Integer> getEnchantments();
    void setEnchantments(Map<Enchantment, Integer> enchantments);

    void setOwner(GameProfile gameProfile);
    @Nullable
    GameProfile getOwnerProfile();
}
