# nanoAI – Your Private AI Assistant

**nanoAI** is a privacy-first Android app that brings powerful AI capabilities directly to your device. Chat with AI, generate images, process audio, get coding help, and translate languages – all while keeping your data private and secure.

## 🌟 What Makes nanoAI Special

- **🔒 Privacy by Design** – Your conversations, personal data, and AI models stay on your device.
- **⚡ Works Offline** – No internet required for most features once models are downloaded.
- **🎯 Multimodal AI** – Chat, image generation, audio processing, code assistance, and translation all in one app.
- **🎨 Beautiful & Accessible** – Clean, intuitive interface that works great on any Android device, with full TalkBack support and Material 3 design.
- **🔄 Flexible & Extensible** – Add cloud AI providers (OpenAI, Gemini, custom endpoints) or use local models as you prefer. Persona system for different AI styles.
- **🛡️ Responsible AI** – No automated content filters; users are responsible for generated content. First-launch disclaimer explains this clearly.
- **📱 Local-First Architecture** – Runs small LLMs on-device for privacy and speed, with cloud fallbacks when needed or for larger models.

## 🚀 Quick Start

### Get the App Running

1. **Download nanoAI** from github release. (coming soon)
2. **Launch the app** and accept the privacy notice
3. **Download a model** from the built-in library
4. **Start chatting** – you're ready to go!

### First Time Setup

```bash
# Clone and build the app
git clone https://github.com/vjaykrsna/nanoAI.git
cd nanoAI
./gradlew installDebug

# Launch on your device or emulator
```


## 💬 What You Can Do

### Chat with AI
- **Multiple personas** – Switch between helpful assistant, coding expert, creative writer, and more. Create custom personas with different prompts and model preferences.
- **Threaded conversations** – Keep different chats organized and easily accessible. Sidebar history with search and archive options.
- **Smart suggestions** – Context-aware responses that understand your conversation history.
- **Local vs Cloud** – Toggle between on-device models (private) and cloud APIs (OpenAI, Gemini, custom endpoints).

### Generate Images
- **On-device creation** – Generate images without sending data to external servers (planned for future release)
- **Multiple styles** – Choose from various artistic styles and formats (planned)
- **Privacy-first** – Your prompts and generated images stay completely private

### Process Audio
- **Voice interaction** – Speech-to-Speech and text-to-speech capabilities (planned for future release)
- **Audio processing** – Transcribe, translate, and analyze audio content (planned)
- **Accessibility focus** – Full screen reader support for visually impaired users

### Additional Features
- **Model Library** – Browse, download, pause/resume models with progress tracking and size requirements.
- **Settings & Export** – Configure APIs, export/import personas and settings (JSON format, unencrypted with warnings).

## 🏗️ Architecture Overview

nanoAI follows clean architecture principles with Kotlin-first design:

- **UI Layer**: Jetpack Compose with Material 3, accessible components
- **Domain Layer**: Use cases for business logic (chat, downloads, personas)
- **Data Layer**: Room database for local storage, Retrofit for cloud APIs, WorkManager for background tasks
- **Runtime Layer**: MediaPipe Generative for on-device inference, extensible to TensorFlow Lite/MLC LLM

Key technologies: Kotlin 1.9.x, Jetpack Compose, Hilt DI, Room, DataStore, WorkManager, Coroutines.

## 🧪 Testing & Quality

- **Comprehensive Test Suite**: Unit tests (ViewModels, repositories), instrumentation tests (Compose UI), macrobenchmarks
- **Coverage Goals**: Targeting 75% ViewModel, 65% UI, 70% Data layer coverage
- **Quality Gates**: ktlint, Detekt, Android Lint, automated CI checks
- **Current Status**: Foundation tests in place, working to close coverage gaps

## 📈 Roadmap

### Short-term (Next Releases)
- 🎯 Close test coverage gaps to meet quality thresholds
- 🎯 Implement text generation and polished chat UI
- 🎯 Import/export improvements for personas and settings

### Medium-term
- 🖼️ Image generation support (on-device and cloud)
- 🎵 Audio input/output (voice chat, transcription, TTS)
- 🔄 Advanced persona workflows and multi-model orchestration
- 🌐 Translation and summarization modes

### Long-term Vision: The AI Powerhouse
nanoAI isn't just an app—it's evolving into your personal AI ecosystem. Imagine:

- **🔄 Multi-API Load Balancing**: Seamlessly switch between OpenAI, Gemini, Anthropic, and custom endpoints with intelligent routing. Configure multiple API keys for cost optimization—never get rate-limited again!
- **🏠 Local Network AI Hub**: Turn your device into a local AI server. Host your own load balancer and API switcher accessible by other apps on your network. Share AI capabilities with your smart home, other devices.
- **🌍 Marketplace**: Community-driven model library with user-contributed models and persona, with feedback and ratings. Earn rewards for contributing high-quality models.
- **🤖 AI Agent**: Build and deploy custom AI agents that work across your devices and services, creating a truly intelligent personal assistant network.
- **💻 Vibe Coding with Linux Backend**: Integrated Termux environment for seamless coding experiences. Write, run, and test code directly within the app using a full Linux backend, with AI assistance for code generation, debugging, and project management.

**The future is limitless**—nanoAI will be the central hub connecting you to the world's AI capabilities, all while keeping your data private and under your control. Join us on this exciting journey!

## 🤝 Contributing

We welcome contributions! The project uses a structured spec-driven development process:

1. Check `specs/` for feature specifications and current plans
2. Review `docs/` for architecture, testing, and API documentation
3. Follow the testing guide for adding comprehensive test coverage
4. Run `./gradlew check` to ensure quality gates pass

### Development Setup
```bash
git clone https://github.com/vjaykrsna/nanoAI.git
cd nanoAI
./gradlew build  # Run full build with tests
./gradlew installDebug  # Install on device
```

See `docs/development/TESTING.md` for detailed testing instructions and `docs/architecture/ARCHITECTURE.md` for technical deep-dive.

Key guides for contributors:
- `docs/development/BUILD_WORKFLOW.md` – build logic conventions and automation
- `docs/development/QUALITY_GATES.md` – required static analysis and coverage thresholds
- `docs/development/TESTING.md` – testing strategy, tooling, and coverage workflows

## 📄 License

[Add license information here]

## 🙏 Acknowledgments

Built with modern Android technologies and a focus on user privacy and responsible AI use.

### Code Assistance
- **Programming help** – Get explanations, debugging help, and code suggestions
- **Multiple languages** – Support for popular programming languages
- **Context aware** – Understands your project structure and coding patterns

### Language Translation
- **Real-time translation** – Translate between multiple languages instantly
- **Conversation mode** – Maintain context across multiple exchanges
- **Offline support** – Works without internet for downloaded language models

## 🔧 Advanced Features

### Model Management
- **Local model library** – Download and manage AI models optimized for your device
- **Cloud integration** – Optional connection to services like OpenAI or Gemini for enhanced capabilities
- **Automatic updates** – Keep your models current with the latest improvements

### Personalization
- **Theme selection** – Light, dark, or system-following themes
- **Layout preferences** – Compact or comfortable spacing based on your preference
- **Accessibility options** – High contrast, large text, and screen reader optimization

### Data Control
- **Export conversations** – Backup your chat history and persona settings
- **Privacy settings** – Control what data (if any) is shared for app improvement
- **Secure storage** – All personal data encrypted and stored locally

## 📊 Quality & Privacy Commitment

nanoAI is built with transparency and user control in mind:

- **Open source** – Code available for security review and community contribution
- **Privacy-first architecture** – Designed to minimize data collection and external dependencies
- **Quality gates** – Automated testing ensures reliability across all features
- **Accessibility compliance** – Works with screen readers and meets WCAG guidelines

## 🛠️ For Developers

### Building from Source
```bash
# Run tests and quality checks
./gradlew check

# Install on connected device
./gradlew installDebug

# View test coverage reports
./gradlew jacocoFullReport
# Reports available at app/build/reports/jacoco/full/index.html
```

### Contributing
We welcome contributions! Please:

1. **Start with tests** – Add tests for new features before implementation
2. **Follow our quality gates** – Ensure all tests pass and coverage thresholds are met
3. **Respect privacy** – Any new features must maintain our privacy-first approach
4. **Document changes** – Update user-facing documentation for any new capabilities

See our [Testing Guide](docs/development/TESTING.md), [Quality Gates](docs/development/QUALITY_GATES.md), and [Architecture Overview](docs/architecture/ARCHITECTURE.md) for development details.

## 🎯 Roadmap

- **Enhanced multimodal** – Better image generation and audio processing capabilities
- **Expanded model support** – More local AI models and cloud provider options
- **Improved accessibility** – Even better support for users with different abilities
- **Performance optimization** – Faster responses and smoother interactions

## 📄 License

MIT License – you're free to use, modify, and distribute this software.

---

**Made with ❤️ for people who value their privacy and want AI that works for them, not the other way around.**

*Have questions? Found a bug? Want to contribute? We'd love to hear from you!*
