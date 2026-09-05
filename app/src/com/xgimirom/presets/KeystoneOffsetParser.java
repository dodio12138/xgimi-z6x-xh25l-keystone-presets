package com.xgimirom.presets;

final class KeystoneOffsetParser {
    private KeystoneOffsetParser() {
    }

    static int[] parse(String value) {
        if (value == null || value.trim().length() == 0 || "0".equals(value.trim())) {
            throw new IllegalArgumentException("预设 kst_ofs 为空");
        }
        String[] raw = value.split(",", -1);
        int[] parsed = new int[9];
        int count = 0;
        for (String item : raw) {
            String trimmed = item.trim();
            if (trimmed.length() == 0) {
                continue;
            }
            if (count >= parsed.length) {
                throw new IllegalArgumentException("kst_ofs 字段数量过多");
            }
            try {
                parsed[count] = Integer.parseInt(trimmed);
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("kst_ofs 含有无效整数: " + trimmed);
            }
            count++;
        }
        if (count != parsed.length) {
            throw new IllegalArgumentException("kst_ofs 不是校验值加 8 个角点");
        }
        int[] offsets = new int[8];
        System.arraycopy(parsed, 1, offsets, 0, offsets.length);
        return offsets;
    }
}
