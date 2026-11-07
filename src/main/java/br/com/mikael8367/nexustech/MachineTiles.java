package br.com.mikael8367.nexustech;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

final class TileCoalGenerator extends AbstractEnergyTile implements ITickable {
    private static final int MAX_FUEL_TICKS = 32000;
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
        if (world.provider.hasSkyLight() && world.isDaytime() && !world.isRaining()
                && world.canSeeSky(pos.up()) && energy.getCapacity() - energy.getEnergyStored() >= 4) {
            energy.addInternal(4);
        }
        EnergyTransfer.push(this, energy, 20);
        if (world.getTotalWorldTime() % 10L == 0L) { markDirty(); syncClient(); }
    }
}
final class TileEnergyCable extends AbstractEnergyTile implements ITickable {
    TileEnergyCable() { super(4000, 512, 512); }
    @Override public void update() {
        if (world == null || world.isRemote) return;
        EnergyTransfer.pull(this, energy, 512);
        EnergyTransfer.push(this, energy, 512);
        if (world.getTotalWorldTime() % 10L == 0L) { markDirty(); syncClient(); }
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
