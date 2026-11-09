package com.bashvollink.curtaindoors.block;

import com.bashvollink.curtaindoors.block.entity.CurtainBlockEntity;
import com.bashvollink.curtaindoors.config.CommonConfig;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A curtain that hangs from a ceiling and extends 1-4 blocks down. Every segment has no collision,
 * so anything can walk through. Segment 0 (the top) carries the block entity that the client uses
 * to simulate and draw the cloth for the whole curtain.
 */
public class CurtainBlock extends Block implements EntityBlock {
    public static final int MAX_LENGTH = 4;
    /** The axis the curtain spans; the curtain is thin along the other horizontal axis. */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    /** 0 = top segment. */
    public static final IntegerProperty SEGMENT = IntegerProperty.create("segment", 0, MAX_LENGTH - 1);
    public static final IntegerProperty LENGTH = IntegerProperty.create("length", 1, MAX_LENGTH);

    private static final VoxelShape SHAPE_X = Block.box(0, 0, 7, 16, 16, 9);
    private static final VoxelShape SHAPE_Z = Block.box(7, 0, 0, 9, 16, 16);

    private final CurtainType type;
    @Nullable
    private final DyeColor color;

    public CurtainBlock(CurtainType type, @Nullable DyeColor color, Properties properties) {
        super(properties);
        this.type = type;
        this.color = color;
        registerDefaultState(stateDefinition.any()
                .setValue(AXIS, Direction.Axis.X)
                .setValue(SEGMENT, 0)
                .setValue(LENGTH, 1));
    }

    public CurtainType getCurtainType() {
        return type;
    }

    @Nullable
    public DyeColor getColor() {
        return color;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, SEGMENT, LENGTH);
    }

    // ---------------------------------------------------------------- placement

    /** A curtain can hang from anything a lantern can hang from. */
    public static boolean canHangBelow(LevelReader level, BlockPos topSegmentPos) {
        return Block.canSupportCenter(level, topSegmentPos.above(), Direction.DOWN);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level level = ctx.getLevel();
        BlockPos clicked = ctx.getClickedPos();
        int max = Math.min(MAX_LENGTH, CommonConfig.maxLength());

        // Find the ceiling: scan up through replaceable space from the clicked position.
        BlockPos top = null;
        for (int up = 0; up < max; up++) {
            BlockPos q = clicked.above(up);
            if (q.getY() >= level.getMaxBuildHeight() - 1) {
                break;
            }
            if (up > 0 && !level.getBlockState(q).canBeReplaced(ctx)) {
                break;
            }
            if (canHangBelow(level, q)) {
                top = q;
                break;
            }
        }
        if (top == null) {
            return null;
        }

        // Hang down through replaceable space until the floor or the max length.
        int length = 1;
        while (length < max) {
            BlockPos q = top.below(length);
            if (q.getY() < level.getMinBuildHeight()) {
                break;
            }
            BlockState below = level.getBlockState(q);
            if (!q.equals(clicked) && !below.canBeReplaced(ctx)) {
                break;
            }
            length++;
        }

        int segment = top.getY() - clicked.getY();
        return defaultBlockState()
                .setValue(AXIS, ctx.getHorizontalDirection().getClockWise().getAxis())
                .setValue(SEGMENT, segment)
                .setValue(LENGTH, length);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.isClientSide) {
            return;
        }
        int segment = state.getValue(SEGMENT);
        int length = state.getValue(LENGTH);
        BlockPos top = pos.above(segment);
        // Top-down, so every new segment finds a valid segment above it.
        for (int i = 0; i < length; i++) {
            if (i != segment) {
                level.setBlock(top.below(i), state.setValue(SEGMENT, i), Block.UPDATE_ALL);
            }
        }
    }

    // ---------------------------------------------------------------- integrity

    private boolean isPartOf(BlockState other, BlockState self, int expectedSegment) {
        return other.is(this)
                && other.getValue(SEGMENT) == expectedSegment
                && other.getValue(LENGTH).equals(self.getValue(LENGTH))
                && other.getValue(AXIS) == self.getValue(AXIS);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        int segment = state.getValue(SEGMENT);
        if (segment == 0) {
            return canHangBelow(level, pos);
        }
        // During placement the segments above don't exist yet; runtime integrity is in updateShape.
        BlockState above = level.getBlockState(pos.above());
        return isPartOf(above, state, segment - 1) || above.canBeReplaced();
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        int segment = state.getValue(SEGMENT);
        int length = state.getValue(LENGTH);
        if (direction == Direction.UP) {
            boolean ok = segment == 0 ? canHangBelow(level, pos) : isPartOf(neighborState, state, segment - 1);
            if (!ok) {
                return Blocks.AIR.defaultBlockState();
            }
        } else if (direction == Direction.DOWN && segment < length - 1 && !isPartOf(neighborState, state, segment + 1)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Only the top segment drops the item. In creative, remove the top silently so breaking a
        // lower segment doesn't drop anything.
        int segment = state.getValue(SEGMENT);
        if (!level.isClientSide && segment > 0 && (player.isCreative() || !player.hasCorrectToolForDrops(state, level, pos))) {
            BlockPos top = pos.above(segment);
            BlockState topState = level.getBlockState(top);
            if (isPartOf(topState, state, 0)) {
                level.setBlock(top, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, 2001, top, Block.getId(topState));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // ---------------------------------------------------------------- shape, pathing, light

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return true;
    }

    @Override
    public PathType getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob) {
        return PathType.OPEN;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        // The block entity renderer on the top segment draws the whole curtain.
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return type == CurtainType.WOOL ? 60 : 0;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return type == CurtainType.WOOL ? 30 : 0;
    }

    // ---------------------------------------------------------------- block entity

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(SEGMENT) == 0 ? new CurtainBlockEntity(pos, state) : null;
    }
}
