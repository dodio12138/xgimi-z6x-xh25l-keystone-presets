package com.xgimirom.presets;

final class MainFocusNavigation {
    static final int SETTINGS = -1;
    static final int LEFT = 0;
    static final int RIGHT = 1;
    static final int UP = 2;
    static final int DOWN = 3;

    private MainFocusNavigation() {
    }

    static int target(int slot, boolean restore, int direction, boolean[] complete,
            int columns) {
        int rows = complete.length / columns;
        int row = slot / columns;
        int column = slot % columns;
        int current = buttonIndex(slot, restore);

        if (direction == UP) {
            if (row == 0) {
                return SETTINGS;
            }
            return preferredButton(slot - columns, restore, complete);
        }
        if (direction == DOWN) {
            if (row >= rows - 1) {
                return current;
            }
            return preferredButton(slot + columns, restore, complete);
        }
        if (direction == LEFT) {
            if (restore) {
                return buttonIndex(slot, false);
            }
            if (column == 0) {
                return current;
            }
            return preferredButton(slot - 1, true, complete);
        }
        if (direction == RIGHT) {
            if (!restore && complete[slot]) {
                return buttonIndex(slot, true);
            }
            if (column >= columns - 1) {
                return current;
            }
            return buttonIndex(slot + 1, false);
        }
        return current;
    }

    private static int preferredButton(int slot, boolean restore, boolean[] complete) {
        return buttonIndex(slot, restore && complete[slot]);
    }

    private static int buttonIndex(int slot, boolean restore) {
        return slot * 2 + (restore ? 1 : 0);
    }
}
