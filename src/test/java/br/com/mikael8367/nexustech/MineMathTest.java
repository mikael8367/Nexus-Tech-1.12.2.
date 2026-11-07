package br.com.mikael8367.nexustech;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class MineMathTest {
    @Test public void volumeMatchesRequestedThreeByTwoByThreeDimensions() {
        assertEquals(3, MineMath.MINE_WIDTH);
        assertEquals(2, MineMath.MINE_HEIGHT);
        assertEquals(3, MineMath.MINE_DEPTH);
        assertEquals(18, MineMath.VOLUME);
    }
    @Test public void offsetsStayInsideTheThreeByTwoByThreeBox() {
        for (int i = 0; i < MineMath.VOLUME; i++) {
            int[] o = MineMath.offsets(i);
            assertTrue("dx out of range at " + i, o[0] >= -1 && o[0] <= 1);
            assertTrue("dy out of range at " + i, o[1] >= -2 && o[1] <= -1);
            assertTrue("dz out of range at " + i, o[2] >= -1 && o[2] <= 1);
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
        assertEquals(-2, MineMath.offsets(MineMath.VOLUME - 1)[1]);
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
        assertEquals(MineMath.VOLUME, MineMath.clampCursor(999));
        assertEquals(0L, MineMath.energyFor(0, 50L));
        assertEquals(900L, MineMath.energyFor(18, 50L));
        assertEquals(Long.MAX_VALUE, MineMath.energyFor(Integer.MAX_VALUE, Long.MAX_VALUE));
    }
}
