package br.com.mikael8367.nexustech;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class MineMathTest {
    @Test public void structureIsThreeByTwoByThree() {
        assertEquals(3, MineMath.STRUCT_WIDTH);
        assertEquals(2, MineMath.STRUCT_HEIGHT);
        assertEquals(3, MineMath.STRUCT_DEPTH);
        assertEquals(18, MineMath.STRUCT_VOLUME);
    }
    @Test public void excavationVolumeMatchesRequestedDimensions() {
        assertEquals(64, MineMath.AREA_WIDTH);
        assertEquals(128, MineMath.AREA_HEIGHT);
        assertEquals(64, MineMath.AREA_DEPTH);
        assertEquals(64 * 128 * 64, MineMath.VOLUME);
    }
    @Test public void offsetsStayInsideTheExcavationBox() {
        for (int i = 0; i < MineMath.VOLUME; i++) {
            int[] o = MineMath.offsets(i);
            assertTrue("dx out of range at " + i, o[0] >= -32 && o[0] <= 31);
            assertTrue("dy out of range at " + i, o[1] >= -128 && o[1] <= -1);
            assertTrue("dz out of range at " + i, o[2] >= -32 && o[2] <= 31);
        }
    }
    @Test public void offsetsCoverEveryCellExactlyOnceTopLayerFirst() {
        Set<String> seen = new HashSet<String>();
        for (int i = 0; i < MineMath.VOLUME; i++) {
            int[] o = MineMath.offsets(i);
            seen.add(o[0] + "," + o[1] + "," + o[2]);
        }
        assertEquals(MineMath.VOLUME, seen.size());
        assertEquals(-1, MineMath.offsets(0)[1]);
        assertEquals(-128, MineMath.offsets(MineMath.VOLUME - 1)[1]);
    }
    @Test public void offsetsRejectsOutOfRangeIndex() {
        try {
            MineMath.offsets(MineMath.VOLUME);
            fail("expected IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) { }
    }
    @Test public void cursorClampAndEnergyMathAreSafe() {
        assertEquals(0, MineMath.clampCursor(-5));
        assertEquals(5, MineMath.clampCursor(5));
        assertEquals(MineMath.VOLUME, MineMath.clampCursor(999999));
        assertEquals(0L, MineMath.energyFor(0, 50L));
        assertEquals(900L, MineMath.energyFor(18, 50L));
        assertEquals(26214400L, MineMath.energyFor(524288, 50L));
        assertEquals(Long.MAX_VALUE, MineMath.energyFor(Integer.MAX_VALUE, Long.MAX_VALUE));
    }
}
