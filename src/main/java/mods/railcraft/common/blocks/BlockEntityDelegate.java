package mods.railcraft.common.blocks;

import mods.railcraft.api.core.IPostConnection;
import mods.railcraft.common.blocks.machine.interfaces.*;
import mods.railcraft.common.plugins.color.ColorPlugin;
import mods.railcraft.common.plugins.color.EnumColor;
import mods.railcraft.common.plugins.forge.PowerPlugin;
import mods.railcraft.common.plugins.forge.WorldPlugin;
import mods.railcraft.common.util.misc.Game;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Level;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 *
 */
public abstract class BlockEntityDelegate extends BlockContainerRailcraft implements ColorPlugin.IColoredBlock {

    protected BlockEntityDelegate(Material materialIn) {
        super(materialIn);
    }

    protected BlockEntityDelegate(Material material, MapColor mapColor) {
        super(material, mapColor);
    }

    public abstract Class<? extends TileEntity> getTileClass(IBlockState state);

    @Override
    public void finalizeDefinition() {
        ColorPlugin.instance.register(this, this);
    }

    @Override
    public IBlockColor colorHandler() {
        return (state, worldIn, pos, tintIndex) -> {
            //TODO: this probably not entirely correct, may need to handle this differently if world/pos null
            if (worldIn != null && pos != null) {
                WorldPlugin.getTileEntity(worldIn, pos, ISmartTile.class).ifPresent(ISmartTile::colorMultiplier);
            }
            return EnumColor.WHITE.getHexColor();
        };
    }

    @Override
    public boolean recolorBlock(World world, BlockPos pos, EnumFacing side, EnumDyeColor color) {
        return WorldPlugin.getTileEntity(world, pos, ISmartTile.class).map(t -> t.recolourBlock(color)).orElse(false);
    }

    @Override
    public int damageDropped(IBlockState state) {
        return getMetaFromState(state);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (hand == EnumHand.OFF_HAND)
            return false;
        return WorldPlugin.getTileEntity(worldIn, pos, ISmartTile.class).map(t -> t.blockActivated(playerIn, hand, heldItem, side, hitX, hitY, hitZ)).orElse(false);
    }

    @Override
    public boolean rotateBlock(World world, BlockPos pos, EnumFacing axis) {
        return WorldPlugin.getTileEntity(world, pos, ITileRotate.class).map(t -> t.rotateBlock(axis)).orElse(false);
    }

    @Override
    public EnumFacing[] getValidRotations(World world, BlockPos pos) {
        return WorldPlugin.getTileEntity(world, pos, ITileRotate.class).map(ITileRotate::getValidRotations).orElseGet(() -> new EnumFacing[]{});
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World worldIn, BlockPos pos, Random rand) {
        WorldPlugin.getTileEntity(worldIn, pos, ISmartTile.class).ifPresent(t -> t.randomDisplayTick(rand));
    }

    @Override
    public boolean isSideSolid(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return WorldPlugin.retrieveFromTile(world, pos, ITileNonSolid.class, t -> t.isSideSolid(side)).orElse(true);
    }

    @Override
    public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        return WorldPlugin.retrieveFromTile(world, pos, ISmartTile.class, t -> t.getDrops(fortune)).orElse(Collections.emptyList());
    }

    public List<ItemStack> getBlockDroppedSilkTouch(World world, BlockPos pos, IBlockState state, int fortune) {
        return WorldPlugin.getTileEntity(world, pos, ISmartTile.class).map(t -> t.getBlockDroppedSilkTouch(fortune)).orElse(Collections.emptyList());
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return WorldPlugin.getTileEntity(world, pos, ISmartTile.class).map(t -> t.canSilkHarvest(player)).orElse(false);
    }

    @Override
    @Nullable
    @SuppressWarnings("deprecation")
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        List<ItemStack> drops = getBlockDroppedSilkTouch(world, pos, world.getBlockState(pos), 0);
        if (drops.isEmpty())
            return getItem(world, pos, state);
        return drops.get(0);
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return true;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        return WorldPlugin.getTileEntity(worldIn, pos, ITileRedstoneEmitter.class).map(t -> t.getPowerOutput(side)).orElse(PowerPlugin.NO_POWER);
    }

    @Override
    public boolean canConnectRedstone(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return WorldPlugin.getTileEntity(world, pos, ISmartTile.class).map(t -> t.canConnectRedstone(side)).orElse(false);
    }

    public void initFromItem(World world, BlockPos pos, ItemStack stack) {
        WorldPlugin.getTileEntity(world, pos, ISmartTile.class).ifPresent(t -> t.initFromItem(stack));
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        WorldPlugin.getTileEntity(worldIn, pos, ISmartTile.class).ifPresent(t -> t.onBlockPlacedBy(state, placer, stack));
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block neighborBlock) {
        if (needsSupport() && !worldIn.isSideSolid(pos.down(), EnumFacing.UP)) {
            WorldPlugin.destroyBlock(worldIn, pos, true);
            return;
        }

        try {
            WorldPlugin.getTileEntity(worldIn, pos, ISmartTile.class).ifPresent(t -> t.onNeighborBlockChange(state, neighborBlock));
        } catch (StackOverflowError error) {
            Game.logThrowable(Level.ERROR, 10, error, "Stack Overflow Error in BlockMachine.onNeighborBlockChange()");
            if (Game.DEVELOPMENT_ENVIRONMENT)
                throw error;
        }
    }

    public boolean needsSupport() {
        return false;
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        WorldPlugin.getTileEntity(worldIn, pos, ISmartTile.class).ifPresent(ISmartTile::onBlockAdded);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        WorldPlugin.getTileEntity(worldIn, pos, ISmartTile.class).ifPresent(ISmartTile::onBlockRemoval);
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return WorldPlugin.retrieveFromTile(world, pos, ITileShaped.class, t -> t.getBoundingBox(world, pos)).orElse(Block.FULL_BLOCK_AABB);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, World world, BlockPos pos) {
        return WorldPlugin.retrieveFromTile(world, pos, ITileShaped.class, t -> t.getCollisionBoundingBox(world, pos)).orElse(Block.FULL_BLOCK_AABB);
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World world, BlockPos pos) {
        return WorldPlugin.retrieveFromTile(world, pos, ITileShaped.class, t -> t.getSelectedBoundingBox(world, pos)).orElse(Block.FULL_BLOCK_AABB);
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        if (pos.getY() < 0)
            return 0;
        return WorldPlugin.retrieveFromTile(world, pos, ITileLit.class, ITileLit::getLightValue).orElse(0);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public float getExplosionResistance(World world, BlockPos pos, Entity exploder, Explosion explosion) {
        return WorldPlugin.getTileEntity(world, pos, ISmartTile.class).map(t -> t.getResistance(exploder) * 3f / 5f).orElse(0f);
    }

    @Override
    public boolean canBeReplacedByLeaves(IBlockState state, IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public boolean canCreatureSpawn(IBlockState state, IBlockAccess world, BlockPos pos, EntityLiving.SpawnPlacementType type) {
        return WorldPlugin.getTileEntity(world, pos, ISmartTile.class).map(t -> t.canCreatureSpawn(type)).orElse(false);
    }

    @Override
    public float getBlockHardness(IBlockState state, World worldIn, BlockPos pos) {
        return WorldPlugin.getTileEntity(worldIn, pos, ISmartTile.class).map(ISmartTile::getHardness).orElse(0F);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return TileManager.isInstance(this::getTileClass, ITileCompare.class, state);
    }

    /**
     * Value is provided by the tile entity
     */

    @Override
    public int getComparatorInputOverride(IBlockState state, World worldIn, BlockPos pos) {
        return WorldPlugin.retrieveFromTile(worldIn, pos, ITileCompare.class, ITileCompare::getComparatorInputOverride).orElse(0);
    }

    public IPostConnection.ConnectStyle connectsToPost(IBlockAccess world, BlockPos pos, IBlockState state, EnumFacing face) {
        return WorldPlugin.getTileEntity(world, pos, ISmartTile.class).map(t -> t.connectsToPost(face)).orElse(IPostConnection.ConnectStyle.NONE);
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return WorldPlugin.getTileEntity(worldIn, pos, ISmartTile.class).map(t -> t.getActualState(state)).orElse(state);
    }
}
