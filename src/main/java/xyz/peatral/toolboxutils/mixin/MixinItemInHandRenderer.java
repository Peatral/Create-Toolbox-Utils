package xyz.peatral.toolboxutils.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllTags;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRenderer {

    @WrapMethod(method = "renderItem")
    public void renderItem(LivingEntity entity, ItemStack itemStack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int seed, Operation<Void> original) {
        if (!(entity instanceof Allay) || !(itemStack.is(AllTags.AllItemTags.TOOLBOXES.tag) && !leftHand)) {
            original.call(entity, itemStack, displayContext, leftHand, poseStack, buffer, seed);
            return;
        }
        poseStack.clear();
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(25.0F));
        poseStack.translate(0, 0.125F, 0);
        poseStack.scale(0.625F, 0.625F, 0.625F);
        original.call(entity, itemStack, ItemDisplayContext.NONE, false, poseStack, buffer, seed);
        poseStack.popPose();
    }
}
