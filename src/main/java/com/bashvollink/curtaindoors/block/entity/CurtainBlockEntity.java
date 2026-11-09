package com.bashvollink.curtaindoors.block.entity;

import com.bashvollink.curtaindoors.block.CurtainBlock;
import com.bashvollink.curtaindoors.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Lives on the top segment of a curtain. It stores nothing on the server; on the client the
 * simulation manager keys its cloth state off this block entity.
 */
public class CurtainBlockEntity extends BlockEntity {
    public CurtainBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CURTAIN.get(), pos, state);
    }

    public int getLength() {
        BlockState state = getBlockState();
        return state.hasProperty(CurtainBlock.LENGTH) ? state.getValue(CurtainBlock.LENGTH) : 1;
    }

    /** World-space box covering every segment of this curtain. */
    public AABB getCurtainBounds() {
        BlockPos top = getBlockPos();
        int length = getLength();
        return new AABB(top.getX(), top.getY() - length + 1, top.getZ(), top.getX() + 1, top.getY() + 1, top.getZ() + 1);
    }
}
