package com.bashvollink.curtaindoors.block;

/** The three curtain families. Each has its own cloth topology, look and sound. */
public enum CurtainType {
    /** Two soft fabric panels that part in the middle. 16 dye colors. */
    WOOL(true),
    /** Overlapping translucent PVC strips, like a walk-in freezer door. */
    PLASTIC(false),
    /** Independent strands of beads. 16 dye colors. */
    BEADED(true);

    private final boolean dyed;

    CurtainType(boolean dyed) {
        this.dyed = dyed;
    }

    public boolean isDyed() {
        return dyed;
    }
}
