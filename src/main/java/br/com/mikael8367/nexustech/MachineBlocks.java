package br.com.mikael8367.nexustech;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

abstract class BlockTechMachine extends BlockContainer {
    protected boolean openMachineGui(World world, BlockPos pos, EntityPlayer player, int id) {
        if (!world.isRemote) player.openGui(NexusTechMod.INSTANCE, id, world, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }
    BlockTechMachine(String name, float hardness) {
        super(Material.IRON);
        setRegistryName(NexusTechMod.MOD_ID, name);
        setTranslationKey(NexusTechMod.MOD_ID + "." + name);
        setCreativeTab(NexusTechCreativeTab.INSTANCE);
        setHardness(hardness);
        setResistance(10.0F);
        setHarvestLevel("pickaxe", 1);
    }
    @Override public EnumBlockRenderType getRenderType(IBlockState state) { return EnumBlockRenderType.MODEL; }
}

final class BlockCoalGenerator extends BlockTechMachine {
    BlockCoalGenerator() { super("coal_generator", 3.5F); }
    @Override public TileEntity createNewTileEntity(World world, int meta) { return new TileCoalGenerator(); }
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
            EnumHand hand, net.minecraft.util.EnumFacing side, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) return true;
        ItemStack held = player.getHeldItem(hand);
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileCoalGenerator)) return false;
        if (isFuel(held)) {
            if (!world.isRemote && ((TileCoalGenerator) tile).addFuel(held) && !player.capabilities.isCreativeMode) held.shrink(1);
            return true;
        }
        return openMachineGui(world, pos, player, TechGuiHandler.GUI_GENERATOR);
    }
    static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && ((stack.getItem() == Items.COAL && (stack.getMetadata() == 0 || stack.getMetadata() == 1))
                || stack.getItem() == Item.getItemFromBlock(Blocks.COAL_BLOCK));
    }
}
final class BlockSolarPanel extends BlockTechMachine {
    private static final AxisAlignedBB BOUNDS = new AxisAlignedBB(0, 0, 0, 1, 0.3125, 1);
    BlockSolarPanel() { super("solar_panel", 1.5F); }
    @Override public TileEntity createNewTileEntity(World world, int meta) { return new TileSolarPanel(); }
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, net.minecraft.util.EnumFacing side, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) return true;
        return openMachineGui(world, pos, player, TechGuiHandler.GUI_SOLAR);
    }
    @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) { return BOUNDS; }
    @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) { return BOUNDS; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
}
final class BlockEnergyCable extends BlockTechMachine {
    private static final AxisAlignedBB BOUNDS = new AxisAlignedBB(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);
    BlockEnergyCable() { super("energy_cable", 1.0F); }
    @Override public TileEntity createNewTileEntity(World world, int meta) { return new TileEnergyCable(); }
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, net.minecraft.util.EnumFacing side, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) return true;
        return openMachineGui(world, pos, player, TechGuiHandler.GUI_CABLE);
    }
    @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) { return BOUNDS; }
    @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) { return BOUNDS; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
}
final class BlockElectricFurnace extends BlockTechMachine {
    BlockElectricFurnace() { super("electric_furnace", 3.5F); }
    @Override public TileEntity createNewTileEntity(World world, int meta) { return new TileElectricFurnace(); }
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
            EnumHand hand, net.minecraft.util.EnumFacing side, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) return true;
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileElectricFurnace)) return false;
        if (world.isRemote) return true;
        TileElectricFurnace furnace = (TileElectricFurnace) tile;
        ItemStack held = player.getHeldItem(hand);
        if (!held.isEmpty() && TileElectricFurnace.isSmeltable(held)) {
            int inserted = furnace.insertInput(held);
            if (inserted > 0) { if (!player.capabilities.isCreativeMode) held.shrink(inserted); return true; }
        }
        if (player.isSneaking()) return furnace.takeOutput(player);
        return openMachineGui(world, pos, player, TechGuiHandler.GUI_FURNACE);
    }
    @Override public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof TileElectricFurnace) ((TileElectricFurnace) tile).dropContents();
        super.breakBlock(world, pos, state);
    }
}
final class BlockElectricAnvil extends BlockTechMachine {
    private static final AxisAlignedBB BOUNDS = new AxisAlignedBB(0.0625, 0, 0.125, 0.9375, 1, 0.875);
    BlockElectricAnvil() { super("electric_anvil", 4.0F); }
    @Override public TileEntity createNewTileEntity(World world, int meta) { return new TileElectricAnvil(); }
    @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) { return BOUNDS; }
    @Override public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) { return BOUNDS; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
            EnumHand hand, net.minecraft.util.EnumFacing side, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) return true;
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileElectricAnvil)) return false;
        if (world.isRemote) return true;
        TileElectricAnvil anvil = (TileElectricAnvil) tile;
        ItemStack held = player.getHeldItem(hand);
        if (anvil.isEmpty() && TileElectricAnvil.isRepairable(held)) {
            if (anvil.insertItem(held) && !player.capabilities.isCreativeMode) held.shrink(1);
            return true;
        }
        if (player.isSneaking()) return anvil.takeOutput(player);
        return openMachineGui(world, pos, player, TechGuiHandler.GUI_ANVIL);
    }
    @Override public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof TileElectricAnvil) ((TileElectricAnvil) tile).dropContents();
        super.breakBlock(world, pos, state);
    }
}

final class BlockDigitalMine extends BlockTechMachine {
    BlockDigitalMine() { super("digital_mine", 3.5F); }
    @Override public TileEntity createNewTileEntity(World world, int meta) { return new TileDigitalMine(); }
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
            EnumHand hand, net.minecraft.util.EnumFacing side, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) return true;
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileDigitalMine)) return false;
        if (world.isRemote) return true;
        TileDigitalMine mine = (TileDigitalMine) tile;
        // Any block of the assembled 3x2x3 structure acts on its controller.
        TileDigitalMine controller = mine.isController() ? mine : mine.findController();
        TileDigitalMine actor = controller != null ? controller : mine;
        ItemStack held = player.getHeldItem(hand);
        if (!held.isEmpty()) {
            // Registers the held item as a filter entry; the item is never consumed.
            actor.addToFilter(held);
            return true;
        }
        if (player.isSneaking()) {
            if (actor.takeOutput(player)) return true;
            return actor.clearFilter();
        }
        BlockPos guiPos = controller != null ? controller.getPos() : pos;
        return openMachineGui(world, guiPos, player, TechGuiHandler.GUI_DIGITAL_MINE);
    }
    @Override public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (!world.isRemote && tile instanceof TileDigitalMine) ((TileDigitalMine) tile).dropContents();
        super.breakBlock(world, pos, state);
    }
}

final class BlockNuclearReactor extends BlockTechMachine {
    BlockNuclearReactor() { super("nuclear_reactor", 5.0F); }
    @Override public TileEntity createNewTileEntity(World world, int meta) { return new TileNuclearReactor(); }
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
            EnumHand hand, net.minecraft.util.EnumFacing side, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) return true;
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileNuclearReactor)) return false;
        if (world.isRemote) return true;
        TileNuclearReactor reactor = (TileNuclearReactor) tile;
        // Any block of the assembled 3x5x3 structure acts on its controller.
        TileNuclearReactor controller = reactor.isController() ? reactor : reactor.findController();
        if (controller == null) return openMachineGui(world, pos, player, TechGuiHandler.GUI_REACTOR);
        ItemStack held = player.getHeldItem(hand);
        if (!held.isEmpty() && held.getItem() == Items.WATER_BUCKET) {
            if (controller.addWater(1000) > 0 && !player.capabilities.isCreativeMode) {
                player.setHeldItem(hand, new ItemStack(Items.BUCKET));
            }
            return true;
        }
        if (!held.isEmpty() && held.getItem() == TechResources.uraniumRod()) {
            if (controller.insertFuel() && !player.capabilities.isCreativeMode) held.shrink(1);
            return true;
        }
        return openMachineGui(world, controller.getPos(), player, TechGuiHandler.GUI_REACTOR);
    }
}

final class TechRegistry {
    static BlockCoalGenerator COAL_GENERATOR;
    static BlockSolarPanel SOLAR_PANEL;
    static BlockEnergyCable ENERGY_CABLE;
    static BlockElectricFurnace ELECTRIC_FURNACE;
    static BlockElectricAnvil ELECTRIC_ANVIL;
    static BlockDigitalMine DIGITAL_MINE;
    static BlockNuclearReactor NUCLEAR_REACTOR;

    @SubscribeEvent public void registerBlocks(RegistryEvent.Register<Block> event) {
        COAL_GENERATOR = new BlockCoalGenerator();
        SOLAR_PANEL = new BlockSolarPanel();
        ENERGY_CABLE = new BlockEnergyCable();
        ELECTRIC_FURNACE = new BlockElectricFurnace();
        ELECTRIC_ANVIL = new BlockElectricAnvil();
        DIGITAL_MINE = new BlockDigitalMine();
        NUCLEAR_REACTOR = new BlockNuclearReactor();
        event.getRegistry().register(COAL_GENERATOR);
        event.getRegistry().register(SOLAR_PANEL);
        event.getRegistry().register(ENERGY_CABLE);
        event.getRegistry().register(ELECTRIC_FURNACE);
        event.getRegistry().register(ELECTRIC_ANVIL);
        event.getRegistry().register(DIGITAL_MINE);
        event.getRegistry().register(NUCLEAR_REACTOR);
        GameRegistry.registerTileEntity(TileCoalGenerator.class, new net.minecraft.util.ResourceLocation(NexusTechMod.MOD_ID, "coal_generator"));
        GameRegistry.registerTileEntity(TileSolarPanel.class, new net.minecraft.util.ResourceLocation(NexusTechMod.MOD_ID, "solar_panel"));
        GameRegistry.registerTileEntity(TileEnergyCable.class, new net.minecraft.util.ResourceLocation(NexusTechMod.MOD_ID, "energy_cable"));
        GameRegistry.registerTileEntity(TileElectricFurnace.class, new net.minecraft.util.ResourceLocation(NexusTechMod.MOD_ID, "electric_furnace"));
        GameRegistry.registerTileEntity(TileElectricAnvil.class, new net.minecraft.util.ResourceLocation(NexusTechMod.MOD_ID, "electric_anvil"));
        GameRegistry.registerTileEntity(TileDigitalMine.class, new net.minecraft.util.ResourceLocation(NexusTechMod.MOD_ID, "digital_mine"));
        GameRegistry.registerTileEntity(TileNuclearReactor.class, new net.minecraft.util.ResourceLocation(NexusTechMod.MOD_ID, "nuclear_reactor"));
    }
    @SubscribeEvent public void registerItems(RegistryEvent.Register<Item> event) {
        Block[] blocks = {COAL_GENERATOR, SOLAR_PANEL, ENERGY_CABLE, ELECTRIC_FURNACE, ELECTRIC_ANVIL, DIGITAL_MINE, NUCLEAR_REACTOR};
        for (Block block : blocks) {
            if (block == null) throw new IllegalStateException("Block registration order invalid");
            event.getRegistry().register(new ItemBlock(block).setRegistryName(block.getRegistryName()));
        }
    }
    @SideOnly(Side.CLIENT)
    @SubscribeEvent public void registerModels(ModelRegistryEvent event) {
        registerModel(COAL_GENERATOR); registerModel(SOLAR_PANEL); registerModel(ENERGY_CABLE);
        registerModel(ELECTRIC_FURNACE); registerModel(ELECTRIC_ANVIL); registerModel(DIGITAL_MINE);
        registerModel(NUCLEAR_REACTOR);
    }
    @SideOnly(Side.CLIENT)
    private static void registerModel(Block block) {
        if (block != null) ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(block), 0,
                new ModelResourceLocation(block.getRegistryName(), "inventory"));
    }
}
