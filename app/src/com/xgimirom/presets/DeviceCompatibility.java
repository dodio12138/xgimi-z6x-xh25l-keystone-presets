package com.xgimirom.presets;

final class DeviceCompatibility {
    static final String TESTED_MODEL = "XH25L";
    static final String TESTED_SYSTEM = "5.0.0.30_375";

    private DeviceCompatibility() {
    }

    static boolean isTested(String model, String device, String product,
            String display, String incremental) {
        boolean modelMatches = contains(model, TESTED_MODEL)
                || contains(device, TESTED_MODEL)
                || contains(product, TESTED_MODEL);
        boolean systemMatches = contains(display, TESTED_SYSTEM)
                || contains(incremental, TESTED_SYSTEM);
        return modelMatches && systemMatches;
    }

    static String deviceLabel(String model, String device) {
        if (model != null && model.trim().length() > 0) {
            return model.trim();
        }
        if (device != null && device.trim().length() > 0) {
            return device.trim();
        }
        return "未知型号";
    }

    private static boolean contains(String value, String expected) {
        return value != null && value.toUpperCase().contains(expected.toUpperCase());
    }
}
