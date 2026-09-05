package com.xgimi.gmpf.api;

import com.xgimi.gmpf.rp.KeyStoneFullCoordinates;
import com.xgimi.gmpf.rp.KeyStonePoint;

/** Emulator-only fake of the private GMPF API used by app reflection. */
public final class DisplayManager {
    private static final DisplayManager INSTANCE = new DisplayManager();
    private final KeyStoneFullCoordinates current = new KeyStoneFullCoordinates();

    private DisplayManager() {
        current.kstMode = 0;
    }

    public static DisplayManager getInstance() {
        return INSTANCE;
    }

    public void getCorrectKeystone(KeyStoneFullCoordinates target) {
        copy(current, target);
    }

    public void correctKeystone(KeyStoneFullCoordinates source) {
        copy(source, current);
    }

    public boolean checkTrapezoidCoordinate(KeyStoneFullCoordinates source) {
        return source != null && source.kstMode >= 0 && source.kstMode <= 5
                && source.coordinates != null && source.coordinates.length == 9;
    }

    private static void copy(KeyStoneFullCoordinates source, KeyStoneFullCoordinates target) {
        target.kstMode = source.kstMode;
        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                KeyStonePoint sourcePoint = source.coordinates[row][column];
                KeyStonePoint targetPoint = target.coordinates[row][column];
                targetPoint.x = sourcePoint.x;
                targetPoint.y = sourcePoint.y;
            }
        }
    }
}
