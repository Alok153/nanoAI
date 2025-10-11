# nanoAI – Offline Multimodal AI Assistant

nanoAI is a privacy-first Android assistant that keeps inference on-device while offering optional cloud fallback when you need extra horsepower. Switch personas, manage local models, and take your conversations anywhere without giving up control over your data.

## ✨ Highlights

- 🔒 **Local-first privacy** – Conversations, models, and telemetry stay on your device by default.
- 🤖 **Personalised personas** – Swap between curated assistants or create your own with custom prompts and model preferences.
- 💬 **Threaded conversations** – Organise chats, archive history, and export backups in a couple of taps.
- ⚙️ **Power-user tooling** – Manage API providers, monitor downloads, and blend local/cloud inference on demand.

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 11+
- Android device or emulator on Android 12 (API 31) or higher

### Install & Run
```bash
git clone https://github.com/vjaykrsna/nanoAI.git
cd nanoAI

# Build and install the debug build
./gradlew installDebug
```

Launch the app, accept the safety disclaimer, download a model from the library, and you’re ready to chat.

## 🔍 What You Can Do

- Explore the **Model Library** to download on-device models using MediaPipe Generative (LiteRT).
- Configure **cloud providers** like OpenAI or Gemini for hybrid inference flows.
- Manage personas with temperature/top-p controls and swap them mid-conversation.
- Export your data for backup or migration directly from Settings.

## 📚 Documentation

- [Testing & Coverage](docs/testing.md) – How we keep quality high.
- [Architecture](docs/ARCHITECTURE.md) – System design, data flow, and modules.
- [API Reference](docs/API.md) – Public surfaces, request shapes, and integration notes.

## 🤝 Contributing

Pull requests and issue reports are welcome. Please open a feature branch, add tests for new behaviour, and run the quality gates (`./gradlew test connectedAndroidTest spotlessCheck detekt`) before submitting a PR.

## 📄 License

This project is licensed under the MIT License – see `LICENSE` for details.

---

**Built with ❤️ for privacy-conscious AI enthusiasts**
