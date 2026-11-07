package br.com.mikael8367.nexustech;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

final class EnergyBuffer implements IEnergyStorage {
    private final int capacity, maxReceive, maxExtract;
    private int energy;
    EnergyBuffer(int capacity, int receive, int extract) {
        if (capacity <= 0 || receive < 0 || extract < 0) throw new IllegalArgumentException("Invalid FE limits");
        this.capacity = capacity; maxReceive = receive; maxExtract = extract;
    }
    @Override public int receiveEnergy(int amount, boolean simulate) {
        if (maxReceive == 0 || amount <= 0) return 0;
        int accepted = Math.min(capacity - energy, Math.min(maxReceive, amount));
        if (!simulate) energy += accepted;
        return accepted;
    }
    @Override public int extractEnergy(int amount, boolean simulate) {
        if (maxExtract == 0 || amount <= 0) return 0;
        int extracted = Math.min(energy, Math.min(maxExtract, amount));
        if (!simulate) energy -= extracted;
        return extracted;
    }
    int addInternal(int amount) {
        if (amount <= 0) return 0;
        int accepted = Math.min(capacity - energy, amount);
        energy += accepted;
        return accepted;
    }
    /**
     * Internal machine consumption. Unlike extractEnergy, this is not limited by
     * maxExtract: machines such as the electric furnace and anvil keep maxExtract = 0
     * so external cables cannot drain them, but still need to spend stored FE to work.
     */
    int spendInternal(int amount) {
        if (amount <= 0) return 0;
        int spent = Math.min(energy, amount);
        energy -= spent;
        return spent;
    }
    void setEnergyStored(int amount) { energy = Math.max(0, Math.min(capacity, amount)); }
    int getCapacity() { return capacity; }
    @Override public int getEnergyStored() { return energy; }
    @Override public int getMaxEnergyStored() { return capacity; }
    @Override public boolean canExtract() { return maxExtract > 0 && energy > 0; }
    @Override public boolean canReceive() { return maxReceive > 0 && energy < capacity; }
}

abstract class AbstractEnergyTile extends TileEntity {
    protected final EnergyBuffer energy;
    AbstractEnergyTile(int capacity, int receive, int extract) { energy = new EnergyBuffer(capacity, receive, extract); }
    @Override public boolean hasCapability(net.minecraftforge.common.capabilities.Capability<?> cap, EnumFacing side) {
        return cap == CapabilityEnergy.ENERGY || super.hasCapability(cap, side);
    }
    @Override @SuppressWarnings("unchecked")
    public <T> T getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, EnumFacing side) {
        return cap == CapabilityEnergy.ENERGY ? (T) energy : super.getCapability(cap, side);
    }
    int getStoredEnergy() { return energy.getEnergyStored(); }
    int getEnergyCapacity() { return energy.getCapacity(); }
    protected void syncClient() {
        if (world != null && !world.isRemote) {
            net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }
    @Override public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 1, getUpdateTag());
    }
    @Override public NBTTagCompound getUpdateTag() { return writeToNBT(new NBTTagCompound()); }
    @Override public void handleUpdateTag(NBTTagCompound tag) { readFromNBT(tag); }
    @Override public void onDataPacket(NetworkManager network, SPacketUpdateTileEntity packet) {
        readFromNBT(packet.getNbtCompound());
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setInteger("NexusEnergy", energy.getEnergyStored());
        return super.writeToNBT(tag);
    }
    @Override public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        energy.setEnergyStored(tag.getInteger("NexusEnergy"));
    }
}

final class EnergyTransfer {
    private EnergyTransfer() { }
    static boolean push(TileEntity source, EnergyBuffer buffer, int limit) {
        World world = source.getWorld();
        if (world == null || world.isRemote || limit <= 0) return false;
        int initial = buffer.getEnergyStored();
        int budget = Math.min(limit, initial);
        BlockPos origin = source.getPos();
        for (EnumFacing side : EnumFacing.values()) {
            if (budget <= 0) break;
            TileEntity target = world.getTileEntity(origin.offset(side));
            if (target == null || target == source || !target.hasCapability(CapabilityEnergy.ENERGY, side.getOpposite())) continue;
            IEnergyStorage receiver = target.getCapability(CapabilityEnergy.ENERGY, side.getOpposite());
            if (receiver == null || !receiver.canReceive()) continue;
            int simulated = receiver.receiveEnergy(buffer.extractEnergy(budget, true), true);
            if (simulated <= 0) continue;
            int extracted = buffer.extractEnergy(simulated, false);
            int inserted = receiver.receiveEnergy(extracted, false);
            if (inserted < extracted) buffer.addInternal(extracted - inserted);
            budget -= inserted;
        }
        return initial != buffer.getEnergyStored();
    }

    static boolean pull(TileEntity destination, EnergyBuffer buffer, int limit) {
        World world = destination.getWorld();
        if (world == null || world.isRemote || limit <= 0 || !buffer.canReceive()) return false;
        int initial = buffer.getEnergyStored();
        int budget = Math.min(limit, buffer.getMaxEnergyStored() - initial);
        BlockPos origin = destination.getPos();
        for (EnumFacing side : EnumFacing.values()) {
            if (budget <= 0) break;
            TileEntity source = world.getTileEntity(origin.offset(side));
            if (source == null || source == destination || source instanceof TileEnergyCable
                    || source instanceof TileCoalGenerator || source instanceof TileSolarPanel
                    || !source.hasCapability(CapabilityEnergy.ENERGY, side.getOpposite())) continue;
            IEnergyStorage provider = source.getCapability(CapabilityEnergy.ENERGY, side.getOpposite());
            if (provider == null || !provider.canExtract()) continue;
            int accepted = buffer.receiveEnergy(provider.extractEnergy(budget, true), true);
            if (accepted <= 0) continue;
            int extracted = provider.extractEnergy(accepted, false);
            int inserted = buffer.receiveEnergy(extracted, false);
            // Keep FE conserved if a third-party provider changes its response between simulation and commit.
            if (inserted < extracted) buffer.addInternal(extracted - inserted);
            budget -= inserted;
        }
        return initial != buffer.getEnergyStored();
    }
}
