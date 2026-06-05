<img src="images/github_todaywallpaper_3840x2160_header.png" alt="Today Wallpaper" width="100%">

<h1 align="center">Today Wallpaper</h1>

<p align="center">
  A calendar-style immersive wallpaper app built with Jetpack Compose + Kotlin
</p>

<p align="center">
  <a href="https://tdwp.btm-m.site">Official Site</a> ·
  <a href="README_ZH.md">中文文档</a> ·
  <a href="https://github.com/ColdP/TodayWallpaper/releases">Releases</a>
</p>

<p align="center">
  <img src="https://img.shields.io/github/v/release/ColdP/TodayWallpaper?style=flat-square" alt="Release">
  <img src="https://img.shields.io/badge/Platform-Android-green?style=flat-square&logo=android" alt="Platform">
  <img src="https://img.shields.io/badge/minSdk-24-blue?style=flat-square" alt="minSdk">
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=flat-square" alt="License">
</p>

---

## Screenshots

<p align="center">
  <img src="images/screenshot_home.png" width="30%" alt="Home">
  &nbsp;&nbsp;
  <img src="images/screenshot_themes.png" width="30%" alt="Categories">
  &nbsp;&nbsp;
  <img src="images/screenshot_mine.png" width="30%" alt="Mine">
</p>
<p align="center">
  <sub>Home &nbsp;|&nbsp; Categories &nbsp;|&nbsp; Mine</sub>
</p>

---

## Features

- **Calendar-style homepage** — Browse daily wallpapers in an immersive, date-driven layout
- **Curated categories** — Discover wallpapers organized by theme and style
- **Collections & custom albums** — Save favorites and build personal collections
- **Bilingual support** — Full Chinese / English interface
- **Custom search** — Search with custom keywords and fetch wallpapers from the Pexels API

---

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Language | Kotlin |
| Architecture | MVVM + Repository |
| Navigation | Navigation Compose |
| Networking | Retrofit 2 + OkHttp + Moshi |
| Image loading | Coil |
| Local storage | Room |
| Build | Gradle Version Catalogs (libs.versions.toml) |

---

## Getting Started

### Prerequisites

- Android Studio Narwhal (2025.1) or later
- JDK 11+
- Android SDK 37

### Build

```bash
# Clone the repo
git clone https://github.com/ColdP/TodayWallpaper.git
cd TodayWallpaper

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires keystore env vars)
./gradlew assembleRelease
```

Release signing requires the following environment variables:

| Variable | Description |
|---|---|
| `KEYSTORE_PATH` | Absolute path to your `.jks` keystore |
| `STORE_PASSWORD` | Keystore password |
| `KEY_PASSWORD` | Key password |

---

## Project Structure

```
TodayWallpaper/
├── app/
│   └── src/
│       └── main/
│           ├── java/btm/m/todaywallpaper/   # Kotlin source
│           └── res/                          # Resources
├── gradle/
│   └── libs.versions.toml                   # Version catalog
├── images/                                  # README assets
├── build.gradle.kts
└── settings.gradle.kts
```

---

## Contributing

Pull requests are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on how to get started, submit a PR, or report an issue.

---

## License

This project is licensed under the [MIT License](LICENSE).

See [OPEN_SOURCE_LICENSES.md](OPEN_SOURCE_LICENSES.md) for the full list of third-party libraries and their licenses.

---

<p align="center">
  Made with ❤️ by <a href="https://btm-m.site">btm_m</a> ·
  <a href="https://btm-m.live">Blog</a>
</p>
