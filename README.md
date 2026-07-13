<div align="center">

![Liqora Launcher Banner](https://via.placeholder.com/1200x400/1E3A8A/FFFFFF?text=Liqora+Launcher+-+Liquid+Glass+Experience)

# Liqora Launcher

**A premium, glass-inspired Android home launcher built with Jetpack Compose**

[![GitHub Stars](https://img.shields.io/github/stars/jester-sys/liqora-launcher?style=for-the-badge&logo=github&color=0ea5e9)](https://github.com/jester-sys/liqora-launcher)
[![License](https://img.shields.io/github/license/jester-sys/liqora-launcher?style=for-the-badge&logo=opensourceinitiative&color=22c55e)](https://github.com/jester-sys/liqora-launcher/blob/main/LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-7F52FF?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-8%2B-34A853?style=for-the-badge&logo=android)](https://developer.android.com)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.6%2B-4285F4?style=for-the-badge&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)

**Minimum SDK**: Android 8.0 (Oreo) • **Language**: Kotlin • **UI**: Jetpack Compose • **Architecture**: MVVM

[Installation](#installation) • [Features](#-features) • [Screenshots](#-screenshots) • [Roadmap](#️-roadmap) • [Contributing](#-contributing)

</div>

## ✨ Overview

**Liqora Launcher** is a modern, highly customizable Android home launcher that brings the elegance of liquid glass aesthetics to the Android ecosystem. It seamlessly blends premium visual effects — dynamic blur, transparency, and fluid glass-like surfaces — with Android's renowned flexibility and customization capabilities.

Unlike many launchers that simply mimic other operating systems, Liqora was designed from the ground up to **enhance** Android's strengths while introducing a refined, contemporary interface that feels both premium and deeply personal.

Built with cutting-edge Jetpack Compose, Material Design 3, and a clean MVVM architecture, Liqora delivers buttery-smooth performance, beautiful animations, and an extensive set of personalization options — all while maintaining excellent battery life and system responsiveness.

---

## 🚀 Features

Liqora Launcher offers a comprehensive suite of features designed for both power users and those seeking a refined daily driver experience.

### Home Screen
The home screen serves as the heart of the Liqora experience. It supports unlimited home screen pages with smooth horizontal paging, drag-and-drop icon placement with real-time preview, resizable widgets with snap-to-grid alignment, custom grid sizes (3x3 up to 8x8), icon size, label visibility, and padding customization, plus long-press gestures for quick actions. All interactions are powered by smooth 60/90/120 FPS animations depending on device capability.

### App Drawer
A modern, searchable app drawer with multiple layout options including alphabetical list with fast scrolling and section headers, grid view with customizable columns, automatic categories, hide apps functionality, real-time search filtering, and gesture support (swipe up from home screen).

### Widgets
Premium widget support including built-in weather widget with live updates, media playback controls with album art and progress, calendar events preview, customizable clock widgets, system information widgets, and full third-party widget compatibility.

### Folder System
Intelligent folder management with create-by-drag, rename, resize, recolor, nested support, folder background blur and glass effects, plus quick access to contained apps.

### Search
Global search with instant app, contact, and settings results, web search integration, search history, suggestions, and voice support.

### Weather
Integrated weather with current conditions, hourly/daily forecasts, location-based updates, animated icons, and customizable refresh intervals.

### Media Controls
Persistent media controls featuring album art with glassmorphic overlay, playback controls, progress bar, and integration with popular media apps.

### Wallpapers
Deep wallpaper integration supporting static images, live wallpapers, dynamic wallpapers with subtle animations, daily rotation, blur/dimming effects, and parallax scrolling.

### Blur Engine
Sophisticated blur system using the Android Liquid Glass library with real-time backdrop sampling, adjustable radius, and performance-aware adaptive blur.

### Transparency & Glass Effects
True glassmorphic design with variable opacity, frosted glass appearance, noise texture, light/dark mode adaptation, border highlights, and subtle inner shadows.

### Themes & Dynamic Colors
Material Design 3 theming with full Dynamic Colors support (Android 12+), custom accent picker, Light/Dark/Black themes, wallpaper-based auto switching, and icon pack readiness.

### Animations
Fluid physics-based animations including icon launch scaling, page transitions, widget resize springs, drawer velocity tracking, all respecting Android's Reduce Motion setting.

### Personalization
Extensive options covering home screen padding, icon packs, font scaling, gesture customization, and navigation/status bar controls.

### Settings
Comprehensive searchable settings organized by Appearance, Behavior, Gestures, Notifications, and Advanced.

### Responsive Design & Accessibility
Optimized for phones, tablets, various densities; full TalkBack, high contrast, font scaling, reduced motion, and color correction support.

---

## 🛠 Technology Stack

- **Kotlin 1.9+** – Primary language with full coroutine support
- **Jetpack Compose 1.6+** – Declarative UI toolkit
- **Material Design 3** – Design system and dynamic theming
- **AndroidX** – Core libraries
- **Room** – Local database for settings and widgets
- **Coil** – Image loading for icons and wallpapers
- **Kotlin Coroutines + StateFlow** – Asynchronous and reactive state management
- **MVVM Architecture** – Clean separation of concerns
- **OkHttp** – Networking (weather, updates)
- **WorkManager** – Background tasks
- **Gradle Kotlin DSL + Kotlin Serialization** – Build and data handling

---

## Installation

Set as default launcher: Go to device **Settings → Apps → Default apps → Home app → Select Liqora Launcher**.

## 🔨 Build Instructions

**Debug APK**
```bash
./gradlew assembleDebug
```

**Release APK**
```bash
./gradlew assembleRelease
```

**Release AAB**
```bash
./gradlew bundleRelease
```

Configure signing via `gradle.properties` or Android Studio for release builds.

---

## 📸 Screenshots

### Home Screen
<img width="300" height="650" src="https://github.com/user-attachments/assets/b6e5182c-f08b-4d1c-b434-acb5c32024ca" alt="Home Screen">

*Beautiful liquid glass home screen with weather widget, media player, and app icons*

### Settings Panel
<img width="300" height="650" src="https://github.com/user-attachments/assets/b78f8c16-b64c-457b-b817-ae8475e82c2d" alt="Settings">

*Comprehensive settings with global themes and visual effects*

<img width="300" height="650" src="https://github.com/user-attachments/assets/fb22984c-ee53-4196-ab23-6f845626774b" alt="Settings Visual Effects">

*Liquid Glass Effects, Blur, and advanced visual controls*

### Customize Panel
<img width="300" height="450" src="https://github.com/user-attachments/assets/b6cf83ee-35d1-4e8a-bf29-53533e503e73" alt="Customize Panel">

*Real-time customization of panel size, background tint, opacity, blur intensity, and corner radius*

---

## 🗺️ Roadmap

### v1.x (Near Term)
- Enhanced gesture navigation
- Full icon pack support
- Additional built-in widgets
- Backup & restore system
- Improved landscape mode

### v2.x (Mid Term)
- Tablet & foldable optimization
- Plugin/extension system
- Advanced theming engine
- Privacy-first cloud sync
- AI-powered suggestions

### Future
- Deeper accessibility tools
- Performance profiling suite
- Community feature voting system

---

## 💡 Why Liqora

Modern mobile interfaces have become flat and uniform. Liqora exists to restore depth, beauty, and personality to the Android home screen without sacrificing performance or customization.

It solves boring static layouts, poor visual hierarchy on high-res displays, lack of cohesive glassmorphic design, and fragmented customization.

**Design Philosophy:**
- Beauty through restraint
- Performance always first
- True to Android's open nature
- Maximum personalization with simplicity
- Open, transparent development

Liqora is proudly Android — elevated.

---

## ⚡ Performance

- **Rendering**: Tightly controlled Compose recomposition + hardware-accelerated blur layers
- **Memory**: Typically under 85MB resident set on mid-range devices
- **Animations**: Consistent 60+ FPS with adaptive frame rate support
- **Battery**: Intelligent WorkManager scheduling with Doze and battery constraints respected
- **Responsiveness**: Sub-16ms input latency for core interactions

The launcher is optimized for both flagship and budget devices.

---

## 📦 Dependencies & Credits

**Android Liquid Glass (Backdrop) Library** by Kyant0
Repository: https://github.com/Kyant0/Android-Liquid-Glass
This library powers the core liquid glass rendering and backdrop effects.

**Fossify Home Launcher (Base)**
Repository: https://github.com/FossifyOrg/Home
Liqora is a fork of Fossify Home Launcher. We thank the Fossify Team for the solid open-source foundation. The project has been extensively redesigned with new Compose UI, Liquid Glass effects, customization depth, widgets, and modern animations while preserving original copyrights.

---

## 🔀 Fork Notice

This project is a fork of **Fossify Home Launcher**.

- Original copyrights and notices remain intact
- Full compliance with GPL-3.0 license
- New contributions include Liquid Glass UI, extensive customization, modern widgets, animations, and settings system
- All license requirements are respected

---

## 👥 Contributing

Contributions are welcome!

**How to Contribute**
1. Fork the repo
2. Create a feature branch
3. Commit changes with clear messages
4. Push and open a Pull Request

Follow ktlint style, update documentation, and ensure the app builds cleanly on Android 8+.

You can contribute via bug reports, feature ideas, code, design feedback, or translations.

---

## 📜 License

**GNU General Public License v3.0**

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

Full license text is available in the [LICENSE](LICENSE) file.

---

## ❤️ Support

Support the project by:
- Starring on GitHub
- Sharing with the community
- Opening issues and discussions
- Submitting Pull Requests
- Spreading the word

Every star and contribution helps!

---

## 👤 Author

**Kanhaiya Yadav**
GitHub: [jester-sys](https://github.com/jester-sys)

Made with ❤️ for the Android community

---

<div align="center">

⭐ **Star this repo if you enjoy Liqora!**

</div>
