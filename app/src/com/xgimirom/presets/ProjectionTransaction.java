package com.xgimirom.presets;

final class ProjectionTransaction {
    interface Device {
        ProjectionPreset capture() throws Exception;
        void apply(ProjectionPreset preset) throws Exception;
        void verify(ProjectionPreset preset) throws Exception;
    }

    static final class Result {
        final boolean success;
        final Exception failure;
        final boolean rollbackAttempted;
        final boolean rollbackSucceeded;
        final Exception rollbackFailure;

        Result(boolean success, Exception failure, boolean rollbackAttempted,
                boolean rollbackSucceeded, Exception rollbackFailure) {
            this.success = success;
            this.failure = failure;
            this.rollbackAttempted = rollbackAttempted;
            this.rollbackSucceeded = rollbackSucceeded;
            this.rollbackFailure = rollbackFailure;
        }
    }

    private ProjectionTransaction() {
    }

    static Result execute(Device device, ProjectionPreset target) {
        String targetError = PresetIntegrity.validatePreset(target);
        if (targetError != null) {
            return new Result(false, new IllegalArgumentException(targetError),
                    false, false, null);
        }

        final ProjectionPreset before;
        try {
            before = device.capture();
            String beforeError = before == null
                    ? "当前画面数据为空"
                    : KeystoneDataValidator.validate(before.fullCoordinates);
            if (beforeError != null) {
                throw new IllegalStateException("无法备份当前画面: " + beforeError);
            }
        } catch (Exception error) {
            return new Result(false, error, false, false, null);
        }

        try {
            device.apply(target);
            device.verify(target);
            return new Result(true, null, false, false, null);
        } catch (Exception failure) {
            try {
                device.apply(before);
                device.verify(before);
                return new Result(false, failure, true, true, null);
            } catch (Exception rollbackFailure) {
                return new Result(false, failure, true, false, rollbackFailure);
            }
        }
    }
}
