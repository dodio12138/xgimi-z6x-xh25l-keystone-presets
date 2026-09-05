package com.xgimirom.presets;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Calls the same full-coordinate GMPF path used by the firmware UI. */
final class GmpfKeystoneBridge {
    private static final String DISPLAY_MANAGER = "com.xgimi.gmpf.api.DisplayManager";
    private static final String FULL_COORDINATES =
            "com.xgimi.gmpf.rp.KeyStoneFullCoordinates";
    private static final String POINT = "com.xgimi.gmpf.rp.KeyStonePoint";
    private static final int GRID = 9;

    static final class ApplyResult {
        final boolean targetWasValid;
        final boolean matches;
        final String requestedCorners;
        final String actualCorners;

        ApplyResult(boolean targetWasValid, boolean matches,
                String requestedCorners, String actualCorners) {
            this.targetWasValid = targetWasValid;
            this.matches = matches;
            this.requestedCorners = requestedCorners;
            this.actualCorners = actualCorners;
        }
    }

    String capture() {
        try {
            Api api = new Api();
            Object coordinates = api.newFullCoordinates((byte) 0);
            api.readMethod.invoke(api.manager, coordinates);
            return encode(api, coordinates);
        } catch (InvocationTargetException error) {
            throw nativeFailure(error);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("校正接口装载失败: " + describe(error), error);
        } catch (LinkageError error) {
            throw new IllegalStateException("校正原生库装载失败: " + describe(error), error);
        }
    }

    ApplyResult apply(String serialized) {
        try {
            Api api = new Api();
            Object requested = decode(api, serialized);
            boolean valid = ((Boolean) api.checkMethod.invoke(api.manager, requested))
                    .booleanValue();
            if (!valid) {
                return new ApplyResult(
                        false, false, corners(api, requested), "未提交（坐标校验失败）");
            }

            // This is the exact call sequence used by KeyStoneManager in the
            // stock manual-keystone UI. correctKeystoneEx is a different API
            // and returns -1 for this four-point workflow on XH25L.
            api.applyMethod.invoke(api.manager, requested);

            Object actual = null;
            boolean matches = false;
            byte mode = api.modeField.getByte(requested);
            for (int attempt = 0; attempt < 8; attempt++) {
                actual = api.newFullCoordinates(mode);
                api.readMethod.invoke(api.manager, actual);
                matches = sameActiveGrid(api, requested, actual);
                if (matches) {
                    break;
                }
                if (attempt < 7) {
                    Thread.sleep(150L);
                }
            }
            return new ApplyResult(
                    true, matches, corners(api, requested), corners(api, actual));
        } catch (InvocationTargetException error) {
            throw nativeFailure(error);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("校正接口装载失败: " + describe(error), error);
        } catch (LinkageError error) {
            throw new IllegalStateException("校正原生库装载失败: " + describe(error), error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("校正硬件回读被中断", error);
        }
    }

    private String encode(Api api, Object full) throws ReflectiveOperationException {
        StringBuilder value = new StringBuilder();
        value.append((int) api.modeField.getByte(full));
        Object rows = api.coordinatesField.get(full);
        for (int row = 0; row < GRID; row++) {
            Object columns = Array.get(rows, row);
            for (int column = 0; column < GRID; column++) {
                Object point = Array.get(columns, column);
                value.append(',').append((int) api.xField.getShort(point));
                value.append(',').append((int) api.yField.getShort(point));
            }
        }
        return value.toString();
    }

    private Object decode(Api api, String value) throws ReflectiveOperationException {
        if (value == null || value.length() == 0) {
            throw new IllegalArgumentException("此槽位没有可用的校正数据");
        }
        String[] fields = value.split(",", -1);
        int expected = 1 + GRID * GRID * 2;
        if (fields.length != expected) {
            throw new IllegalArgumentException(
                    "校正数据字段数量错误：需要 " + expected + "，实际 " + fields.length);
        }
        int mode = parse(fields[0], "模式");
        if (mode < 0 || mode > 5) {
            throw new IllegalArgumentException("不支持的校正模式: " + mode);
        }
        Object full = api.newFullCoordinates((byte) mode);
        Object rows = api.coordinatesField.get(full);
        int index = 1;
        for (int row = 0; row < GRID; row++) {
            Object columns = Array.get(rows, row);
            for (int column = 0; column < GRID; column++) {
                Object point = Array.get(columns, column);
                int x = parse(fields[index++], "x");
                int y = parse(fields[index++], "y");
                if (x < Short.MIN_VALUE || x > Short.MAX_VALUE
                        || y < Short.MIN_VALUE || y > Short.MAX_VALUE) {
                    throw new IllegalArgumentException("校正坐标超出有效范围");
                }
                api.xField.setShort(point, (short) x);
                api.yField.setShort(point, (short) y);
            }
        }
        return full;
    }

    private boolean sameActiveGrid(Api api, Object left, Object right)
            throws ReflectiveOperationException {
        byte mode = api.modeField.getByte(left);
        if (mode != api.modeField.getByte(right)) {
            return false;
        }
        int[] size = activeSize(mode);
        Object leftRows = api.coordinatesField.get(left);
        Object rightRows = api.coordinatesField.get(right);
        for (int row = 0; row < size[0]; row++) {
            Object leftColumns = Array.get(leftRows, row);
            Object rightColumns = Array.get(rightRows, row);
            for (int column = 0; column < size[1]; column++) {
                Object leftPoint = Array.get(leftColumns, column);
                Object rightPoint = Array.get(rightColumns, column);
                if (api.xField.getShort(leftPoint) != api.xField.getShort(rightPoint)
                        || api.yField.getShort(leftPoint) != api.yField.getShort(rightPoint)) {
                    return false;
                }
            }
        }
        return true;
    }

    private String corners(Api api, Object full) throws ReflectiveOperationException {
        byte mode = api.modeField.getByte(full);
        int[] size = activeSize(mode);
        int bottom = size[0] - 1;
        int right = size[1] - 1;
        Object rows = api.coordinatesField.get(full);
        return "模式" + ((int) mode)
                + " TL" + pointText(api, rows, 0, 0)
                + " TR" + pointText(api, rows, 0, right)
                + " BL" + pointText(api, rows, bottom, 0)
                + " BR" + pointText(api, rows, bottom, right);
    }

    private String pointText(Api api, Object rows, int row, int column)
            throws ReflectiveOperationException {
        Object point = Array.get(Array.get(rows, row), column);
        return "(" + ((int) api.xField.getShort(point))
                + "," + ((int) api.yField.getShort(point)) + ")";
    }

    private int[] activeSize(byte mode) {
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

    private int parse(String value, String label) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(label + " 不是整数: " + value);
        }
    }

    private static IllegalStateException nativeFailure(InvocationTargetException error) {
        Throwable cause = error.getCause();
        Throwable shown = cause == null ? error : cause;
        return new IllegalStateException("校正原生接口异常: " + describe(shown), shown);
    }

    private static String describe(Throwable error) {
        String message = error.getMessage();
        return error.getClass().getSimpleName()
                + (message == null || message.length() == 0 ? "" : ": " + message);
    }

    private static final class Api {
        final Object manager;
        final Class<?> fullClass;
        final Field modeField;
        final Field coordinatesField;
        final Field xField;
        final Field yField;
        final Method readMethod;
        final Method applyMethod;
        final Method checkMethod;

        Api() throws ReflectiveOperationException {
            Class<?> pointClass = Class.forName(POINT);
            fullClass = Class.forName(FULL_COORDINATES);
            Class<?> managerClass = Class.forName(DISPLAY_MANAGER);
            manager = managerClass.getMethod("getInstance").invoke(null);
            modeField = fullClass.getField("kstMode");
            coordinatesField = fullClass.getField("coordinates");
            xField = pointClass.getField("x");
            yField = pointClass.getField("y");
            readMethod = managerClass.getMethod("getCorrectKeystone", fullClass);
            applyMethod = managerClass.getMethod("correctKeystone", fullClass);
            checkMethod = managerClass.getMethod("checkTrapezoidCoordinate", fullClass);
        }

        Object newFullCoordinates(byte mode) throws ReflectiveOperationException {
            Object full = fullClass.getConstructor().newInstance();
            modeField.setByte(full, mode);
            return full;
        }
    }
}
