# 第一代极米 Z6X 手动校正预设

这是一个用于第一代极米 Z6X 的 Android TV 应用，可以保存手动校正数据，并在不同投影位置之间快速切换。目前只在硬件型号 `XH25L` 上测试过。

> [!WARNING]
> 其他 Z6X 版本和极米机型尚未测试，不代表一定不能使用。由于应用调用了极米系统的私有接口，请先备份重要设置再进行测试。

## 已测试环境

| 项目 | 版本 |
| --- | --- |
| 设备 | 第一代极米 Z6X |
| 硬件型号 | `XH25L` |
| 系统版本 | `5.0.0.30_375` |
| 底层版本 | `OF122V12.15.213` |
| Android | 8.0.0 / API 26 |

## 功能

- 保存和应用六个手动校正预设
- 自定义预设名称
- 显示保存时间和槽位状态
- 单独开关保存、应用前的确认窗口
- 支持 Android TV 遥控器操作
- 不需要 Root，也不会修改系统分区

## 使用

1. 在极米系统中完成手动投影校正。
2. 打开应用，选择一个槽位并点击“保存当前”。
3. 投影位置改变后，找到对应槽位并点击“应用预设”。

重复保存会覆盖该槽位原有的数据。预设名称和确认窗口可以在“设置”中修改，恢复默认设置不会删除已保存的校正数据。

## 构建

仓库不包含极米固件、原厂 APK、DEX 或 SO 文件。构建前请按照 [`local-vendor/README.md`](local-vendor/README.md) 准备以下文件：

```text
local-vendor/
├── libdisplaymanager_jni.xgimi.so
├── libgmpfdisplaymanager_hidl.xgimi.so
└── setting_classes.dex
```

安装 JDK 和 Android SDK 后运行：

```bash
export ANDROID_SDK_ROOT=/path/to/Android/sdk
export JAVA_HOME=/path/to/jdk
./scripts/build_apk.sh
```

默认使用 Android Platform `android-36.1` 和 Build Tools `36.0.0`，也可以通过 `ANDROID_PLATFORM` 和 `ANDROID_BUILD_TOOLS_VERSION` 修改。

构建结果位于 `outputs/`。其中 `.apk1` 与 `.apk` 内容相同，供会隐藏 APK 文件的投影仪文件管理器使用。

## 安装

```bash
adb install -r outputs/xh25l-keystone-presets-debug.apk
```

也可以把 `.apk1` 文件复制到 U 盘，在投影仪上使用系统安装器打开。

## 说明

- 极米系统更新后，私有接口可能发生变化。
- `tools/emulator/mock-xgimi-service/` 只用于模拟器界面测试，不能测试真实投影校正。
- 本项目不是极米官方软件。
- 项目代码使用 [MIT License](LICENSE)，该许可证不包括极米固件或原厂文件。
