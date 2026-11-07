package br.com.mikael8367.nexustech;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.oredict.OreDictionary;

final class TileCoalGenerator extends AbstractEnergyTile implements ITickable {
    static final int MAX_FUEL_TICKS = 32000;
    private int fuelTicks;
    TileCoalGenerator() { super(32000, 0, 40); }
    int getFuelTicks() { return fuelTicks; }
    boolean addFuel(ItemStack fuel) {
        if (world == null || world.isRemote || !BlockCoalGenerator.isFuel(fuel)) return false;
        int ticks = fuel.getItem() == net.minecraft.init.Items.COAL ? 1600
                : fuel.getItem() == net.minecraft.item.Item.getItemFromBlock(net.minecraft.init.Blocks.COAL_BLOCK) ? 16000 : 0;
        if (ticks == 0 || ticks > MAX_FUEL_TICKS - fuelTicks) return false;
        fuelTicks += ticks;
        markDirty();
        return true;
    }
    @Override public void update() {
        if (world == null || world.isRemote) return;
        if (fuelTicks > 0 && energy.getCapacity() - energy.getEnergyStored() >= 10) {
            energy.addInternal(10);
            fuelTicks--;
        }
        EnergyTransfer.push(this, energy, 40);
        if (world.getTotalWorldTime() % 10L == 0L) { markDirty(); syncClient(); }
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setInteger("FuelTicks", fuelTicks);
        return super.writeToNBT(tag);
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        fuelTicks = Math.max(0, Math.min(MAX_FUEL_TICKS, tag.getInteger("FuelTicks")));
    }
}

final class TileSolarPanel extends AbstractEnergyTile implements ITickable {
    TileSolarPanel() { super(2000, 0, 20); }
    @Override public void update() {
        if (world == null || world.isRemote) return;
        if (world.provider.hasSkyLight() && world.isDaytime() && !world.isRaining() && !world.isThundering()
                && world.canSeeSky(pos.up()) && energy.getCapacity() - energy.getEnergyStored() >= 4) {
            energy.addInternal(4);
        }
        EnergyTransfer.push(this, energy, 20);
        if (world.getTotalWorldTime() % 10L == 0L) { markDirty(); syncClient(); }
    }
}
final class TileEnergyCable extends AbstractEnergyTile implements ITickable {
    private int inLastTick;
    private int outLastTick;
    TileEnergyCable() { super(4000, 512, 512); }
    int getInLastTick() { return inLastTick; }
    int getOutLastTick() { return outLastTick; }
    @Override public void update() {
        if (world == null || world.isRemote) return;
        inLastTick = EnergyTransfer.pull(this, energy, 512);
        outLastTick = EnergyTransfer.push(this, energy, 512);
        if (world.getTotalWorldTime() % 10L == 0L) { markDirty(); syncClient(); }
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setInteger("CableIn", inLastTick);
        tag.setInteger("CableOut", outLastTick);
        return super.writeToNBT(tag);
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        inLastTick = Math.max(0, tag.getInteger("CableIn"));
        outLastTick = Math.max(0, tag.getInteger("CableOut"));
    }
}

final class TileElectricFurnace extends AbstractEnergyTile implements ITickable {
    private int cookTime;
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override protected void onContentsChanged(int slot) { markDirty(); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot == 0 && isSmeltable(stack); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 0 ? super.insertItem(slot, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 1 ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
    };
    TileElectricFurnace() { super(12000, 512, 0); }
    int getCookTime() { return cookTime; }
    int getInputCount() { return inventory.getStackInSlot(0).getCount(); }
    int getOutputCount() { return inventory.getStackInSlot(1).getCount(); }
    static boolean isSmeltable(ItemStack stack) {
        return stack != null && !stack.isEmpty() && !FurnaceRecipes.instance().getSmeltingResult(stack).isEmpty();
    }
    int insertInput(ItemStack stack) {
        if (!isSmeltable(stack)) return 0;
        int before = stack.getCount();
        ItemStack remainder = inventory.insertItem(0, stack.copy(), false);
        return before - remainder.getCount();
    }
    boolean takeOutput(EntityPlayer player) {
        ItemStack output = inventory.extractItem(1, 64, false);
        if (output.isEmpty()) return false;
        if (!player.inventory.addItemStackToInventory(output) && !output.isEmpty()) player.dropItem(output, false);
        return true;
    }
    private boolean canSmelt() {
        ItemStack input = inventory.getStackInSlot(0);
        if (input.isEmpty() || energy.getEnergyStored() < 10) return false;
        ItemStack result = FurnaceRecipes.instance().getSmeltingResult(input);
        if (result.isEmpty() || result.getCount() > result.getMaxStackSize()) return false;
        ItemStack output = inventory.getStackInSlot(1);
        if (output.isEmpty()) return result.getCount() <= inventory.getSlotLimit(1);
        if (!ItemStack.areItemsEqual(output, result) || !ItemStack.areItemStackTagsEqual(output, result)) return false;
        return output.getCount() + result.getCount() <= Math.min(output.getMaxStackSize(), inventory.getSlotLimit(1));
    }
    private void smeltOne() {
        ItemStack input = inventory.getStackInSlot(0);
        ItemStack result = FurnaceRecipes.instance().getSmeltingResult(input).copy();
        ItemStack output = inventory.getStackInSlot(1);
        if (output.isEmpty()) inventory.setStackInSlot(1, result);
        else {
            ItemStack merged = output.copy();
            merged.grow(result.getCount());
            inventory.setStackInSlot(1, merged);
        }
        ItemStack remaining = input.copy();
        remaining.shrink(1);
        inventory.setStackInSlot(0, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
    }
    @Override public void update() {
        if (world == null || world.isRemote) return;
        EnergyTransfer.pull(this, energy, 512);
        if (canSmelt() && energy.spendInternal(10) == 10) {
            cookTime++;
            if (cookTime >= 200) {
                smeltOne();
                cookTime = 0;
            }
        } else if (cookTime > 0) {
            cookTime = Math.max(0, cookTime - 2);
        }
        if (world.getTotalWorldTime() % 10L == 0L) { markDirty(); syncClient(); }
    }
    void dropContents() {
        if (world == null || world.isRemote) return;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i).copy();
            if (!stack.isEmpty()) {
                EntityItem entity = new EntityItem(world, pos.getX() + 0.5D, pos.getY() + 0.5D,
                        pos.getZ() + 0.5D, stack);
                if (world.spawnEntity(entity)) inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }
    @Override public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> cap, net.minecraft.util.EnumFacing side) {
        return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(cap, side);
    }
    @Override @SuppressWarnings("unchecked")
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, net.minecraft.util.EnumFacing side) {
        return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? (T) inventory : super.getCapability(cap, side);
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setTag("Inventory", inventory.serializeNBT());
        tag.setInteger("CookTime", cookTime);
        return super.writeToNBT(tag);
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("Inventory")) inventory.deserializeNBT(tag.getCompoundTag("Inventory"));
        cookTime = Math.max(0, Math.min(199, tag.getInteger("CookTime")));
    }
}

final class RepairMath {
    private RepairMath() { }
    static int points(int damage, int stored, int limit, int cost) {
        if (damage <= 0 || stored <= 0 || limit <= 0 || cost <= 0) return 0;
        return Math.min(damage, Math.min(limit, stored / cost));
    }
    static int energyCost(int points, int cost) {
        if (points <= 0 || cost <= 0) return 0;
        long result = (long) points * cost;
        return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
    }
}

final class TileElectricAnvil extends AbstractEnergyTile implements ITickable {
    private static final int ENERGY_PER_DURABILITY = 25;
    private static final int REPAIR_PER_TICK = 4;
    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override protected void onContentsChanged(int slot) { markDirty(); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot == 0 && isRepairable(stack); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 0 ? super.insertItem(slot, stack, simulate) : stack;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 0 && isFullyRepaired() ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
    };
    TileElectricAnvil() { super(100000, 1024, 0); }
    int getRepairDamage() { ItemStack stack = inventory.getStackInSlot(0); return stack.isEmpty() ? 0 : stack.getItemDamage(); }
    boolean hasRepairItem() { return !inventory.getStackInSlot(0).isEmpty(); }
    static boolean isRepairable(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getCount() == 1
                && stack.isItemStackDamageable() && stack.getItemDamage() > 0;
    }
    boolean isEmpty() { return inventory.getStackInSlot(0).isEmpty(); }
    boolean insertItem(ItemStack stack) {
        if (!isEmpty() || !isRepairable(stack)) return false;
        return inventory.insertItem(0, stack.copy(), false).isEmpty();
    }
    boolean isFullyRepaired() {
        ItemStack stack = inventory.getStackInSlot(0);
        return !stack.isEmpty() && stack.getItemDamage() == 0;
    }
    boolean takeOutput(EntityPlayer player) {
        ItemStack out = inventory.extractItem(0, 1, false);
        if (out.isEmpty()) return false;
        if (!player.inventory.addItemStackToInventory(out) && !out.isEmpty()) player.dropItem(out, false);
        return true;
    }
    @Override public void update() {
        if (world == null || world.isRemote) return;
        EnergyTransfer.pull(this, energy, 1024);
        ItemStack item = inventory.getStackInSlot(0);
        if (!item.isEmpty() && item.isItemStackDamageable() && item.getItemDamage() > 0) {
            int points = RepairMath.points(item.getItemDamage(), energy.getEnergyStored(),
                    REPAIR_PER_TICK, ENERGY_PER_DURABILITY);
            if (points > 0) {
                int cost = RepairMath.energyCost(points, ENERGY_PER_DURABILITY);
                if (energy.spendInternal(cost) == cost) {
                    ItemStack repaired = item.copy();
                    repaired.setItemDamage(Math.max(0, repaired.getItemDamage() - points));
                    inventory.setStackInSlot(0, repaired);
                }
            }
        }
        if (world.getTotalWorldTime() % 10L == 0L) { markDirty(); syncClient(); }
    }
    void dropContents() {
        if (world == null || world.isRemote) return;
        ItemStack stack = inventory.getStackInSlot(0).copy();
        if (!stack.isEmpty()) {
            EntityItem entity = new EntityItem(world, pos.getX() + 0.5D, pos.getY() + 0.5D,
                    pos.getZ() + 0.5D, stack);
            if (world.spawnEntity(entity)) inventory.setStackInSlot(0, ItemStack.EMPTY);
        }
    }
    @Override public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> cap, net.minecraft.util.EnumFacing side) {
        return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(cap, side);
    }
    @Override @SuppressWarnings("unchecked")
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, net.minecraft.util.EnumFacing side) {
        return cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? (T) inventory : super.getCapability(cap, side);
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setTag("Inventory", inventory.serializeNBT());
        return super.writeToNBT(tag);
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("Inventory")) inventory.deserializeNBT(tag.getCompoundTag("Inventory"));
    }
}

/**
 * Pure geometry and arithmetic for the Digital Mine. Keeps the multiblock
 * structure and excavation contracts in one place and stays unit-testable
 * without a Minecraft runtime.
 */
final class MineMath {
    /** Physical structure the player assembles: 3x2x3 Digital Mine blocks. */
    static final int STRUCT_WIDTH = 3;
    static final int STRUCT_HEIGHT = 2;
    static final int STRUCT_DEPTH = 3;
    static final int STRUCT_VOLUME = STRUCT_WIDTH * STRUCT_HEIGHT * STRUCT_DEPTH;
    /** Excavation volume below the assembled structure. */
    static final int AREA_WIDTH = 64;
    static final int AREA_HEIGHT = 128;
    static final int AREA_DEPTH = 64;
    static final int VOLUME = AREA_WIDTH * AREA_HEIGHT * AREA_DEPTH;
    private MineMath() { }
    /**
     * Scan index to {dx, dy, dz} offsets relative to the structure center at its
     * bottom layer. Layers are excavated top first, one 64x64 layer at a time.
     */
    static int[] offsets(int index) {
        if (index < 0 || index >= VOLUME) throw new IndexOutOfBoundsException("Scan index out of range: " + index);
        int layerSize = AREA_WIDTH * AREA_DEPTH;
        int layer = index / layerSize;
        int rest = index % layerSize;
        int dx = rest / AREA_DEPTH - AREA_WIDTH / 2;
        int dz = rest % AREA_DEPTH - AREA_DEPTH / 2;
        int dy = -1 - layer;
        return new int[] {dx, dy, dz};
    }
    static int clampCursor(int stored) { return Math.max(0, Math.min(VOLUME, stored)); }
    static long energyFor(int blocks, long costPerBlock) {
        if (blocks <= 0 || costPerBlock <= 0L) return 0L;
        // Saturate before multiplying: overflow can wrap around to a positive value.
        if (costPerBlock > Long.MAX_VALUE / blocks) return Long.MAX_VALUE;
        long total = (long) blocks * costPerBlock;
        return total < 0L ? Long.MAX_VALUE : total;
    }
}

final class TileDigitalMine extends AbstractEnergyTile implements ITickable {
    private static final int ENERGY_PER_BLOCK = 50;
    private static final int FILTER_LIMIT = 6;
    private static final int SCAN_PER_TICK = 2048;
    private static final int MAX_MINE_PER_TICK = 16;
    private static final long STRUCTURE_CHECK_INTERVAL = 20L;
    private final ItemStackHandler filter = new ItemStackHandler(FILTER_LIMIT) {
        @Override protected void onContentsChanged(int slot) { markDirty(); }
    };
    private final ItemStackHandler inventory = new ItemStackHandler(9) {
        @Override protected void onContentsChanged(int slot) { markDirty(); }
    };
    /** External mods and pipes may only take mined items out; insertion is internal-only. */
    private final IItemHandler outputView = new IItemHandler() {
        @Override public int getSlots() { return inventory.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return inventory.extractItem(slot, amount, simulate); }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return false; }
    };
    private int cursor;
    private boolean outputFull;
    private boolean structureComplete;
    private boolean controllerTile;
    private BlockPos controllerOrigin;
    private long lastStructureCheck;

    TileDigitalMine() { super(100000, 512, 0); }

    static int mineVolume() { return MineMath.VOLUME; }
    int getProgress() { return MineMath.clampCursor(cursor); }
    boolean isFinished() { return MineMath.clampCursor(cursor) >= MineMath.VOLUME; }
    boolean isOutputFull() { return outputFull; }
    boolean isStructureComplete() { return structureComplete; }
    boolean isController() { return controllerTile; }

    /** The elected controller of the assembled 3x2x3 structure, or null when absent. */
    TileDigitalMine findController() {
        if (world == null || world.isRemote || controllerOrigin == null) return null;
        TileEntity tile = world.getTileEntity(controllerOrigin);
        return tile instanceof TileDigitalMine ? (TileDigitalMine) tile : null;
    }

    void receiveInternal(int amount) { if (amount > 0) energy.addInternal(amount); }

    int getFilterCount() {
        int count = 0;
        for (int i = 0; i < filter.getSlots(); i++) if (!filter.getStackInSlot(i).isEmpty()) count++;
        return count;
    }
    List<String> getFilterNames() {
        List<String> names = new ArrayList<String>();
        for (int i = 0; i < filter.getSlots(); i++) {
            ItemStack entry = filter.getStackInSlot(i);
            if (!entry.isEmpty()) names.add(entry.getDisplayName());
        }
        return names;
    }
    int getOutputCount() {
        int count = 0;
        for (int i = 0; i < inventory.getSlots(); i++) count += inventory.getStackInSlot(i).getCount();
        return count;
    }

    /** Registers the held item type as a filter entry; the item itself is never consumed. */
    boolean addToFilter(ItemStack stack) {
        if (world == null || world.isRemote || stack == null || stack.isEmpty()) return false;
        for (int i = 0; i < filter.getSlots(); i++) {
            ItemStack entry = filter.getStackInSlot(i);
            if (!entry.isEmpty() && ItemStack.areItemsEqual(entry, stack)) return false;
        }
        ItemStack one = stack.copy();
        one.setCount(1);
        for (int i = 0; i < filter.getSlots(); i++) {
            if (filter.getStackInSlot(i).isEmpty()) {
                filter.setStackInSlot(i, one);
                markDirty(); syncClient();
                return true;
            }
        }
        return false;
    }

    boolean clearFilter() {
        if (getFilterCount() == 0) return false;
        for (int i = 0; i < filter.getSlots(); i++) filter.setStackInSlot(i, ItemStack.EMPTY);
        markDirty(); syncClient();
        return true;
    }

    boolean takeOutput(EntityPlayer player) {
        boolean taken = false;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack output = inventory.extractItem(i, 64, false);
            if (output.isEmpty()) continue;
            taken = true;
            if (!player.inventory.addItemStackToInventory(output) && !output.isEmpty()) player.dropItem(output, false);
        }
        return taken;
    }

    private static boolean isOre(ItemStack stack) {
        for (int id : OreDictionary.getOreIDs(stack)) {
            if (OreDictionary.getOreName(id).startsWith("ore")) return true;
        }
        return false;
    }

    private boolean matchesFilter(ItemStack stack) {
        for (int i = 0; i < filter.getSlots(); i++) {
            ItemStack entry = filter.getStackInSlot(i);
            if (entry.isEmpty()) continue;
            if (ItemStack.areItemsEqual(entry, stack)) return true;
            if (sharesOreName(entry, stack)) return true;
        }
        return false;
    }

    private static boolean sharesOreName(ItemStack first, ItemStack second) {
        for (int a : OreDictionary.getOreIDs(first)) {
            if (!OreDictionary.getOreName(a).startsWith("ore")) continue;
            for (int b : OreDictionary.getOreIDs(second)) {
                if (a == b) return true;
            }
        }
        return false;
    }

    /**
     * Elects the controller deterministically: among every complete 3x2x3 box of
     * Digital Mine blocks that contains this block, the one with the lowest
     * corner wins, and its corner block is the controller. Every tile of the
     * structure computes the same winner, so exactly one tile mines.
     */
    private void refreshStructure() {
        long now = world.getTotalWorldTime();
        if (now - lastStructureCheck >= STRUCTURE_CHECK_INTERVAL) {
            lastStructureCheck = now;
            BlockPos best = null;
            for (int ox = 0; ox < MineMath.STRUCT_WIDTH; ox++) {
                for (int oy = 0; oy < MineMath.STRUCT_HEIGHT; oy++) {
                    for (int oz = 0; oz < MineMath.STRUCT_DEPTH; oz++) {
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
        for (int dx = 0; dx < MineMath.STRUCT_WIDTH; dx++) {
            for (int dy = 0; dy < MineMath.STRUCT_HEIGHT; dy++) {
                for (int dz = 0; dz < MineMath.STRUCT_DEPTH; dz++) {
                    if (world.getBlockState(origin.add(dx, dy, dz)).getBlock() != TechRegistry.DIGITAL_MINE) return false;
                }
            }
        }
        return true;
    }

    private boolean isTargetBlock(IBlockState state, BlockPos target) {
        if (!world.isBlockLoaded(target)) return false; // never force-load or blind-mine chunks
        if (state.getBlock() == Blocks.BEDROCK) return false;
        if (world.getTileEntity(target) != null) return false; // never break container or technical blocks
        Item item = Item.getItemFromBlock(state.getBlock());
        if (item == null || item == Items.AIR) return false;
        ItemStack stack = new ItemStack(item, 1, state.getBlock().getMetaFromState(state));
        if (getFilterCount() == 0) return isOre(stack);
        return matchesFilter(stack);
    }

    private boolean canFitDrop(ItemStack drop) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (inventory.insertItem(i, drop, true).isEmpty()) return true;
        }
        return false;
    }

    private void insertInternal(ItemStack drop) {
        ItemStack remaining = drop;
        for (int i = 0; i < inventory.getSlots() && !remaining.isEmpty(); i++) {
            remaining = inventory.insertItem(i, remaining, false);
        }
        if (!remaining.isEmpty()) {
            EntityItem entity = new EntityItem(world, pos.getX() + 0.5D, pos.getY() + 0.5D,
                    pos.getZ() + 0.5D, remaining);
            world.spawnEntity(entity);
        }
    }

    /** Scans up to SCAN_PER_TICK positions and mines up to MAX_MINE_PER_TICK targets per tick. */
    private boolean mineTick() {
        BlockPos base = controllerOrigin.add(MineMath.STRUCT_WIDTH / 2, 0, MineMath.STRUCT_DEPTH / 2);
        boolean changed = false;
        int scanned = 0;
        int mined = 0;
        while (mined < MAX_MINE_PER_TICK && scanned < SCAN_PER_TICK
                && cursor < MineMath.VOLUME && energy.getEnergyStored() >= ENERGY_PER_BLOCK) {
            BlockPos target = null;
            IBlockState targetState = null;
            while (cursor < MineMath.VOLUME && scanned < SCAN_PER_TICK) {
                int[] offset = MineMath.offsets(cursor);
                cursor++;
                scanned++;
                BlockPos candidate = base.add(offset[0], offset[1], offset[2]);
                IBlockState state = world.getBlockState(candidate);
                if (isTargetBlock(state, candidate)) { target = candidate; targetState = state; break; }
            }
            if (target == null) break;
            List<ItemStack> drops = targetState.getBlock().getDrops(world, target, targetState, 0);
            boolean fits = true;
            for (ItemStack drop : drops) {
                if (drop == null || drop.isEmpty() || !canFitDrop(drop)) { fits = false; break; }
            }
            if (fits) {
                for (ItemStack drop : drops) if (drop != null && !drop.isEmpty()) insertInternal(drop);
                world.setBlockToAir(target);
                energy.spendInternal(ENERGY_PER_BLOCK);
                outputFull = false;
                changed = true;
                mined++;
            } else {
                cursor = MineMath.clampCursor(cursor - 1); // wait for room instead of losing drops
                outputFull = true;
                break;
            }
        }
        return changed;
    }

    @Override public void update() {
        if (world == null || world.isRemote) return;
        refreshStructure();
        if (!controllerTile) {
            // Energy a cable pushed into a non-controller block before validation
            // is forwarded to the controller so nothing is stranded.
            if (structureComplete && energy.getEnergyStored() > 0) {
                TileDigitalMine controller = findController();
                if (controller != null) {
                    controller.receiveInternal(energy.spendInternal(energy.getEnergyStored()));
                }
            }
            return;
        }
        EnergyTransfer.pull(this, energy, 512);
        boolean changed = !isFinished() && energy.getEnergyStored() >= ENERGY_PER_BLOCK && mineTick();
        if (changed || world.getTotalWorldTime() % 10L == 0L) { markDirty(); syncClient(); }
    }

    void dropContents() {
        if (world == null || world.isRemote) return;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i).copy();
            if (!stack.isEmpty()) {
                EntityItem entity = new EntityItem(world, pos.getX() + 0.5D, pos.getY() + 0.5D,
                        pos.getZ() + 0.5D, stack);
                if (world.spawnEntity(entity)) inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    @Override public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> cap, net.minecraft.util.EnumFacing side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return controllerTile && super.hasCapability(cap, side);
        }
        if (cap == net.minecraftforge.energy.CapabilityEnergy.ENERGY) {
            // Every block of the assembled structure charges the controller,
            // so cables touching any mine block can push FE in.
            if (!controllerTile && structureComplete) {
                TileDigitalMine controller = findController();
                if (controller != null) return controller.hasCapability(cap, side);
            }
            return controllerTile && super.hasCapability(cap, side);
        }
        return super.hasCapability(cap, side);
    }
    @Override @SuppressWarnings("unchecked")
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, net.minecraft.util.EnumFacing side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && controllerTile) return (T) outputView;
        if (cap == net.minecraftforge.energy.CapabilityEnergy.ENERGY && !controllerTile && structureComplete) {
            TileDigitalMine controller = findController();
            if (controller != null && controller.controllerTile) return controller.getCapability(cap, side);
        }
        return super.getCapability(cap, side);
    }

    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setTag("Filter", filter.serializeNBT());
        tag.setTag("Inventory", inventory.serializeNBT());
        tag.setInteger("MineCursor", cursor);
        tag.setBoolean("MineOutputFull", outputFull);
        tag.setBoolean("MineStruct", structureComplete);
        tag.setBoolean("MineCtrl", controllerTile);
        return super.writeToNBT(tag);
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("Filter")) filter.deserializeNBT(tag.getCompoundTag("Filter"));
        if (tag.hasKey("Inventory")) inventory.deserializeNBT(tag.getCompoundTag("Inventory"));
        cursor = MineMath.clampCursor(tag.getInteger("MineCursor"));
        outputFull = tag.getBoolean("MineOutputFull");
        structureComplete = tag.getBoolean("MineStruct");
        controllerTile = tag.getBoolean("MineCtrl");
    }
}