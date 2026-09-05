package com.xgimi.gmpf.rp;

public final class KeyStoneFullCoordinates {
    public byte kstMode;
    public final KeyStonePoint[][] coordinates = new KeyStonePoint[9][9];

    public KeyStoneFullCoordinates() {
        for (int row = 0; row < coordinates.length; row++) {
            for (int column = 0; column < coordinates[row].length; column++) {
                coordinates[row][column] = new KeyStonePoint(
                        (short) (100 + row * 10 + column),
                        (short) (200 + row * 10 + column));
            }
        }
    }
}
