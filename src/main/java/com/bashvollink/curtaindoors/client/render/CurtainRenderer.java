package com.bashvollink.curtaindoors.client.render;

import com.bashvollink.curtaindoors.CurtainDoors;
import com.bashvollink.curtaindoors.block.CurtainBlock;
import com.bashvollink.curtaindoors.block.CurtainType;
import com.bashvollink.curtaindoors.block.entity.CurtainBlockEntity;
import com.bashvollink.curtaindoors.client.sim.ClothSim;
import com.bashvollink.curtaindoors.client.sim.CurtainSpace;
import com.bashvollink.curtaindoors.client.sim.SimManager;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Draws a whole curtain from its top segment, using the client cloth simulation. */
public class CurtainRenderer implements BlockEntityRenderer<CurtainBlockEntity> {
    private static final ResourceLocation PLASTIC_TEXTURE = CurtainDoors.id("block/plastic_strip");
    private static final ResourceLocation BEAD_TEXTURE = CurtainDoors.id("block/bead");
    private static final ResourceLocation STRING_TEXTURE = ResourceLocation.withDefaultNamespace("block/white_wool");
    private static final ResourceLocation WOOD_ROD_TEXTURE = ResourceLocation.withDefaultNamespace("block/stripped_oak_log");
    private static final ResourceLocation METAL_ROD_TEXTURE = ResourceLocation.withDefaultNamespace("block/iron_block");
    private static final float ROD_HALF = 0.03F;

    public CurtainRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CurtainBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof CurtainBlock block) || state.getValue(CurtainBlock.SEGMENT) != 0) {
            return;
        }
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        Direction.Axis axis = state.getValue(CurtainBlock.AXIS);
        int length = state.getValue(CurtainBlock.LENGTH);

        SimManager.Entry entry = SimManager.get(be);
        ClothSim sim = entry.sim();
        float[] p = entry.renderPositions;
        sim.interpolate(partialTick, p);

        // Curtain-local -> relative to this block entity (the top block).
        float yShift = -(length - 1);
        for (int k = 0; k < p.length; k += 3) {
            float x = p[k], z = p[k + 2];
            p[k] = CurtainSpace.worldX(axis, x, z);
            p[k + 1] += yShift;
            p[k + 2] = CurtainSpace.worldZ(axis, x, z);
        }

        // Light per block, bottom first.
        int[] lights = new int[length];
        BlockPos top = be.getBlockPos();
        for (int j = 0; j < length; j++) {
            lights[j] = j == length - 1 ? packedLight : LevelRenderer.getLightColor(level, top.below(length - 1 - j));
        }

        Function<ResourceLocation, TextureAtlasSprite> atlas = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
        PoseStack.Pose pose = poseStack.last();
        float fallbackNx = axis == Direction.Axis.X ? 0F : 1F;
        float fallbackNz = axis == Direction.Axis.X ? 1F : 0F;
        CurtainType type = block.getCurtainType();
        DyeColor color = block.getColor();

        switch (type) {
            case WOOL -> {
                TextureAtlasSprite wool = atlas.apply(ResourceLocation.withDefaultNamespace(
                        "block/" + (color == null ? DyeColor.WHITE : color).getName() + "_wool"));
                SheetMesher.render(buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)), pose,
                        sim.layout, p, lights, -yShift, wool, 0xFFFFFFFF, false, fallbackNx, fallbackNz);
            }
            case PLASTIC -> SheetMesher.render(buffers.getBuffer(RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS)),
                    pose, sim.layout, p, lights, -yShift, atlas.apply(PLASTIC_TEXTURE), 0xFFFFFFFF, true, fallbackNx, fallbackNz);
            case BEADED -> {
                int rgb = (color == null ? DyeColor.WHITE : color).getTextureDiffuseColor();
                BeadMesher.render(buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)), pose,
                        sim.layout, p, lights, -yShift, atlas.apply(BEAD_TEXTURE), atlas.apply(STRING_TEXTURE), 0xFF000000 | rgb);
            }
        }

        // Rod across the top.
        TextureAtlasSprite rod = atlas.apply(type == CurtainType.PLASTIC ? METAL_ROD_TEXTURE : WOOD_ROD_TEXTURE);
        float y0 = 1F - 2F * ROD_HALF, y1 = 1F;
        float x0, x1, z0, z1;
        if (axis == Direction.Axis.X) {
            x0 = 0F; x1 = 1F; z0 = 0.5F - ROD_HALF; z1 = 0.5F + ROD_HALF;
        } else {
            x0 = 0.5F - ROD_HALF; x1 = 0.5F + ROD_HALF; z0 = 0F; z1 = 1F;
        }
        MeshUtil.box(buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)), pose,
                x0, y0, z0, x1, y1, z1, rod, 0xFFFFFFFF, packedLight);
    }

    @Override
    public AABB getRenderBoundingBox(CurtainBlockEntity be) {
        return be.getCurtainBounds().inflate(0.75, 0.0, 0.75);
    }
}
