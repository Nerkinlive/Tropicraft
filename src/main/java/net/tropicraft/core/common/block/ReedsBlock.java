package net.tropicraft.core.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.Tags;

import java.util.Random;

public final class ReedsBlock extends Block implements SimpleWaterloggedBlock, IPlantable {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<Type> TYPE = EnumProperty.create("type", Type.class);

    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

    public ReedsBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.getStateDefinition().any().setValue(TYPE, Type.SINGLE).setValue(WATERLOGGED, false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, Random rand) {
        if (!state.canSurvive(world, pos)) {
            world.destroyBlock(pos, true);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level world = context.getLevel();
        FluidState fluid = world.getFluidState(context.getClickedPos());
        return this.defaultBlockState()
                .setValue(TYPE, this.getAppropriateTypeAt(world, context.getClickedPos()))
                .setValue(WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos currentPos, BlockPos facingPos) {
        if (!state.canSurvive(world, currentPos)) {
            return Blocks.AIR.defaultBlockState();
        }

        if (state.getValue(WATERLOGGED)) {
            world.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
        }

        return state.setValue(TYPE, this.getAppropriateTypeAt(world, currentPos));
    }

    private Type getAppropriateTypeAt(LevelAccessor world, BlockPos pos) {
        if (world.getBlockState(pos.above()).is(this)) {
            return Type.BOTTOM;
        }
        return world.getBlockState(pos.below()).is(this) ? Type.TOP : Type.SINGLE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        BlockPos groundPos = pos.below();
        BlockState growOn = world.getBlockState(groundPos);
        if (growOn.canSustainPlant(world, groundPos, Direction.UP, this)) {
            return true;
        }

        return growOn.getBlock() == this || this.canGrowOn(growOn);
    }

    private boolean canGrowOn(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(BlockTags.SAND) || state.is(BlockTags.DIRT) || state.is(Tags.Blocks.GRAVEL)
                || state.is(Blocks.CLAY);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, TYPE);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getPlant(BlockGetter world, BlockPos pos) {
        return this.defaultBlockState();
    }

    public enum Type implements StringRepresentable {
        SINGLE("single", "reeds_top_short"),
        BOTTOM("bottom", "reeds_bottom"),
        TOP("top", "reeds_top_tall", "reeds_top_short");

        private final String key;
        private final String[] textures;

        Type(String key, String... textures) {
            this.key = key;
            this.textures = textures;
        }

        public String[] getTextures() {
            return this.textures;
        }

        @Override
        public String getSerializedName() {
            return this.key;
        }
    }
}
