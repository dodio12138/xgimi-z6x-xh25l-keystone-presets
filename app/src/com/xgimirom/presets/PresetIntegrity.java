package com.xgimirom.presets;

final class PresetIntegrity {
    static final int CURRENT_SCHEMA = 1;

    private PresetIntegrity() {
    }

    static String validateStored(int schema, long savedAt, ProjectionPreset preset) {
        if (savedAt <= 0L) {
            return "缺少保存时间";
        }
        if (schema != 0 && schema != CURRENT_SCHEMA) {
            return "预设数据版本不受支持";
        }
        return validatePreset(preset);
    }

    static String validatePreset(ProjectionPreset preset) {
        if (preset == null) {
            return "预设数据为空";
        }
        String coordinateError = KeystoneDataValidator.validate(preset.fullCoordinates);
        if (coordinateError != null) {
            return coordinateError;
        }
        String offsets = preset.environments.get("kst_ofs");
        try {
            KeystoneOffsetParser.parse(offsets);
        } catch (IllegalArgumentException error) {
            return error.getMessage();
        }
        return null;
    }
}
