package br.com.mikael8367.nexustech;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Server-authoritative, read-only machine status window. */
public final class TechGuiHandler implements IGuiHandler {
    public static final int GUI_GENERATOR = 0;
    public static final int GUI_SOLAR = 1;
    public static final int GUI_CABLE = 2;
    public static final int GUI_FURNACE = 3;
    public static final int GUI_ANVIL = 4;
    public static final int GUI_DIGITAL_MINE = 5;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (!isValid(id, tile)) return null;
        return new TechMachineContainer(player, tile);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if (!isValid(id, tile)) return null;
        return new TechMachineScreen(tile, title(id));
    }

    private static boolean isValid(int id, TileEntity tile) {
        switch (id) {
            case GUI_GENERATOR: return tile instanceof TileCoalGenerator;
            case GUI_SOLAR: return tile instanceof TileSolarPanel;
            case GUI_CABLE: return tile instanceof TileEnergyCable;
            case GUI_FURNACE: return tile instanceof TileElectricFurnace;
            case GUI_ANVIL: return tile instanceof TileElectricAnvil;
            case GUI_DIGITAL_MINE: return tile instanceof TileDigitalMine;
            default: return false;
        }
    }

    private static String title(int id) {
        switch (id) {
            case GUI_GENERATOR: return "Gerador a carvão";
            case GUI_SOLAR: return "Painel solar";
            case GUI_CABLE: return "Cabo de energia";
            case GUI_FURNACE: return "Forno elétrico";
            case GUI_ANVIL: return "Bigorna elétrica";
            case GUI_DIGITAL_MINE: return "Mina Digital";
            default: return "Nexus Tech";
        }
    }
}

final class TechMachineContainer extends Container {
    private final EntityPlayer player;
    private final TileEntity tile;
    TechMachineContainer(EntityPlayer player, TileEntity tile) {
        this.player = player;
        this.tile = tile;
    }
    @Override public boolean canInteractWith(EntityPlayer candidate) {
        if (candidate != player || tile.isInvalid() || tile.getWorld() == null) return false;
        BlockPos pos = tile.getPos();
        return candidate.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D
                && tile.getWorld().getTileEntity(pos) == tile;
    }
}
