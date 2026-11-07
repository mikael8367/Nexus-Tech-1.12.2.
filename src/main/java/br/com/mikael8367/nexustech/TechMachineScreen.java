package br.com.mikael8367.nexustech;

import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.tileentity.TileEntity;

/**
 * Compact status-only screen in the vanilla Minecraft container style:
 * beveled gray panel, dark title text, no-drop-shadow font and
 * vanilla-flavored progress bars. Item insertion/removal remains
 * server-side on block interaction.
 */
public final class TechMachineScreen extends GuiScreen {
    private static final int PANEL_WIDTH = 176;
    private static final int DEFAULT_PANEL_HEIGHT = 118;
    private static final int MINE_PANEL_HEIGHT = 148;
    private static final int REACTOR_PANEL_HEIGHT = 156;

    private static final int BG = 0xFFC6C6C6;
    private static final int BEVEL_LIGHT = 0xFFFFFFFF;
    private static final int BEVEL_DARK = 0xFF555555;
    private static final int OUTLINE = 0xFF000000;
    private static final int TEXT = 0xFF404040;
    private static final int TEXT_SOFT = 0xFF6E6E6E;
    private static final int STATUS_OK = 0xFF2E6B2E;
    private static final int STATUS_WARN = 0xFF8B3030;
    private static final int BAR_TRACK = 0xFF8B8B8B;
    private static final int BAR_FILL = 0xFFF0F0F0;
    private static final int ENERGY_FILL = 0xFF3FBF3F;
    private static final int HEAT_FILL = 0xFFD9711E;
    private static final int HEAT_FILL_HOT = 0xFFB02020;
    private static final int WATER_FILL = 0xFF3050DC;

    private final TileEntity tile;
    private final String title;

    public TechMachineScreen(TileEntity tile, String title) {
        this.tile = tile;
        this.title = title;
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int panelHeight = tile instanceof TileDigitalMine ? MINE_PANEL_HEIGHT
                : tile instanceof TileNuclearReactor ? REACTOR_PANEL_HEIGHT : DEFAULT_PANEL_HEIGHT;
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - panelHeight) / 2;
        drawPanel(left, top, left + PANEL_WIDTH, top + panelHeight);
        fontRenderer.drawString(title, left + 8, top + 6, TEXT, false);
        int y = top + 22;
        if (tile instanceof AbstractEnergyTile) {
            AbstractEnergyTile energy = (AbstractEnergyTile) tile;
            fontRenderer.drawString("Energia", left + 8, y, TEXT, false);
            fontRenderer.drawString(formatFe(energy.getStoredEnergy()) + " / " + formatFe(energy.getEnergyCapacity()) + " FE",
                    left + 62, y, TEXT_SOFT, false);
            drawBar(left + 8, y + 9, PANEL_WIDTH - 16, energy.getStoredEnergy(), energy.getEnergyCapacity(), ENERGY_FILL);
            y += 16;
        }
        if (tile instanceof TileCoalGenerator) {
            TileCoalGenerator generator = (TileCoalGenerator) tile;
            fontRenderer.drawString("Combustível", left + 8, y, TEXT, false);
            fontRenderer.drawString(generator.getFuelTicks() + " ticks", left + 86, y, TEXT_SOFT, false);
            drawBar(left + 8, y + 9, PANEL_WIDTH - 16, generator.getFuelTicks(), TileCoalGenerator.MAX_FUEL_TICKS, BAR_FILL);
            y += 16;
            fontRenderer.drawString("Clique com carvão na mão.", left + 8, y, TEXT_SOFT, false);
        } else if (tile instanceof TileSolarPanel) {
            fontRenderer.drawString("Geração: até 4 FE/t de dia", left + 8, y, TEXT, false);
            y += 12;
            fontRenderer.drawString("Céu aberto, sem chuva.", left + 8, y, TEXT_SOFT, false);
        } else if (tile instanceof TileEnergyCable) {
            fontRenderer.drawString("Transferência: 512 FE/t", left + 8, y, TEXT, false);
            y += 12;
            fontRenderer.drawString("Conecte máquinas FE.", left + 8, y, TEXT_SOFT, false);
        } else if (tile instanceof TileElectricFurnace) {
            TileElectricFurnace furnace = (TileElectricFurnace) tile;
            fontRenderer.drawString("Entrada: " + furnace.getInputCount() + " | Saída: " + furnace.getOutputCount(),
                    left + 8, y, TEXT, false);
            y += 12;
            fontRenderer.drawString("Progresso", left + 8, y, TEXT, false);
            fontRenderer.drawString(furnace.getCookTime() + " / 200", left + 70, y, TEXT_SOFT, false);
            drawBar(left + 8, y + 9, PANEL_WIDTH - 16, furnace.getCookTime(), 200, BAR_FILL);
            y += 16;
            fontRenderer.drawString("Clique com item smeltável.", left + 8, y, TEXT_SOFT, false);
            y += 12;
            fontRenderer.drawString("Agachar retira a saída.", left + 8, y, TEXT_SOFT, false);
        } else if (tile instanceof TileElectricAnvil) {
            TileElectricAnvil anvil = (TileElectricAnvil) tile;
            fontRenderer.drawString(anvil.hasRepairItem() ? "Dano restante: " + anvil.getRepairDamage()
                    : "Nenhum item para reparar", left + 8, y, anvil.hasRepairItem() ? TEXT : TEXT_SOFT, false);
            y += 12;
            fontRenderer.drawString("4 pontos/t, 25 FE/ponto", left + 8, y, TEXT_SOFT, false);
            y += 12;
            fontRenderer.drawString("Agachar retira o item.", left + 8, y, TEXT_SOFT, false);
        } else if (tile instanceof TileNuclearReactor) {
            TileNuclearReactor reactor = (TileNuclearReactor) tile;
            if (!reactor.isStructureComplete()) {
                fontRenderer.drawString("Estrutura incompleta:", left + 8, y, STATUS_WARN, false);
                y += 11;
                fontRenderer.drawString("monte 3x5x3 deste bloco.", left + 8, y, STATUS_WARN, false);
                y += 12;
            } else {
                fontRenderer.drawString("Calor", left + 8, y, TEXT, false);
                fontRenderer.drawString(reactor.getHeat() + " / " + ReactorMath.MAX_HEAT,
                        left + 40, y, TEXT_SOFT, false);
                boolean hot = reactor.getHeat() * 5 >= ReactorMath.MAX_HEAT * 4;
                drawBar(left + 8, y + 9, PANEL_WIDTH - 16, reactor.getHeat(), ReactorMath.MAX_HEAT,
                        hot ? HEAT_FILL_HOT : HEAT_FILL);
                y += 16;
                fontRenderer.drawString("Água", left + 8, y, TEXT, false);
                fontRenderer.drawString(reactor.getWaterMb() + " / " + ReactorMath.MAX_WATER_MB + " mB",
                        left + 40, y, TEXT_SOFT, false);
                drawBar(left + 8, y + 9, PANEL_WIDTH - 16, reactor.getWaterMb(), ReactorMath.MAX_WATER_MB, WATER_FILL);
                y += 16;
                fontRenderer.drawString(reactor.getFuelTicks() > 0
                        ? "Combustível: " + reactor.getFuelTicks() + " ticks"
                        : "Sem barra de urânio", left + 8, y,
                        reactor.getFuelTicks() > 0 ? TEXT : TEXT_SOFT, false);
                y += 12;
                if (reactor.isMeltdownActive()) {
                    String alarm = "! EXPLOSÃO EM " + reactor.getMeltdownSeconds() + "s !";
                    boolean blink = System.currentTimeMillis() / 400L % 2L == 0L;
                    fontRenderer.drawString(alarm, left + 8, y, blink ? STATUS_WARN : TEXT, false);
                    y += 12;
                } else if (hot) {
                    fontRenderer.drawString("Calor crítico: resfrie!", left + 8, y, STATUS_WARN, false);
                    y += 12;
                } else {
                    fontRenderer.drawString("Reação " + (reactor.getFuelTicks() > 0 ? "estável" : "parada"),
                            left + 8, y, TEXT_SOFT, false);
                    y += 12;
                }
                fontRenderer.drawString("Gelo ao redor: -" + reactor.getIceCount() + " calor/t",
                        left + 8, y, TEXT_SOFT, false);
                y += 12;
            }
            fontRenderer.drawString("Balde de água: +1000 mB.", left + 8, y, TEXT_SOFT, false);
            y += 12;
            fontRenderer.drawString("Urânio na mão: abastece.", left + 8, y, TEXT_SOFT, false);
        } else if (tile instanceof TileDigitalMine) {
            TileDigitalMine mine = (TileDigitalMine) tile;
            if (!mine.isStructureComplete()) {
                fontRenderer.drawString("Estrutura incompleta:", left + 8, y, STATUS_WARN, false);
                y += 11;
                fontRenderer.drawString("monte 3x2x3 deste bloco.", left + 8, y, STATUS_WARN, false);
                y += 12;
            } else if (!mine.isController()) {
                fontRenderer.drawString("Parte da estrutura 3x2x3.", left + 8, y, TEXT_SOFT, false);
                y += 11;
                fontRenderer.drawString("O controlador opera a máquina.", left + 8, y, TEXT_SOFT, false);
                y += 12;
            } else {
                if (mine.isOutputFull()) {
                    fontRenderer.drawString("Saída cheia: retire itens.", left + 8, y, STATUS_WARN, false);
                } else if (mine.isFinished()) {
                    fontRenderer.drawString("Escavação concluída", left + 8, y, STATUS_OK, false);
                } else {
                    fontRenderer.drawString("Progresso: " + mine.getProgress() + " / " + TileDigitalMine.mineVolume(),
                            left + 8, y, TEXT, false);
                }
                y += 12;
                fontRenderer.drawString("Área: 64x128x64 abaixo.", left + 8, y, TEXT_SOFT, false);
                y += 12;
                List<String> filterNames = mine.getFilterNames();
                List<String> shown = filterNames.size() > 2 ? filterNames.subList(0, 2) : filterNames;
                String filter = filterNames.isEmpty() ? "Filtro: vazio (minérios)"
                        : "Filtro: " + String.join(", ", shown)
                        + (filterNames.size() > 2 ? " +" + (filterNames.size() - 2) : "");
                fontRenderer.drawString(filter, left + 8, y, TEXT_SOFT, false);
                y += 12;
            }
            fontRenderer.drawString("Item na mão = filtro.", left + 8, y, TEXT_SOFT, false);
            y += 12;
            fontRenderer.drawString("Agachando: retirar/limpar.", left + 8, y, TEXT_SOFT, false);
        }
        String esc = "ESC para fechar";
        fontRenderer.drawString(esc, left + (PANEL_WIDTH - fontRenderer.getStringWidth(esc)) / 2,
                top + panelHeight - 14, TEXT_SOFT, false);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /** Vanilla-style beveled gray panel with a black outline. */
    private void drawPanel(int left, int top, int right, int bottom) {
        drawRect(left - 1, top - 1, right + 1, bottom + 1, OUTLINE);
        drawRect(left, top, right, bottom, BG);
        drawRect(left, top, right, top + 1, BEVEL_LIGHT);
        drawRect(left, top, left + 1, bottom, BEVEL_LIGHT);
        drawRect(left, bottom - 1, right, bottom, BEVEL_DARK);
        drawRect(right - 1, top, right, bottom, BEVEL_DARK);
    }

    /** Vanilla-style progress bar: dark edges, gray track, colored fill. */
    private void drawBar(int x, int y, int w, int value, int max, int fill) {
        drawRect(x, y, x + w, y + 4, BEVEL_DARK);
        drawRect(x + 1, y + 1, x + w - 1, y + 3, BAR_TRACK);
        if (max <= 0 || value <= 0) return;
        int amount = (int) ((long) Math.min(value, max) * (w - 2) / max);
        if (amount > 0) drawRect(x + 1, y + 1, x + 1 + amount, y + 3, fill);
    }

    /** Compact FE text so large buffers like the reactor's fit the panel. */
    private static String formatFe(int value) {
        if (value >= 1000000) return Integer.toString(value / 1000000) + "M";
        if (value >= 10000) return Integer.toString(value / 1000) + "k";
        return Integer.toString(value);
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}
