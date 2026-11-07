package br.com.mikael8367.nexustech;
import org.junit.Test;
import static org.junit.Assert.*;

public class EnergyBufferTest {
    @Test public void receiveRespectsRateCapacityAndSimulation() {
        EnergyBuffer buffer = new EnergyBuffer(100, 20, 10);
        assertEquals(20, buffer.receiveEnergy(80, false));
        assertEquals(20, buffer.getEnergyStored());
        assertEquals(20, buffer.receiveEnergy(80, true));
        assertEquals(20, buffer.getEnergyStored());
        buffer.setEnergyStored(95);
        assertEquals(5, buffer.receiveEnergy(80, false));
        assertEquals(100, buffer.getEnergyStored());
        assertFalse(buffer.canReceive());
    }
    @Test public void extractRespectsStoredAmountAndRate() {
        EnergyBuffer buffer = new EnergyBuffer(100, 10, 15);
        buffer.addInternal(80);
        assertEquals(15, buffer.extractEnergy(50, false));
        assertEquals(65, buffer.getEnergyStored());
    }
    @Test public void generatorBufferRejectsExternalInput() {
        EnergyBuffer buffer = new EnergyBuffer(100, 0, 10);
        assertEquals(0, buffer.receiveEnergy(20, false));
        assertEquals(10, buffer.addInternal(10));
        assertFalse(buffer.canReceive());
        assertTrue(buffer.canExtract());
    }
    @Test public void internalSpendBypassesExternalExtractLimit() {
        EnergyBuffer buffer = new EnergyBuffer(100, 10, 0);
        buffer.addInternal(50);
        assertEquals(0, buffer.extractEnergy(10, false));
        assertEquals(50, buffer.getEnergyStored());
        assertEquals(10, buffer.spendInternal(10));
        assertEquals(40, buffer.getEnergyStored());
        assertEquals(40, buffer.spendInternal(100));
        assertEquals(0, buffer.getEnergyStored());
        assertEquals(0, buffer.spendInternal(5));
    }
    @Test public void invalidAmountsDoNotChangeEnergy() {
        EnergyBuffer buffer = new EnergyBuffer(100, 10, 10);
        assertEquals(0, buffer.receiveEnergy(-1, false));
        assertEquals(0, buffer.extractEnergy(-1, false));
        assertEquals(0, buffer.addInternal(-10));
        assertEquals(0, buffer.getEnergyStored());
    }
}
