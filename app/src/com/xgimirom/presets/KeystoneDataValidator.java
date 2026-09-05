package com.xgimirom.presets;

final class KeystoneDataValidator {
    private static final int GRID = 9;
    private static final int FIELD_COUNT = 1 + GRID * GRID * 2;

    private KeystoneDataValidator() {
    }

    static String validate(String serialized) {
        if (serialized == null || serialized.trim().length() == 0) {
            return "校正坐标为空";
        }
        String[] fields = serialized.split(",", -1);
        if (fields.length != FIELD_COUNT) {
            return "校正坐标字段数量错误";
        }
        Integer mode = parseInteger(fields[0]);
        if (mode == null || mode < 0 || mode > 5) {
            return "校正模式无效";
        }
        for (int index = 1; index < fields.length; index++) {
            Integer value = parseInteger(fields[index]);
            if (value == null || value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
                return "校正坐标包含无效数值";
            }
        }
        return null;
    }

    static boolean sameActiveGrid(String left, String right) {
        if (validate(left) != null || validate(right) != null) {
            return false;
        }
        String[] leftFields = left.split(",", -1);
        String[] rightFields = right.split(",", -1);
        int mode = Integer.parseInt(leftFields[0].trim());
        if (mode != Integer.parseInt(rightFields[0].trim())) {
            return false;
        }
        int[] size = activeSize(mode);
        for (int row = 0; row < size[0]; row++) {
            for (int column = 0; column < size[1]; column++) {
                int index = 1 + (row * GRID + column) * 2;
                if (!leftFields[index].trim().equals(rightFields[index].trim())
                        || !leftFields[index + 1].trim().equals(
                                rightFields[index + 1].trim())) {
                    return false;
                }
            }
        }
        return true;
    }

    static int[] activeSize(int mode) {
        switch (mode) {
            case 0: return new int[]{2, 2};
            case 1: return new int[]{3, 3};
            case 2: return new int[]{3, 5};
            case 3: return new int[]{5, 3};
            case 4: return new int[]{5, 5};
            case 5: return new int[]{9, 9};
            default: throw new IllegalArgumentException("不支持的校正模式: " + mode);
        }
    }

    private static Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException error) {
            return null;
        }
    }
}
