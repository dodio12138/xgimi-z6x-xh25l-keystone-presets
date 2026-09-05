package com.xgimirom.presets;

import java.util.LinkedHashMap;
import java.util.Map;

public final class UnitTests {
    private static int assertions;

    public static void main(String[] args) {
        testCoordinateParsing();
        testOffsetParsing();
        testPresetIntegrity();
        testFocusNavigation();
        testCompatibility();
        testFailureRollback();
        testRollbackFailure();
        System.out.println("OK " + assertions + " assertions");
    }

    private static void testCoordinateParsing() {
        String mode0 = coordinates(0, 0);
        check(KeystoneDataValidator.validate(mode0) == null,
                "valid mode 0 coordinates");
        check(KeystoneDataValidator.validate("0,1,2") != null,
                "reject short coordinate payload");
        check(KeystoneDataValidator.validate(coordinates(6, 0)) != null,
                "reject unsupported mode");
        check(KeystoneDataValidator.validate(coordinates(0, 40000)) != null,
                "reject coordinates outside short range");
        check(KeystoneDataValidator.sameActiveGrid(mode0, coordinates(0, 0)),
                "equal active grids match");
        check(!KeystoneDataValidator.sameActiveGrid(mode0, coordinates(1, 0)),
                "different modes do not match");
    }

    private static void testOffsetParsing() {
        int[] offsets = KeystoneOffsetParser.parse("9,1,2,3,4,5,6,7,8");
        check(offsets.length == 8 && offsets[0] == 1 && offsets[7] == 8,
                "parse checksum plus eight offsets");
        expectIllegal(() -> KeystoneOffsetParser.parse("0"), "reject zero offsets");
        expectIllegal(() -> KeystoneOffsetParser.parse("9,1,2"), "reject short offsets");
        expectIllegal(() -> KeystoneOffsetParser.parse("9,1,2,3,4,5,6,7,8,9"),
                "reject extra offsets");
    }

    private static void testPresetIntegrity() {
        ProjectionPreset valid = preset("9,1,2,3,4,5,6,7,8", 0);
        check(PresetIntegrity.validateStored(0, 1L, valid) == null,
                "accept valid legacy preset");
        check(PresetIntegrity.validateStored(PresetIntegrity.CURRENT_SCHEMA, 1L, valid)
                == null, "accept current preset schema");
        check(PresetIntegrity.validateStored(99, 1L, valid) != null,
                "reject unsupported preset schema");
        check(PresetIntegrity.validateStored(0, 0L, valid) != null,
                "reject missing save time");
        check(PresetIntegrity.validatePreset(preset(null, 0)) != null,
                "reject missing kst_ofs");
    }

    private static void testFocusNavigation() {
        boolean[] complete = {true, false, false, false, false, true};
        check(MainFocusNavigation.target(0, false, MainFocusNavigation.LEFT,
                complete, 3) == 0, "left edge does not reverse direction");
        check(MainFocusNavigation.target(5, true, MainFocusNavigation.RIGHT,
                complete, 3) == 11, "right edge does not reverse direction");
        check(MainFocusNavigation.target(0, false, MainFocusNavigation.RIGHT,
                complete, 3) == 1, "save moves to available apply action");
        check(MainFocusNavigation.target(1, false, MainFocusNavigation.RIGHT,
                complete, 3) == 4, "empty apply action is skipped");
        check(MainFocusNavigation.target(3, true, MainFocusNavigation.UP,
                complete, 3) == 1, "vertical navigation preserves action when available");
        check(MainFocusNavigation.target(0, false, MainFocusNavigation.UP,
                complete, 3) == MainFocusNavigation.SETTINGS,
                "top row moves to settings");
    }

    private static void testCompatibility() {
        check(DeviceCompatibility.isTested("XH25L", "other", "other",
                "5.0.0.30_375", ""), "recognize tested device and system");
        check(!DeviceCompatibility.isTested("XH25L", "other", "other",
                "5.0.0.29", ""), "warn for untested firmware");
        check(!DeviceCompatibility.isTested("XH26", "other", "other",
                "5.0.0.30_375", ""), "warn for untested hardware");
    }

    private static void testFailureRollback() {
        ProjectionPreset before = preset("0", 0);
        ProjectionPreset target = preset("9,1,2,3,4,5,6,7,8", 1);
        FakeDevice device = new FakeDevice(before, target, false);
        ProjectionTransaction.Result result = ProjectionTransaction.execute(device, target);
        check(!result.success, "failed apply is reported");
        check(result.rollbackAttempted && result.rollbackSucceeded,
                "failed apply rolls back");
        check(device.current == before, "rollback restores previous state");
    }

    private static void testRollbackFailure() {
        ProjectionPreset before = preset("0", 0);
        ProjectionPreset target = preset("9,1,2,3,4,5,6,7,8", 1);
        FakeDevice device = new FakeDevice(before, target, true);
        ProjectionTransaction.Result result = ProjectionTransaction.execute(device, target);
        check(!result.success && result.rollbackAttempted && !result.rollbackSucceeded,
                "rollback failure is surfaced");
        check(result.rollbackFailure != null, "rollback error is retained");
    }

    private static ProjectionPreset preset(String offsets, int coordinateValue) {
        Map<String, String> environments = new LinkedHashMap<String, String>();
        if (offsets != null) {
            environments.put("kst_ofs", offsets);
        }
        return new ProjectionPreset(coordinates(0, coordinateValue), environments);
    }

    private static String coordinates(int mode, int value) {
        StringBuilder serialized = new StringBuilder(String.valueOf(mode));
        for (int index = 0; index < 9 * 9 * 2; index++) {
            serialized.append(',').append(value);
        }
        return serialized.toString();
    }

    private static void expectIllegal(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            assertions++;
        }
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class FakeDevice implements ProjectionTransaction.Device {
        ProjectionPreset current;
        final ProjectionPreset target;
        final boolean failRollback;
        int applyCount;

        FakeDevice(ProjectionPreset current, ProjectionPreset target, boolean failRollback) {
            this.current = current;
            this.target = target;
            this.failRollback = failRollback;
        }

        @Override
        public ProjectionPreset capture() {
            return current;
        }

        @Override
        public void apply(ProjectionPreset preset) throws Exception {
            applyCount++;
            current = preset;
            if (preset == target && applyCount == 1) {
                throw new Exception("simulated partial apply");
            }
            if (failRollback && applyCount > 1) {
                throw new Exception("simulated rollback failure");
            }
        }

        @Override
        public void verify(ProjectionPreset preset) throws Exception {
            if (current != preset) {
                throw new Exception("state mismatch");
            }
        }
    }
}
