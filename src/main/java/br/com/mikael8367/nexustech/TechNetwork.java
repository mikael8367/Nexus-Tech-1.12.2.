package br.com.mikael8367.nexustech;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/** Tiny channel used by the reactor GUI slots. */
final class TechNetwork {
    static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(NexusTechMod.MOD_ID);

    static void register() {
        CHANNEL.registerMessage(ReactorSlotMessage.Handler.class, ReactorSlotMessage.class, 0, Side.SERVER);
    }

    /** Slot 0 = uranium rod/ingot, slot 1 = water bucket; both use the item held in the main hand. */
    static final class ReactorSlotMessage implements IMessage {
        private BlockPos pos;
        private int slot;

        ReactorSlotMessage() {}
        ReactorSlotMessage(BlockPos pos, int slot) { this.pos = pos; this.slot = slot; }

        @Override public void fromBytes(ByteBuf buf) {
            pos = BlockPos.fromLong(buf.readLong());
            slot = buf.readByte();
        }
        @Override public void toBytes(ByteBuf buf) {
            buf.writeLong(pos.toLong());
            buf.writeByte(slot);
        }

        static final class Handler implements IMessageHandler<ReactorSlotMessage, IMessage> {
            @Override public IMessage onMessage(final ReactorSlotMessage message, final MessageContext ctx) {
                final EntityPlayerMP player = ctx.getServerHandler().player;
                player.getServerWorld().addScheduledTask(new Runnable() {
                    @Override public void run() { handle(player, message.pos, message.slot); }
                });
                return null;
            }
        }

        static void handle(EntityPlayerMP player, BlockPos pos, int slot) {
            if (player.getDistanceSq(pos) > 64.0D * 64.0D) return;
            TileEntity tile = player.world.getTileEntity(pos);
            if (!(tile instanceof TileNuclearReactor)) return;
            TileNuclearReactor clicked = (TileNuclearReactor) tile;
            TileNuclearReactor controller = clicked.isController() ? clicked : clicked.findController();
            if (controller == null) controller = clicked;
            ItemStack held = player.getHeldItem(EnumHand.MAIN_HAND);
            if (slot == 0) {
                if (held.isEmpty() || (held.getItem() != TechResources.uraniumRod()
                        && held.getItem() != TechResources.uraniumIngot())) return;
                boolean rod = held.getItem() == TechResources.uraniumRod();
                if (controller.insertFuel(rod ? ReactorMath.ROD_TICKS : ReactorMath.ROD_TICKS / 4)) {
                    if (!player.capabilities.isCreativeMode) held.shrink(1);
                    player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(rod
                            ? "Barra de uranio inserida."
                            : "Lingote de uranio inserido (25% de uma barra)."), true);
                } else {
                    player.sendStatusMessage(new net.minecraft.util.text.TextComponentString(
                            "O reator ja tem combustivel: gaste o atual antes de abastecer."), true);
                }
            } else if (slot == 1) {
                if (held.isEmpty() || held.getItem() != Items.WATER_BUCKET) return;
                if (controller.addWater(1000) > 0 && !player.capabilities.isCreativeMode) {
                    player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.BUCKET));
                }
            }
        }
    }
}
