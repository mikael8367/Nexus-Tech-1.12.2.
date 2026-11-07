package br.com.mikael8367.nexustech;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

/** Ore and ingot catalog shared by every Nexus Tech machine. */
final class TechResources {
    static final String[] ORE_NAMES = {"copper_ore", "tin_ore", "aluminum_ore", "lead_ore"};
    static final String[] INGOT_NAMES = {"copper_ingot", "tin_ingot", "aluminum_ingot", "lead_ingot"};
    static final String[] ORE_DICTIONARY = {"oreCopper", "oreTin", "oreAluminum", "oreLead"};
    static final String[] INGOT_DICTIONARY = {"ingotCopper", "ingotTin", "ingotAluminum", "ingotLead"};
    private static final Block[] ORE_BLOCKS = new Block[ORE_NAMES.length];
    private static final Item[] INGOT_ITEMS = new Item[INGOT_NAMES.length];

    static Block ore(int index) { return ORE_BLOCKS[index]; }
    static Item ingot(int index) { return INGOT_ITEMS[index]; }
    static void registerOreBlock(int index, Block block) { ORE_BLOCKS[index] = block; }
    static void registerIngotItem(int index, Item item) { INGOT_ITEMS[index] = item; }

    private TechResources() { }
}

final class BlockTechOre extends Block {
    BlockTechOre(String name) {
        super(Material.ROCK);
        setRegistryName(NexusTechMod.MOD_ID, name);
        setTranslationKey(NexusTechMod.MOD_ID + "." + name);
        setCreativeTab(NexusTechCreativeTab.INSTANCE);
        setHardness(3.0F);
        setResistance(5.0F);
        setHarvestLevel("pickaxe", 1);
    }
}

final class ItemTechIngot extends Item {
    ItemTechIngot(String name) {
        setRegistryName(NexusTechMod.MOD_ID, name);
        setTranslationKey(NexusTechMod.MOD_ID + "." + name);
        setCreativeTab(NexusTechCreativeTab.INSTANCE);
    }
}

final class TechResourceRegistry {
    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> event) {
        for (int i = 0; i < TechResources.ORE_NAMES.length; i++) {
            Block ore = new BlockTechOre(TechResources.ORE_NAMES[i]);
            TechResources.registerOreBlock(i, ore);
            event.getRegistry().register(ore);
        }
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        for (int i = 0; i < TechResources.ORE_NAMES.length; i++) {
            Block ore = TechResources.ore(i);
            if (ore == null) throw new IllegalStateException("Ore block registration order invalid");
            event.getRegistry().register(new ItemBlock(ore).setRegistryName(ore.getRegistryName()));
            OreDictionary.registerOre(TechResources.ORE_DICTIONARY[i], new ItemStack(ore));
        }
        for (int i = 0; i < TechResources.INGOT_NAMES.length; i++) {
            Item ingot = new ItemTechIngot(TechResources.INGOT_NAMES[i]);
            TechResources.registerIngotItem(i, ingot);
            event.getRegistry().register(ingot);
            OreDictionary.registerOre(TechResources.INGOT_DICTIONARY[i], new ItemStack(ingot));
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void registerModels(ModelRegistryEvent event) {
        for (int i = 0; i < TechResources.ORE_NAMES.length; i++) {
            registerItemModel(Item.getItemFromBlock(TechResources.ore(i)));
        }
        for (int i = 0; i < TechResources.INGOT_NAMES.length; i++) {
            registerItemModel(TechResources.ingot(i));
        }
    }

    @SideOnly(Side.CLIENT)
    private static void registerItemModel(Item item) {
        if (item != null) ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }
}

/** Overworld vein generation for the Nexus Tech ores. */
final class TechWorldGen implements IWorldGenerator {
    private static final int[] MIN_Y = {20, 5, 10, 5};
    private static final int[] MAX_Y = {64, 40, 48, 32};
    private static final int[] ATTEMPTS = {12, 10, 8, 8};
    private static final int[] VEIN_SIZE = {8, 7, 6, 6};
    private final WorldGenMinable[] veins;

    TechWorldGen() { veins = new WorldGenMinable[VEIN_SIZE.length]; }

    private void ensureVeins() {
        for (int i = 0; i < veins.length; i++) {
            if (veins[i] == null) {
                veins[i] = new WorldGenMinable(TechResources.ore(i).getDefaultState(), VEIN_SIZE[i]);
            }
        }
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
            IChunkGenerator generator, IChunkProvider provider) {
        if (world.provider.getDimension() != 0) return; // overworld only
        ensureVeins();
        for (int i = 0; i < veins.length; i++) {
            for (int attempt = 0; attempt < ATTEMPTS[i]; attempt++) {
                BlockPos pos = new BlockPos(chunkX * 16 + random.nextInt(16),
                        MIN_Y[i] + random.nextInt(MAX_Y[i] - MIN_Y[i] + 1),
                        chunkZ * 16 + random.nextInt(16));
                veins[i].generate(world, random, pos);
            }
        }
    }
}
