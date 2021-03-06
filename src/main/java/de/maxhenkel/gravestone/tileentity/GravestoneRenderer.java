package de.maxhenkel.gravestone.tileentity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import de.maxhenkel.corelib.client.PlayerSkins;
import de.maxhenkel.gravestone.Main;
import de.maxhenkel.gravestone.blocks.GraveStoneBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.model.HumanoidHeadModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

import java.util.UUID;

public class GravestoneRenderer extends TileEntityRenderer<GraveStoneTileEntity> {


    public GravestoneRenderer(TileEntityRendererDispatcher rendererDispatcher) {
        super(rendererDispatcher);
    }

    @Override
    public void render(GraveStoneTileEntity target, float f1, MatrixStack matrixStack, IRenderTypeBuffer buffer, int light, int i2) {
        String name = target.getPlayerName();

        if (name == null || name.isEmpty()) {
            return;
        }

        Direction direction = target.getBlockState().get(GraveStoneBlock.FACING);

        matrixStack.push();

        matrixStack.translate(0.5D, 1D, 0.5D);

        matrixStack.rotate(Vector3f.XP.rotationDegrees(180F));

        matrixStack.rotate(Vector3f.YP.rotationDegrees(180F + direction.getHorizontalAngle()));

        FontRenderer renderer = renderDispatcher.getFontRenderer();

        int textWidth = renderer.getStringWidth(name);
        double textScale = Math.min(0.8D / textWidth, 0.02D);

        matrixStack.translate(0D, 0.3D, 0.37D);

        matrixStack.scale((float) textScale, (float) textScale, (float) textScale);

        float left = (float) (-renderer.getStringWidth(name) / 2);
        renderer.renderString(name, left, 0F, Main.CLIENT_CONFIG.graveTextColor, false, matrixStack.getLast().getMatrix(), buffer, false, 0, light);

        matrixStack.pop();

        BlockState state = target.getWorld().getBlockState(target.getPos().down());

        boolean render = Block.isOpaque(state.getRenderShape(target.getWorld(), target.getPos()));

        if (target.renderHead() && target.getPlayerUUID() != null && !target.getPlayerUUID().isEmpty() && Main.CLIENT_CONFIG.renderSkull.get() && render) {
            try {
                renderSkull(target.getPlayerUUID(), target.getPlayerName(), direction, matrixStack, buffer);
            } catch (Exception e) {
            }
        }
    }

    public void renderSkull(String uuid, String name, Direction rotation, MatrixStack matrixStack, IRenderTypeBuffer buffer) {
        HumanoidHeadModel model = new HumanoidHeadModel();
        ResourceLocation resourcelocation = DefaultPlayerSkin.getDefaultSkinLegacy();

        if (uuid != null) {
            try {
                resourcelocation = PlayerSkins.getSkin(UUID.fromString(uuid), name);
            } catch (Exception e) {
            }
        }

        matrixStack.push();

        matrixStack.translate(0.5D, 0D, 0.5D);

        matrixStack.rotate(Vector3f.YP.rotationDegrees(180F - rotation.getHorizontalAngle()));
        matrixStack.rotate(Vector3f.YP.rotationDegrees(-26F));
        matrixStack.translate(0D, -0.14D, 0.18D);
        matrixStack.rotate(Vector3f.XP.rotationDegrees(180F));

        matrixStack.rotate(Vector3f.XP.rotationDegrees(-61F));

        RenderSystem.color4f(1F, 1F, 1F, 1F);
        model.render(matrixStack, buffer.getBuffer(model.getRenderType(resourcelocation)), 15728880, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        matrixStack.pop();
    }
}
