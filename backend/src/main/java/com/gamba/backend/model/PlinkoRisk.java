package com.gamba.backend.model;

// How spread out Plinko's payouts are: higher risk pays more at the edges and less in the middle
public enum PlinkoRisk {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High");

    // How the risk is named in the balance ledger
    private final String label;

    PlinkoRisk(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
