package xyz.peatral.toolboxutils.mixin;

import com.simibubi.create.AllTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.TridentLoyaltyEnchantment;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TridentLoyaltyEnchantment.class)
public class MixinTridentLoyaltyEnchantment extends Enchantment {
    public MixinTridentLoyaltyEnchantment(Rarity pRarity, EnchantmentCategory pCategory, EquipmentSlot[] pApplicableSlots) {
        super(pRarity, pCategory, pApplicableSlots);
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return super.canEnchant(stack) || stack.is(AllTags.AllItemTags.TOOLBOXES.tag);
    }
}
