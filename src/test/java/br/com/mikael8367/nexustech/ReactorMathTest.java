package br.com.mikael8367.nexustech;

import org.junit.Test;
import static org.junit.Assert.*;

/** Unit coverage for the pure reactor math: heat balance, meltdown timing and explosion scale. */
public class ReactorMathTest {
    @Test public void clampsToValidRange() {
        assertEquals(0, ReactorMath.clamp(-5, ReactorMath.MAX_HEAT));
        assertEquals(ReactorMath.MAX_HEAT, ReactorMath.clamp(5000, ReactorMath.MAX_HEAT));
        assertEquals(400, ReactorMath.clamp(400, ReactorMath.MAX_HEAT));
    }

    @Test public void structureIsFortyFiveBlocks() {
        assertEquals(45, ReactorMath.STRUCT_VOLUME);
        assertEquals(3, ReactorMath.STRUCT_WIDTH);
        assertEquals(5, ReactorMath.STRUCT_HEIGHT);
        assertEquals(3, ReactorMath.STRUCT_DEPTH);
    }

    @Test public void burningWithoutCoolingHeatsUp() {
        assertTrue(ReactorMath.heatDelta(true, false, false, 0) > 0);
        assertTrue(ReactorMath.heatDelta(false, false, false, 0) < 0);
    }

    @Test public void waterCoolsABurningReactor() {
        assertTrue(ReactorMath.heatDelta(true, false, true, 0) < 0);
    }

    @Test public void iceCoolingIsCapped() {
        assertEquals(ReactorMath.heatDelta(false, false, false, 8),
                ReactorMath.heatDelta(false, false, false, 64));
    }

    @Test public void meltdownHeatsUpEvenIdleAndIgnoresAmbientCooling() {
        assertTrue(ReactorMath.heatDelta(false, true, false, 0) > 0);
        assertTrue(ReactorMath.heatDelta(false, false, false, 0) < ReactorMath.heatDelta(false, true, false, 0));
    }

    @Test public void activeCoolingCanBeatTheMeltdownRunaway() {
        assertTrue(ReactorMath.heatDelta(false, true, true, 10) < 0);
        assertTrue(ReactorMath.heatDelta(false, true, false, 16) < 0);
    }

    @Test public void meltdownTimingAndRescueThreshold() {
        assertEquals(60, ReactorMath.MELTDOWN_TICKS / 20);
        assertEquals(400, ReactorMath.MELTDOWN_CANCEL_HEAT);
    }

    @Test public void explosionMatchesOneHundredTnts() {
        assertEquals(100, ReactorMath.EXPLOSION_COUNT);
        assertEquals(4.0F, ReactorMath.TNT_POWER, 0.0F);
    }

    @Test public void iceMeltsOnlyWhenHotAndOnInterval() {
        assertTrue(ReactorMath.iceMelts(ReactorMath.ICE_MELT_THRESHOLD, 60L));
        assertFalse(ReactorMath.iceMelts(ReactorMath.ICE_MELT_THRESHOLD - 1, 60L));
        assertFalse(ReactorMath.iceMelts(ReactorMath.ICE_MELT_THRESHOLD, 61L));
    }

    @Test public void waterEvaporatesFasterDuringMeltdown() {
        assertTrue(ReactorMath.waterUseMb(true) > ReactorMath.waterUseMb(false));
    }

    @Test public void oneRodBurnsTenMinutesAtFullRate() {
        assertEquals(12000, ReactorMath.ROD_TICKS);
        assertEquals(4096, ReactorMath.FE_PER_TICK);
    }
}
