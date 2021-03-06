package de.maxhenkel.gravestone.blocks;

import de.maxhenkel.corelib.block.DirectionalVoxelShape;
import de.maxhenkel.corelib.block.IItemBlock;
import de.maxhenkel.gravestone.Main;
import de.maxhenkel.gravestone.tileentity.GraveStoneTileEntity;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.block.material.PushReaction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.*;

import javax.annotation.Nullable;

public class GraveStoneBlock extends Block implements ITileEntityProvider, IItemBlock, IBucketPickupHandler, ILiquidContainer {

    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final Material GRAVESTONE_MATERIAL = new Material(MaterialColor.DIRT, false, false, true, false, false, false, PushReaction.BLOCK);

    public GraveStoneBlock() {
        super(Properties.create(GRAVESTONE_MATERIAL, MaterialColor.DIRT).hardnessAndResistance(0.3F, Float.MAX_VALUE));
        setDefaultState(this.stateContainer.getBaseState().with(FACING, Direction.NORTH).with(WATERLOGGED, false));
        setRegistryName(Main.MODID, "gravestone");
    }

    @Override
    public Item toItem() {
        return new BlockItem(this, new Item.Properties().group(ItemGroup.DECORATIONS)).setRegistryName(getRegistryName());
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(WATERLOGGED);
    }

    public Fluid pickupFluid(IWorld worldIn, BlockPos pos, BlockState state) {
        if (state.get(WATERLOGGED)) {
            worldIn.setBlockState(pos, state.with(WATERLOGGED, false), 3);
            return Fluids.WATER;
        } else {
            return Fluids.EMPTY;
        }
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStillFluidState(false) : super.getFluidState(state);
    }

    @Override
    public boolean canContainFluid(IBlockReader worldIn, BlockPos pos, BlockState state, Fluid fluidIn) {
        return !state.get(WATERLOGGED) && fluidIn == Fluids.WATER;
    }

    @Override
    public boolean receiveFluid(IWorld worldIn, BlockPos pos, BlockState state, FluidState fluidStateIn) {
        if (!state.get(WATERLOGGED) && fluidStateIn.getFluid() == Fluids.WATER) {
            if (!worldIn.isRemote()) {
                worldIn.setBlockState(pos, state.with(WATERLOGGED, true), 3);
                worldIn.getPendingFluidTicks().scheduleTick(pos, fluidStateIn.getFluid(), fluidStateIn.getFluid().getTickRate(worldIn));
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public BlockState updatePostPlacement(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (stateIn.get(WATERLOGGED)) {
            worldIn.getPendingFluidTicks().scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickRate(worldIn));
        }

        return super.updatePostPlacement(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    @Override
    public void onExplosionDestroy(World p_180652_1_, BlockPos p_180652_2_, Explosion p_180652_3_) {

    }

    @Override
    public void onBlockExploded(BlockState state, World world, BlockPos pos, Explosion explosion) {

    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        FluidState ifluidstate = context.getWorld().getFluidState(context.getPos());
        return this.getDefaultState().with(FACING, context.getPlacementHorizontalFacing()).with(WATERLOGGED, ifluidstate.getFluid() == Fluids.WATER);
    }

    @Override
    public boolean isTransparent(BlockState state) {
        return false;
    }

    @Override
    public boolean canEntityDestroy(BlockState state, IBlockReader world, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public BlockRenderType getRenderType(BlockState p_149645_1_) {
        return BlockRenderType.MODEL;
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult result) {
        TileEntity tileentity = world.getTileEntity(pos);

        if (!(tileentity instanceof GraveStoneTileEntity)) {
            return ActionResultType.FAIL;
        }

        GraveStoneTileEntity grave = (GraveStoneTileEntity) tileentity;

        String name = grave.getPlayerName();
        String time = grave.getTimeString();

        if (name == null || name.isEmpty()) {
            return ActionResultType.FAIL;
        }

        if (!world.isRemote) {
            if (time == null || time.isEmpty()) {
                player.sendMessage(new StringTextComponent(name), Util.field_240973_b_);
            } else {
                player.sendMessage(new TranslationTextComponent("message.gravestone.died", name, time), player.getUniqueID());
            }
        }

        return ActionResultType.SUCCESS;
    }

    @Override
    public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            TileEntity tileentity = worldIn.getTileEntity(pos);
            if (tileentity instanceof GraveStoneTileEntity) {
                InventoryHelper.dropInventoryItems(worldIn, pos, ((GraveStoneTileEntity) tileentity).getInventory());
                worldIn.updateComparatorOutputLevel(pos, this);
            }
            super.onReplaced(state, worldIn, pos, newState, isMoving);
        }
    }

    @Override
    public boolean isReplaceable(BlockState p_196253_1_, BlockItemUseContext p_196253_2_) {
        return false;
    }

    @Override
    public boolean canBeReplacedByLeaves(BlockState state, IWorldReader world, BlockPos pos) {
        return false;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
        return SHAPE.get(state.get(FACING));
    }

    @Override
    public VoxelShape getRenderShape(BlockState state, IBlockReader reader, BlockPos pos) {
        return SHAPE.get(state.get(FACING));
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
        return SHAPE.get(state.get(FACING));
    }

    @Override
    public VoxelShape getRaytraceShape(BlockState state, IBlockReader reader, BlockPos pos) {
        return SHAPE.get(state.get(FACING));
    }

    private static final VoxelShape BASE1 = Block.makeCuboidShape(0D, 0D, 0D, 16D, 1D, 16D);
    private static final VoxelShape BASE2 = Block.makeCuboidShape(1D, 1D, 1D, 15D, 2D, 15D);

    private static final DirectionalVoxelShape SHAPE = new DirectionalVoxelShape.Builder()
            .direction(Direction.NORTH,
                    BASE1,
                    BASE2,
                    Block.makeCuboidShape(1D, 2D, 1D, 15D, 12D, 2D),
                    Block.makeCuboidShape(2D, 12D, 1D, 14D, 14D, 2D),
                    Block.makeCuboidShape(3D, 14D, 1D, 13D, 15D, 2D))
            .direction(Direction.SOUTH,
                    BASE1,
                    BASE2,
                    Block.makeCuboidShape(1D, 2D, 15D, 15D, 12D, 14D),
                    Block.makeCuboidShape(2D, 12D, 15D, 14D, 14D, 14D),
                    Block.makeCuboidShape(3D, 14D, 15D, 13D, 15D, 14D)
            ).direction(Direction.EAST,
                    BASE1,
                    BASE2,
                    Block.makeCuboidShape(15D, 2D, 1D, 14D, 12D, 15D),
                    Block.makeCuboidShape(15D, 12D, 2D, 14D, 14D, 14D),
                    Block.makeCuboidShape(15D, 14D, 3D, 14D, 15D, 13D)
            ).direction(Direction.WEST,
                    BASE1,
                    BASE2,
                    Block.makeCuboidShape(1D, 2D, 1D, 2D, 12D, 15D),
                    Block.makeCuboidShape(1D, 12D, 2D, 2D, 14D, 14D),
                    Block.makeCuboidShape(1D, 14D, 3D, 2D, 15D, 13D)
            ).build();

    @Override
    public TileEntity createNewTileEntity(IBlockReader reader) {
        return new GraveStoneTileEntity();
    }

}
