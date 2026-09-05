# 岁记日历

一款离线优先的原生 Android 小应用，用同一条记录同时追踪公历和农历纪念日，并把倒数日、每日清单和桌面组件放在一起。

## 功能

- 月历同时显示公历日期与农历名称，支持 1901—2100 年。
- 生日、忌日、纪念日和自定义事件支持新增、编辑与删除。
- 一条纪念记录可同时启用公历、农历两条年度轨道；同日时格子只显示一个事件标记，详情保留两条依据。
- 闰月年份缺失时回退到同名普通月；农历三十不存在、公历 2 月 29 日或月末 31 日不存在时顺延到当月最后一天，并显示“本年日期已调整”。
- 每个日期都能记录清单，支持不重复、每天、每周指定星期、每月和每年。
- 未完成事项默认永久留在原日期；“移到今天”会保留原记录并创建有关联的新记录。
- 倒数日使用卡片展示，同一人物可同时看到下一次公历和农历日期；自定义日期支持倒数或累计。
- 提供倒数日组件和今日清单组件，今日组件可直接切换完成状态。
- 纪念日默认提前 7 天及当天 09:00 提醒，清单提醒默认关闭。提醒由 WorkManager 调度，可能受系统省电策略影响而延迟。
- 通过系统文件选择器导入、导出版本化 JSON；导入前预览数量，可安全合并或确认后覆盖恢复。

## 隐私

应用默认完全离线，不申请联网、联系人、系统日历或存储权限，不包含统计和追踪 SDK。数据保存在本机 Room 数据库中；请主动导出备份以防设备丢失。

## 技术栈

- Kotlin 2.3、Jetpack Compose、Material 3、Navigation Compose
- Room 2.8.4、DataStore、WorkManager、Glance AppWidget
- [Kizitonwose Calendar 2.10.1](https://github.com/kizitonwose/Calendar)
- [tyme4kt 1.5.0](https://github.com/6tail/tyme4kt)
- minSdk 26，compileSdk / targetSdk 36，JDK 17

## 本地构建

1. 安装 JDK 17、Android SDK Platform 36 与 Build Tools 36.0.0。
2. 克隆仓库并运行：

```bash
git clone https://github.com/jimzhou03/suiji-calendar-android.git
cd suiji-calendar-android
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Windows PowerShell 使用 `./gradlew.bat`。Debug APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 核心数据规则

- 输入公历日期时保存对应农历日期；输入农历日期时反算公历日期。
- 周期清单只记录规则，单次完成、取消完成和迁移状态保存在 `TaskOccurrence`，不会影响其他周期。
- 每月 31 日、每年 2 月 29 日等在目标月份不存在时取当月最后一天。
- 覆盖恢复在单个数据库事务内完成；损坏文件和未知格式版本会在写入前被拒绝。

## 验证

核心单元测试覆盖：

- `2003-06-30 = 农历六月初一`
- 2026 年公历生日 `2026-06-30` 与农历生日 `2026-07-14`
- 闰月回退、农历三十、公历 2 月 29 日、每月 31 日
- 重复任务单次完成隔离，以及“移到今天”的双记录关联
- JSON 往返、损坏文件与未知版本

GitHub Actions 会在每次推送执行单元测试、Lint、Debug 构建与 `git diff --check`，并上传 Debug APK 工件。

当前不提供 Google Play 包或正式 GitHub Release。请仅使用虚构资料测试；真机验收完成后再单独准备发行版。

## 提交规范

标题格式：`<类型>(<中文范围>): <动词开头主题><Emoji>`，不超过 50 字。例如：

```text
feat(清单): 新增每日任务与迁移功能🎉
fix(日历): 修正闰月日期回退逻辑🐛
```

复杂改动可在正文说明原因与方案；Issue 或破坏性变更写在 Footer。

## 许可证

本项目使用 [Apache License 2.0](LICENSE)。第三方依赖遵循各自许可证。
