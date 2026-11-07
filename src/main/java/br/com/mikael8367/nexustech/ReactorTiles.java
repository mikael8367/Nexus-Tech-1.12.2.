package br.com.mikael8367.nexustech;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

/**
 * Pure reactor math kept free of Minecraft world types so unit tests cover
 * heat, water, ice, snow, heat sinks and meltdown behavior without a runtime.
 */
final class ReactorMath {
    /** Physical structure the player assembles: 3x5x3 reactor blocks. */
    static final int STRUCT_WIDTH = 3;
    static final int STRUCT_HEIGHT = 5;
    static final int STRUCT_DEPTH = 3;
    static final int STRUCT_VOLUME = STRUCT_WIDTH * STRUCT_HEIGHT * STRUCT_DEPTH;
    static final int MAX_HEAT = 1000;
    static final int MAX_WATER_MB = 20000;
    static final int BURNING_HEAT = 2;
    static final int AMBIENT_COOLING = 1;
    static final int WATER_COOLING = 2;
    static final int WATER_USE_MB = 4;
    static final int ICE_COOLING = 1;
    static final int ICE_COOLING_CAP = 8;
    static final int SNOW_COOLING = 1;
    static final int SNOW_COOLING_CAP = 4;
    static final int SHELL_WATER_COOLING = 1;
    static final int SHELL_WATER_COOLING_CAP = 6;
    static final int SINK_COOLING = 2;
    static final int SINK_COOLING_CAP = 12;
    static final int ICE_MELT_THRESHOLD = 700;
    static final int SNOW_MELT_THRESHOLD = 400;
    static final int ICE_MELT_INTERVAL = 60;
    static final int ROD_TICKS = 12000; // 10 min of burn per rod
    static final int FE_PER_TICK = 4096;
    static final int MELTDOWN_TICKS = 1200; // 60 s countdown
    static final int MELTDOWN_CANCEL_HEAT = 400;
    static final int MELTDOWN_RUNAWAY = 1;
    static final int MELTDOWN_WATER_COOLING = 3;
    static final int MELTDOWN_WATER_USE_MB = 12;
    static final int MELTDOWN_ICE_COOLING = 2;
    static final int MELTDOWN_ICE_COOLING_CAP = 16;
    static final int MELTDOWN_SNOW_COOLING = 1;
    static final int MELTDOWN_SNOW_COOLING_CAP = 8;
    static final int MELTDOWN_SHELL_WATER_COOLING = 2;
    static final int MELTDOWN_SHELL_WATER_COOLING_CAP = 12;
    static final int MELTDOWN_SINK_COOLING = 3;
    static final int MELTDOWN_SINK_COOLING_CAP = 24;
    static final int EXPLOSION_COUNT = 100;
    static final float TNT_POWER = 4.0F;

    static int clamp(int value, int max) {
        return value < 0 ? 0 : (value > max ? max : value);
    }

    /** Net heat change per tick for the given reactor state. */
    static int heatDelta(boolean burning, boolean meltdown, boolean tankWater,
            int ice, int snow, int shellWater, int sinks) {
        int delta = meltdown ? MELTDOWN_RUNAWAY
                : (burning ? BURNING_HEAT : 0) - AMBIENT_COOLING;
        if (tankWater) delta -= meltdown ? MELTDOWN_WATER_COOLING : WATER_COOLING;
        delta -= Math.min(ice * (meltdown ? MELTDOWN_ICE_COOLING : ICE_COOLING),
                meltdown ? MELTDOWN_ICE_COOLING_CAP : ICE_COOLING_CAP);
        delta -= Math.min(snow * (meltdown ? MELTDOWN_SNOW_COOLING : SNOW_COOLING),
                meltdown ? MELTDOWN_SNOW_COOLING_CAP : SNOW_COOLING_CAP);
        delta -= Math.min(shellWater * (meltdown ? MELTDOWN_SHELL_WATER_COOLING : SHELL_WATER_COOLING),
                meltdown ? MELTDOWN_SHELL_WATER_COOLING_CAP : SHELL_WATER_COOLING_CAP);
        delta -= Math.min(sinks * (meltdown ? MELTDOWN_SINK_COOLING : SINK_COOLING),
                meltdown ? MELTDOWN_SINK_COOLING_CAP : SINK_COOLING_CAP);
        return delta;
    }

    static int waterUseMb(boolean meltdown) {
        return meltdown ? MELTDOWN_WATER_USE_MB : WATER_USE_MB;
    }

    /** Whether one perimeter ice block turns to water on this game tick. */
    static boolean iceMelts(int heat, long gameTime, boolean meltdown) {
        return heat >= (meltdown ? MELTDOWN_CANCEL_HEAT : ICE_MELT_THRESHOLD)
                && gameTime % (meltdown ? 20L : ICE_MELT_INTERVAL) == 0L;
    }

    /** Snow melts at a lower threshold than ice, and faster in a meltdown. */
    static boolean snowMelts(int heat, long gameTime, boolean meltdown) {
        return heat >= (meltdown ? MELTDOWN_CANCEL_HEAT : SNOW_MELT_THRESHOLD)
                && gameTime % (meltdown ? 20L : ICE_MELT_INTERVAL) == 0L;
    }

    /** During a meltdown the water pooled around the structure evaporates. */
    static boolean shellWaterEvaporates(boolean meltdown, long gameTime) {
        return meltdown && gameTime % 20L == 0L;
    }

    private ReactorMath() { }
}

/**
 * Nuclear reactor multiblock (3x5x3 of nuclear_reactor blocks). Only the
 * elected controller runs logic; the structure elects the lowest complete
 * corner exactly like the Digital Mine. Starts powered off and is toggled
 * with a sneak right-click, which also lights up the whole structure.
 * Burns uranium rods into a large FE buffer, heats up, cools with an
 * internal water tank plus ice, snow, water and heat sinks placed on the
 * perimeter, and melts down at max heat: alarm, particles, a 60 s
 * countdown, then a 100-TNT explosion. Cooling below 400 heat during the
 * countdown cancels the meltdown.
 */
final class TileNuclearReactor extends AbstractEnergyTile implements ITickable {
    private static final long STRUCTURE_CHECK_INTERVAL = 20L;
    private static final double ALARM_RANGE_SQ = 64.0D * 64.0D;

    private int heat;
    private int waterMb;
    private int fuelTicks;
    private int iceCount = -1;        // -1 = recount on demand
    private int snowCount = -1;
    private int shellWaterCount = -1;
    private int sinkCount = -1;
    private long meltdownTicks = -1L; // -1 = not in meltdown
    private boolean structureComplete;
    private boolean controllerTile;
    private boolean running; // starts OFF; toggled by sneak right-click
    private BlockPos controllerOrigin;
    private long lastStructureCheck;

    TileNuclearReactor() { super(5000000, 0, 4096); }

    int getHeat() { return heat; }
    int getWaterMb() { return waterMb; }
    int getFuelTicks() { return fuelTicks; }
    int getIceCount() { return Math.max(0, iceCount); }
    int getSnowCount() { return Math.max(0, snowCount); }
    int getShellWaterCount() { return Math.max(0, shellWaterCount); }
    int getSinkCount() { return Math.max(0, sinkCount); }
    boolean isMeltdownActive() { return meltdownTicks >= 0L; }
    int getMeltdownSeconds() { return Math.max(0, (int) (meltdownTicks / 20L)); }
    boolean isStructureComplete() { return structureComplete; }
    boolean isController() { return controllerTile; }
    boolean isRunning() { return running; }

    /** The elected controller of the assembled 3x5x3 structure, or null when absent. */
    TileNuclearReactor findController() {
        if (world == null || world.isRemote || controllerOrigin == null) return null;
        TileEntity tile = world.getTileEntity(controllerOrigin);
        return tile instanceof TileNuclearReactor ? (TileNuclearReactor) tile : null;
    }

    void receiveInternal(int amount) { if (amount > 0) energy.addInternal(amount); }

    /** Fills the tank from an external source (water bucket right-click). */
    int addWater(int milliBuckets) {
        if (world == null || world.isRemote || milliBuckets <= 0) return 0;
        int added = Math.min(milliBuckets, ReactorMath.MAX_WATER_MB - waterMb);
        if (added <= 0) return 0;
        waterMb += added;
        markDirty(); syncClient();
        return added;
    }

    /** Inserts one uranium rod worth of fuel; only when the current rod is spent. */
    boolean insertFuel() { return insertFuel(ReactorMath.ROD_TICKS); }

    /** Inserts a custom amount of fuel ticks; an ingot is worth 25% of a rod. */
    boolean insertFuel(int ticks) {
        if (world == null || world.isRemote || fuelTicks > 0 || ticks <= 0) return false;
        fuelTicks = Math.min(ReactorMath.ROD_TICKS, ticks);
        markDirty(); syncClient();
        return true;
    }

    /** Flips the on/off switch; called from the block's sneak right-click. */
    boolean toggleRunning() {
        if (world == null || world.isRemote) return running;
        running = !running;
        markDirty(); syncClient();
        return running;
    }

    /**
     * Elects the controller deterministically: among every complete 3x5x3 box of
     * reactor blocks that contains this block, the lowest corner wins and its
     * corner block is the controller. Mirrors the Digital Mine.
     */
    private void refreshStructure() {
        long now = world.getTotalWorldTime();
        if (now - lastStructureCheck >= STRUCTURE_CHECK_INTERVAL) {
            lastStructureCheck = now;
            invalidateShellCounts();
            BlockPos best = null;
            for (int ox = 0; ox < ReactorMath.STRUCT_WIDTH; ox++) {
                for (int oy = 0; oy < ReactorMath.STRUCT_HEIGHT; oy++) {
                    for (int oz = 0; oz < ReactorMath.STRUCT_DEPTH; oz++) {
                        BlockPos origin = pos.add(-ox, -oy, -oz);
                        if (!isCompleteStructure(origin)) continue;
                        if (best == null || origin.compareTo(best) < 0) best = origin;
                    }
                }
            }
            controllerOrigin = best;
            structureComplete = best != null;
            controllerTile = best != null && best.equals(pos);
        }
    }

    private boolean isCompleteStructure(BlockPos origin) {
        for (int dx = 0; dx < ReactorMath.STRUCT_WIDTH; dx++) {
            for (int dy = 0; dy < ReactorMath.STRUCT_HEIGHT; dy++) {
                for (int dz = 0; dz < ReactorMath.STRUCT_DEPTH; dz++) {
                    if (world.getBlockState(origin.add(dx, dy, dz)).getBlock() != TechRegistry.NUCLEAR_REACTOR) return false;
                }
            }
        }
        return true;
    }

    private void invalidateShellCounts() {
        iceCount = -1;
        snowCount = -1;
        shellWaterCount = -1;
        sinkCount = -1;
    }

    /** One pass over the shell counts ice, snow, pooled water and heat sinks. */
    private void countShell() {
        if (iceCount >= 0) return;
        int ice = 0, snow = 0, shellWater = 0, sinks = 0;
        for (int dx = -1; dx <= ReactorMath.STRUCT_WIDTH; dx++) {
            for (int dy = -1; dy <= ReactorMath.STRUCT_HEIGHT; dy++) {
                for (int dz = -1; dz <= ReactorMath.STRUCT_DEPTH; dz++) {
                    boolean shell = dx < 0 || dx >= ReactorMath.STRUCT_WIDTH
                            || dy < 0 || dy >= ReactorMath.STRUCT_HEIGHT
                            || dz < 0 || dz >= ReactorMath.STRUCT_DEPTH;
                    if (!shell) continue;
                    IBlockState state = world.getBlockState(pos.add(dx, dy, dz));
                    net.minecraft.block.Block block = state.getBlock();
                    if (block == Blocks.ICE || block == Blocks.PACKED_ICE) ice++;
                    else if (block == Blocks.SNOW || block == Blocks.SNOW_LAYER) snow++;
                    else if (block == Blocks.WATER) shellWater++;
                    else if (block == TechRegistry.HEAT_SINK) sinks++;
                }
            }
        }
        iceCount = ice;
        snowCount = snow;
        shellWaterCount = shellWater;
        sinkCount = sinks;
    }

    /** Turns the first perimeter ice block into water. */
    private void meltOneIce() {
        if (replaceShellBlock(Blocks.ICE, Blocks.PACKED_ICE, Blocks.WATER.getDefaultState())) invalidateShellCounts();
    }

    /** Turns the first perimeter snow block or layer into water. */
    private void meltOneSnow() {
        if (replaceShellBlock(Blocks.SNOW, Blocks.SNOW_LAYER, Blocks.WATER.getDefaultState())) invalidateShellCounts();
    }

    /** Evaporates the first water block pooled around the structure. */
    private void evaporateOneWater() {
        if (replaceShellBlock(Blocks.WATER, Blocks.WATER, Blocks.AIR.getDefaultState())) invalidateShellCounts();
    }

    private boolean replaceShellBlock(net.minecraft.block.Block first, net.minecraft.block.Block second, IBlockState to) {
        for (int dx = -1; dx <= ReactorMath.STRUCT_WIDTH; dx++) {
            for (int dy = -1; dy <= ReactorMath.STRUCT_HEIGHT; dy++) {
                for (int dz = -1; dz <= ReactorMath.STRUCT_DEPTH; dz++) {
                    boolean shell = dx < 0 || dx >= ReactorMath.STRUCT_WIDTH
                            || dy < 0 || dy >= ReactorMath.STRUCT_HEIGHT
                            || dz < 0 || dz >= ReactorMath.STRUCT_DEPTH;
                    if (!shell) continue;
                    BlockPos target = pos.add(dx, dy, dz);
                    net.minecraft.block.Block block = world.getBlockState(target).getBlock();
                    if (block == first || block == second) {
                        world.setBlockState(target, to, 3);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override public void update() {
        if (world == null || world.isRemote) return;
        refreshStructure();
        if (!controllerTile) {
            // Energy that somehow entered a non-controller block is forwarded so nothing is stranded.
            if (structureComplete && energy.getEnergyStored() > 0) {
                TileNuclearReactor controller = findController();
                if (controller != null) controller.receiveInternal(energy.spendInternal(energy.getEnergyStored()));
            }
            return;
        }
        long now = world.getTotalWorldTime();
        boolean meltdown = meltdownTicks >= 0L;
        boolean burning = running && !meltdown && fuelTicks > 0;
        if (burning && energy.getCapacity() - energy.getEnergyStored() >= ReactorMath.FE_PER_TICK) {
            energy.addInternal(ReactorMath.FE_PER_TICK);
            fuelTicks--;
        }
        EnergyTransfer.push(this, energy, ReactorMath.FE_PER_TICK);
        countShell();
        boolean tankWater = waterMb > 0;
        heat = ReactorMath.clamp(heat + ReactorMath.heatDelta(burning, meltdown, tankWater,
                iceCount, snowCount, shellWaterCount, sinkCount), ReactorMath.MAX_HEAT);
        if (tankWater && heat > 0) waterMb = Math.max(0, waterMb - ReactorMath.waterUseMb(meltdown));
        if (ReactorMath.iceMelts(heat, now, meltdown)) meltOneIce();
        else if (ReactorMath.snowMelts(heat, now, meltdown)) meltOneSnow();
        if (ReactorMath.shellWaterEvaporates(meltdown, now)) evaporateOneWater();
        if (!meltdown && heat >= ReactorMath.MAX_HEAT) {
            meltdownTicks = ReactorMath.MELTDOWN_TICKS;
            meltdown = true;
        }
        if (meltdown) {
            meltdownAlarm(now);
            if (heat < ReactorMath.MELTDOWN_CANCEL_HEAT) {
                meltdownTicks = -1L; // cooled down in time: accident averted
            } else if (--meltdownTicks < 0L) {
                explode();
                return;
            }
        }
        if (now % (meltdown ? 2L : 10L) == 0L) { markDirty(); syncClient(); }
    }

    /** Rising-pitch alarm, smoke and embers on the structure, actionbar countdown. */
    private void meltdownAlarm(long now) {
        if (now % 20L == 0L) {
            float progress = 1.0F - (float) meltdownTicks / (float) ReactorMath.MELTDOWN_TICKS;
            world.playSound(null, pos, SoundEvents.BLOCK_NOTE_PLING, SoundCategory.BLOCKS,
                    1.0F, 0.6F + 1.4F * Math.max(0.0F, Math.min(1.0F, progress)));
            String message = "Reator: EXPLOSAO EM " + getMeltdownSeconds() + "s";
            for (EntityPlayer player : world.playerEntities) {
                if (player instanceof EntityPlayerMP && player.getDistanceSq(pos) <= ALARM_RANGE_SQ) {
                    ((EntityPlayerMP) player).sendStatusMessage(new TextComponentString(message), true);
                }
            }
        }
        if (now % 2L == 0L) {
            for (int i = 0; i < 4; i++) {
                int index = world.rand.nextInt(ReactorMath.STRUCT_VOLUME);
                int dx = index % ReactorMath.STRUCT_WIDTH;
                int dy = (index / ReactorMath.STRUCT_WIDTH) % ReactorMath.STRUCT_HEIGHT;
                int dz = index / (ReactorMath.STRUCT_WIDTH * ReactorMath.STRUCT_HEIGHT);
                BlockPos target = pos.add(dx, dy, dz);
                world.spawnParticle(EnumParticleTypes.SMOKE_LARGE,
                        target.getX() + world.rand.nextDouble(), target.getY() + 1.0D,
                        target.getZ() + world.rand.nextDouble(), 0.0D, 0.1D, 0.0D);
                if (world.rand.nextInt(4) == 0) {
                    world.spawnParticle(EnumParticleTypes.LAVA,
                            target.getX() + 0.5D, target.getY() + 1.0D, target.getZ() + 0.5D,
                            0.0D, 0.0D, 0.0D);
                }
            }
        }
    }

    /** One hundred TNT-strength blasts scattered across the structure area. */
    private void explode() {
        for (int i = 0; i < ReactorMath.EXPLOSION_COUNT; i++) {
            double x = pos.getX() + world.rand.nextDouble() * 11.0D - 5.0D;
            double y = pos.getY() + world.rand.nextDouble() * 11.0D - 5.0D;
            double z = pos.getZ() + world.rand.nextDouble() * 11.0D - 5.0D;
            world.createExplosion(null, x, y, z, ReactorMath.TNT_POWER, true);
        }
    }

    @Override public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> cap, net.minecraft.util.EnumFacing side) {
        if (cap == net.minecraftforge.energy.CapabilityEnergy.ENERGY) {
            // The whole assembled structure acts as one single energy provider,
            // so cables touching any reactor block can extract FE.
            if (!controllerTile && structureComplete) {
                TileNuclearReactor controller = findController();
                if (controller != null) return controller.hasCapability(cap, side);
            }
            return controllerTile && super.hasCapability(cap, side);
        }
        return super.hasCapability(cap, side);
    }

    @Override @SuppressWarnings("unchecked")
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, net.minecraft.util.EnumFacing side) {
        if (cap == net.minecraftforge.energy.CapabilityEnergy.ENERGY && !controllerTile && structureComplete) {
            TileNuclearReactor controller = findController();
            if (controller != null && controller.controllerTile) return controller.getCapability(cap, side);
        }
        return super.getCapability(cap, side);
    }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setInteger("ReactorHeat", heat);
        tag.setInteger("ReactorWater", waterMb);
        tag.setInteger("ReactorFuel", fuelTicks);
        tag.setInteger("ReactorIce", iceCount);
        tag.setInteger("ReactorSnow", snowCount);
        tag.setInteger("ReactorShellWater", shellWaterCount);
        tag.setInteger("ReactorSinks", sinkCount);
        tag.setBoolean("ReactorRunning", running);
        tag.setBoolean("ReactorStruct", structureComplete);
        tag.setBoolean("ReactorCtrl", controllerTile);
        if (meltdownTicks >= 0L) tag.setLong("ReactorMeltdown", meltdownTicks);
        return super.writeToNBT(tag);
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        heat = ReactorMath.clamp(tag.getInteger("ReactorHeat"), ReactorMath.MAX_HEAT);
        waterMb = Math.max(0, Math.min(ReactorMath.MAX_WATER_MB, tag.getInteger("ReactorWater")));
        fuelTicks = Math.max(0, Math.min(ReactorMath.ROD_TICKS, tag.getInteger("ReactorFuel")));
        iceCount = Math.max(-1, tag.getInteger("ReactorIce"));
        snowCount = Math.max(-1, tag.getInteger("ReactorSnow"));
        shellWaterCount = Math.max(-1, tag.getInteger("ReactorShellWater"));
        sinkCount = Math.max(-1, tag.getInteger("ReactorSinks"));
        running = tag.getBoolean("ReactorRunning");
        structureComplete = tag.getBoolean("ReactorStruct");
        controllerTile = tag.getBoolean("ReactorCtrl");
        meltdownTicks = tag.hasKey("ReactorMeltdown") ? Math.max(-1L, tag.getLong("ReactorMeltdown")) : -1L;
    }
}
