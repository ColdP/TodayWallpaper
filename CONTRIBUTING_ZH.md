# 贡献指南

感谢你对 Today Wallpaper 的关注！请阅读以下指南，让协作更加顺畅。

## 开始之前

1. **Fork** 本仓库到你的 GitHub 账号。
2. **克隆**你的 Fork 到本地：
   ```bash
   git clone https://github.com/YOUR_USERNAME/TodayWallpaper.git
   cd TodayWallpaper
   ```
3. 为你的改动创建一个新分支：
   ```bash
   git checkout -b feat/你的功能名称
   ```

## 开发环境

- Android Studio Narwhal (2025.1) 或更高版本
- JDK 11+
- Android SDK 37

在 Android Studio 中打开项目，等待 Gradle 同步完成后即可开始构建。

## 提交规范

- 每个提交只包含一个逻辑改动。
- 使用简洁的英文提交信息，推荐格式：
  ```
  feat: 添加深色模式切换
  fix: 修复 API 超时时的崩溃
  refactor: 提取 WallpaperRepository
  ```
- 遵循项目现有代码风格（项目已启用 Kotlin 官方代码风格 `kotlin.code.style=official`）。

## 提交 Pull Request

1. 将你的分支推送到你的 Fork：
   ```bash
   git push origin feat/你的功能名称
   ```
2. 在 GitHub 上向本仓库的 `main` 分支发起 **Pull Request**。
3. 在 PR 描述中说明：
   - 这个改动做了什么
   - 如有 UI 变化，请附上截图
   - 测试步骤

对于较大的改动，建议**先开 Issue 讨论方案**，再动手编写代码。

## 反馈 Bug

提交 Bug 报告时，请尽量包含以下信息：

- 设备型号及 Android 版本
- 应用版本号
- 复现步骤
- 预期行为与实际行为
- 如有可能，附上 Logcat 日志

## 行为准则

请保持友善和建设性的沟通态度，任何形式的骚扰行为都不被接受。

---

*感谢你帮助 Today Wallpaper 变得更好！*
