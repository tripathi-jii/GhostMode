# 👻 GhostMode

### 🔐 A Privacy-Focused Android Vault for Your Personal Media

**GhostMode** is a privacy-focused Android application designed to provide a discreet and secure space for storing private photos and sensitive media.

The application combines a **decoy notes interface**, **authentication**, **biometric security**, and **encrypted local storage** to create an additional layer of privacy for personal content.

> ⚠️ **Educational Project:** GhostMode is developed for learning and experimentation with Android privacy, authentication, encryption, and secure local storage.

---

## ✨ Features

* 🔐 **Secure Private Vault**
  Store private photos and sensitive media in a protected environment.

* 📝 **Decoy Notes Interface**
  A normal-looking notes interface provides a discreet front layer.

* 🖼️ **Private Photo Storage**
  Keep selected personal images inside the private vault.

* 🔒 **Encrypted Storage**
  Sensitive data is intended to be protected using Android cryptography mechanisms.

* 👆 **Biometric Authentication**
  Supports device biometric authentication such as fingerprint or face authentication where available.

* 🛡️ **Privacy-Focused Architecture**
  Designed with local-first storage and minimal exposure of sensitive information.

* 📱 **Modern Android UI**
  Built using **Jetpack Compose** for a modern and responsive interface.

---

## 🖼️ App Preview

> Add your application screenshots here.

| Home / Decoy Interface | Authentication    | Private Vault      |
| ---------------------- | ----------------- | ------------------ |
| 📱 Add Screenshot      | 🔐 Add Screenshot | 🖼️ Add Screenshot |

You can add screenshots using:

```md
![GhostMode Home](screenshots/home.png)
![Authentication](screenshots/authentication.png)
![Private Vault](screenshots/vault.png)
```

---

## 🛠️ Tech Stack

| Technology                    | Usage                         |
| ----------------------------- | ----------------------------- |
| **Kotlin**                    | Application development       |
| **Jetpack Compose**           | Modern UI                     |
| **Android Studio**            | Development environment       |
| **Gradle**                    | Build & dependency management |
| **Android Cryptography APIs** | Data protection               |
| **Biometric Authentication**  | Secure authentication         |
| **Android Local Storage**     | Local data/media storage      |

---

## 🏗️ Project Architecture

GhostMode follows a modular structure where different responsibilities are separated into dedicated packages.

```text
GhostMode/
│
├── app/
│   │
│   └── src/
│       └── main/
│           │
│           ├── java/
│           │   └── com/
│           │       └── vaitri/
│           │           └── ghostmode/
│           │               │
│           │               ├── login/
│           │               │   ├── LoginScreen.kt
│           │               │   └── Authentication.kt
│           │               │
│           │               ├── screens/
│           │               │   ├── HomeScreen.kt
│           │               │   ├── VaultScreen.kt
│           │               │   └── NotesScreen.kt
│           │               │
│           │               ├── security/
│           │               │   ├── Encryption.kt
│           │               │   └── BiometricHelper.kt
│           │               │
│           │               └── ui/
│           │                   ├── theme/
│           │                   ├── components/
│           │                   └── navigation/
│           │
│           └── res/
│               ├── drawable/
│               ├── mipmap/
│               └── values/
│
├── gradle/
│
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
│
├── screenshots/
│   ├── home.png
│   ├── authentication.png
│   └── vault.png
│
├── .gitignore
├── LICENSE
└── README.md
```

---

## 🔄 Application Flow

```text
                    ┌──────────────────┐
                    │    GhostMode     │
                    │      Launch      │
                    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │  Decoy Interface │
                    │   Notes Screen   │
                    └────────┬─────────┘
                             │
                      Authentication
                             │
                             ▼
                    ┌──────────────────┐
                    │   Biometric /    │
                    │   Authentication │
                    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │   Private Vault  │
                    └────────┬─────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │ Private Photos & │
                    │   Sensitive Data │
                    └──────────────────┘
```

---

## 🔐 Security & Privacy

GhostMode is designed with privacy and secure local storage in mind.

The application explores Android security mechanisms such as:

* 🔒 Local encrypted storage
* 👆 Biometric authentication
* 🔑 Secure authentication mechanisms
* 📱 Device-level security
* 🛡️ Minimal exposure of sensitive data
* 🚫 No unnecessary external data sharing

### ⚠️ Important

GhostMode should **not be considered a professionally audited security product**.

The actual security level depends on the implementation, Android version, device security, encryption/key-management strategy, and application configuration.

### 🚨 Never Commit Secrets

Never commit the following to the repository:

```text
❌ API Keys
❌ Passwords
❌ Private Keys
❌ Signing Keys
❌ Keystores
❌ Authentication Tokens
❌ Other Sensitive Credentials
```

Use `.gitignore` and secure local configuration for sensitive development data.

---

## 📱 Requirements

Before running GhostMode, make sure you have:

* Android **12 or higher**
* Android Studio
* Kotlin
* Gradle
* Android SDK
* Android Emulator or physical Android device

---

## 🚀 Getting Started

### 1️⃣ Clone the Repository

```bash
git clone https://github.com/tripathi-jii/GhostMode.git
```

### 2️⃣ Open in Android Studio

Open the cloned **GhostMode** folder in Android Studio.

### 3️⃣ Sync Gradle

Allow Android Studio to download the required dependencies and complete Gradle synchronization.

### 4️⃣ Connect a Device

You can either:

* Connect a physical Android device with USB debugging enabled
* Start an Android Emulator

### 5️⃣ Run the Application

Click:

```text
Run ▶
```

in Android Studio.

---

## 🧪 Development

For development, the recommended workflow is:

```text
Clone Repository
       ↓
Open in Android Studio
       ↓
Sync Gradle
       ↓
Build Project
       ↓
Run Emulator / Device
       ↓
Test Features
       ↓
Commit Changes
```

---

## 📂 Important Directories

### `login/`

Contains authentication-related components.

```text
login/
├── LoginScreen.kt
└── Authentication.kt
```

### `screens/`

Contains the main application screens.

```text
screens/
├── HomeScreen.kt
├── VaultScreen.kt
└── NotesScreen.kt
```

### `security/`

Contains security-related functionality.

```text
security/
├── Encryption.kt
└── BiometricHelper.kt
```

### `ui/`

Contains reusable UI components, themes, and navigation.

```text
ui/
├── theme/
├── components/
└── navigation/
```

---

## 🎯 Project Goals

The main goal of GhostMode is to explore practical Android development concepts including:

* Android application architecture
* Jetpack Compose
* Secure local storage
* Encryption
* Biometric authentication
* Privacy-oriented UI design
* Android security APIs
* Modular project organization

---

## 🔮 Future Improvements

Possible future enhancements include:

* 🔐 Stronger key-management architecture
* 🖼️ Support for additional media types
* 📁 Private folders/albums
* 🔍 Secure media search
* 🌙 Improved dark/light themes
* 🔄 Secure backup and restore
* 🔒 Automatic vault locking
* 🧹 Secure deletion mechanisms
* 📊 Security/activity logs
* 🎨 Improved animations and UI/UX
* 🧪 Automated security and UI testing

---

## 🤝 Contributing

Contributions, suggestions, and improvements are welcome.

### Contribution Steps

```bash
# Fork the repository

# Clone your fork
git clone <your-fork-url>

# Create a new branch
git checkout -b feature/new-feature

# Make your changes

# Commit your changes
git commit -m "Add new feature"

# Push the branch
git push origin feature/new-feature
```

Then create a **Pull Request**.

---

## 👨‍💻 Developer

### Vaibhav Tripathi

🔗 GitHub:
https://github.com/tripathi-jii

---

## 📄 License

This project is currently intended for **educational and development purposes**.

If you plan to distribute GhostMode publicly, add an appropriate open-source license such as **MIT**, **Apache-2.0**, or another license that matches your intended usage.

---

## ⚠️ Disclaimer

GhostMode is an educational/project application created to explore Android privacy and security concepts.

Users are responsible for using the application in accordance with applicable laws, regulations, and platform policies.

The application should not be considered a substitute for professionally audited secure-storage software.

---

<div align="center">

### 👻 GhostMode

**Privacy • Security • Simplicity**

Made with ❤️ using Kotlin & Jetpack Compose

</div>
