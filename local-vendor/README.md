# 本地私有依赖目录

本目录只用于放置从**你自己拥有的 XH25L 设备或合法取得的对应固件**中提取的极米私有文件。它们受原厂版权约束，不属于本项目的开源内容，也不会提交到 GitHub。

构建正式 APK 前，需要在此目录准备：

- `libdisplaymanager_jni.xgimi.so`
- `libgmpfdisplaymanager_hidl.xgimi.so`
- `setting_classes.dex`

请勿从不明来源下载这些文件，也不要使用其他硬件型号的文件混合构建。
