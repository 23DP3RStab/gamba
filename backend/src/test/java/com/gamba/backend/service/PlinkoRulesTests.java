package com.gamba.backend.service;

import com.gamba.backend.model.PlinkoRisk;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlinkoRulesTests {

    private static BigDecimal x(String value) {
        return new BigDecimal(value);
    }

    @Test
    void everyTableHas13SlotsThatMirrorEachOther() {
        for (PlinkoRisk risk : PlinkoRisk.values()) {
            List<BigDecimal> slots = PlinkoRules.MULTIPLIERS.get(risk);
            assertEquals(13, slots.size(), risk.name());
            for (int i = 0; i < slots.size(); i++) {
                assertEquals(slots.get(i), slots.get(12 - i), risk + " slot " + i);
            }
        }
    }

    @Test
    void theEdgesPayTheMostAndTheMiddleTheLeast() {
        assertEquals(x("10.00"), PlinkoRules.MULTIPLIERS.get(PlinkoRisk.LOW).get(0));
        assertEquals(x("0.50"), PlinkoRules.MULTIPLIERS.get(PlinkoRisk.LOW).get(6));
        assertEquals(x("33.00"), PlinkoRules.MULTIPLIERS.get(PlinkoRisk.MEDIUM).get(12));
        assertEquals(x("170.00"), PlinkoRules.MULTIPLIERS.get(PlinkoRisk.HIGH).get(0));
        assertEquals(x("0.20"), PlinkoRules.MULTIPLIERS.get(PlinkoRisk.HIGH).get(6));
    }

    @Test
    void everyRiskLevelPaysBackAbout99Percent() {
        for (PlinkoRisk risk : PlinkoRisk.values()) {
            double rtp = PlinkoRules.returnToPlayer(risk);
            assertTrue(rtp > 0.98 && rtp < 0.995, risk + " pays back " + rtp);
        }
    }

    @Test
    void theSlotIsHowOftenTheBallWentRight() {
        assertEquals(0, PlinkoRules.slotOf("LLLLLLLLLLLL"));
        assertEquals(12, PlinkoRules.slotOf("RRRRRRRRRRRR"));
        assertEquals(6, PlinkoRules.slotOf("LRLRLRLRLRLR"));
        assertEquals(3, PlinkoRules.slotOf("RRRLLLLLLLLL"));
        assertThrows(IllegalArgumentException.class, () -> PlinkoRules.slotOf("LR"));
        assertThrows(IllegalArgumentException.class, () -> PlinkoRules.slotOf("LLLLLLLLLLLX"));
        assertThrows(IllegalArgumentException.class, () -> PlinkoRules.slotOf(null));
    }

    @Test
    void payoutsAreRoundedDownToCents() {
        assertEquals(x("0.04"), PlinkoRules.payout(x("0.15"), x("0.30")));
        assertEquals(x("170.00"), PlinkoRules.payout(x("1.00"), x("170.00")));
        assertEquals(x("8.91"), PlinkoRules.payout(x("1.10"), x("8.10")));
    }
}
