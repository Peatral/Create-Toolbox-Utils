package xyz.peatral.toolboxutils.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.model.AllayModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import xyz.peatral.toolboxutils.ToolboxPartialModels;

public class ConciergeHatArmorLayer extends RenderLayer<Allay, AllayModel> {
    public ConciergeHatArmorLayer(RenderLayerParent<Allay, AllayModel> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack ms, MultiBufferSource buffer, int light, Allay livingEntity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!livingEntity.getPersistentData().getBoolean("ConciergeHat")) {
            return;
        }

        PartialModel hat = ToolboxPartialModels.CONCIERGE_HAT;
        AllayModel model = getParentModel();

        ms.pushPose();
        var msr = TransformStack.of(ms);

        model.root().translateAndRotate(ms);
        model.root().getChild("head").translateAndRotate(ms);
        ms.scale(1, -1, -1);
        ms.translate(0, 3.5F / 16.0F, 0);
        msr.rotateXDegrees(-8.5F);
        BlockState air = Blocks.AIR.defaultBlockState();
        CachedBuffers.partial(hat, air)
                .disableDiffuse()
                .light(light)
                .renderInto(ms, buffer.getBuffer(Sheets.cutoutBlockSheet()));

        ms.popPose();
    }
}
