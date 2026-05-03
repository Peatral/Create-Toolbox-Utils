package xyz.peatral.toolboxutils.toolbox.concierge;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.simibubi.create.Create;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class AttributeModifiers {
    public static final AttributeModifier followRangeAttributeModifier =
            new AttributeModifier(Create.asResource("concierge_follow_range_attribute_modifier"), 256,
                    AttributeModifier.Operation.ADD_VALUE);

    public static final Multimap<Holder<Attribute>, AttributeModifier> conciergeModifier = ImmutableMultimap.of(
            Attributes.FOLLOW_RANGE, followRangeAttributeModifier
    );
}
