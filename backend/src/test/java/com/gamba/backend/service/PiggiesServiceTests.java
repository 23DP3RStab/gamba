package com.gamba.backend.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class PiggiesServiceTests {

    @Test
    void housesPayTheSameMultipliersAsTheFrontendShows() {
        assertArrayEquals(
                new BigDecimal[]{new BigDecimal("2.00"), new BigDecimal("5.00"), new BigDecimal("16.67")},
                PiggiesService.MULTIPLIERS);
    }
}
