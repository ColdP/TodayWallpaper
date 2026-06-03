# Contributing to Today Wallpaper

**[中文版](CONTRIBUTING_ZH.md)**

Thank you for your interest in contributing! Here are a few guidelines to keep things smooth.

## Getting Started

1. **Fork** the repository on GitHub.
2. **Clone** your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/TodayWallpaper.git
   cd TodayWallpaper
   ```
3. Create a new branch for your change:
   ```bash
   git checkout -b feat/your-feature-name
   ```

## Development Setup

- Android Studio Narwhal (2025.1) or later
- JDK 11+
- Android SDK 37

Open the project in Android Studio and let Gradle sync complete before building.

## Making Changes

- Keep commits focused — one logical change per commit.
- Use clear, concise commit messages in English:
  ```
  feat: add dark mode toggle
  fix: resolve crash on API timeout
  refactor: extract WallpaperRepository
  ```
- Follow the existing code style (Kotlin official code style is enforced via `kotlin.code.style=official`).

## Submitting a Pull Request

1. Push your branch to your fork:
   ```bash
   git push origin feat/your-feature-name
   ```
2. Open a **Pull Request** against the `main` branch of this repository.
3. Fill in the PR description with:
   - What the change does
   - Any relevant screenshots (for UI changes)
   - Steps to test

For significant changes, please **open an issue first** to discuss your proposal before writing code.

## Reporting Issues

When filing a bug report, please include:
- Device model and Android version
- App version
- Steps to reproduce
- Expected vs. actual behavior
- Logcat output if available

## Code of Conduct

Be respectful and constructive. Harassment of any kind will not be tolerated.

---

*Thanks for helping make Today Wallpaper better!*
