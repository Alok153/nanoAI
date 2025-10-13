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

- [Testing & Coverage](docs/testing.md) – How we keep quality high with automated coverage gates.
- [Architecture](docs/ARCHITECTURE.md) – System design, data flow, and modules.
- [API Reference](docs/API.md) – Public surfaces, request shapes, and integration notes.
- [Coverage Risk Register](docs/coverage/risk-register.md) – Open coverage gaps and mitigation tracking.
- [Development Roadmap](docs/todo-next.md) – Next phase priorities and coverage goals.

### 📊 Quality & Coverage

nanoAI enforces automated quality gates with test coverage thresholds:
- **ViewModel**: 75% target (current: 39.58%)
- **UI**: 65% target (current: 1.90%)
- **Data**: 70% target (current: 18.91%)

Run the full test suite and coverage report:
```bash
# Run all tests (unit + instrumentation)
./gradlew testDebugUnitTest ciManagedDeviceDebugAndroidTest

# Generate merged coverage report
./gradlew jacocoFullReport

# Verify coverage thresholds
./gradlew verifyCoverageThresholds
```

Coverage reports are available at `app/build/reports/jacoco/full/index.html` after running the merge task. See [Testing & Coverage Guide](docs/testing.md) for detailed instructions on running tests, managing test environments, and interpreting coverage reports.

## 🤝 Contributing

Pull requests and issue reports are welcome. Please:

1. Open a feature branch from the latest `main`
2. **Add tests first** (TDD approach) for new behaviour before implementation
3. Run the complete quality gate suite before submitting:
   ```bash
   ./gradlew check  # Runs tests, coverage, spotless, detekt
   ```
4. Ensure coverage thresholds are maintained or improved
5. Update documentation if you modify public APIs or workflows

See [Testing & Coverage Guide](docs/testing.md) for detailed testing requirements and [Architecture](docs/ARCHITECTURE.md) for design principles.

## 📄 License

This project is licensed under the MIT License – see `LICENSE` for details.

---

**Built with ❤️ for privacy-conscious AI enthusiasts**
