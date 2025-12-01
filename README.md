### RedHead operates independently and relies entirely on community donations to stay online and continue developing free and open-source projects. We support Bitcoin, Monero, and Stripe -- but we prefer bitcoin/monero donations, as Stripe is unreliable as of the current moment.
### You can find all of our donation methods on our website:
[redheadindustries.xyz](https://redheadindustries.xyz)

# GNUXON – The Free and Open-Source Bodycam App  
**Version:** v2.0b “Bobcat – Milestone 2”  
**License:** GNU General Public License v3 (GPLv3)  
**Developer:** RedHead (RIIDF Branch)  

---

## 🧠 What is GNUXON?

**GNUXON** (short for “GNU AXON”) is a free and open-source bodycam application for Android devices, created to give everyday people the ability to record safely, securely, and transparently.

It is developed by **RedHead Industries** under the **RIIDF (RedHead International Internet Defense Force) Branch**, and serves as a tool for privacy-conscious users, journalists, and security personnel who value freedom and personal transparency.

---

## 🧩 Features

- 🎥 **Full Camera Recording:** Records video directly using Android’s CameraX API.  
- 🔈 **Hardware Controls:**  
  - **Volume Up:** Start Recording  
  - **Volume Down:** Stop Recording  
- 💡 **Automatic Wake Lock:** Keeps the phone awake while recording to prevent interruptions.  
- 🌓 **UI Auto-Hiding:** The interface hides during recording for discretion and reappears when stopped.  
- 🔔 **Foreground Notification:** Displays persistent recording status for reliability.  
- 💾 **External Storage Saving:** Saves recordings to `/Movies/GNUXON/` for easy access.  
- 🔒 **No Internet Permissions:** GNUXON never connects to the internet — your data stays offline.  
- ⚙️ **FOSS & Transparent:** 100% source available, under the GNU GPLv3 License.

---

## ⚖️ License

GNUXON is licensed under the **GNU General Public License v3 (GPLv3)**.  

You are free to:
- Use, share, and modify the app
- Redistribute under the same license  
- Study and adapt the code for your own needs  

See the [LICENSE](LICENSE) file for full legal information.

---

## 🛠️ Build Instructions

1. Clone the repository:

```
bash
git clone https://github.com/redhead-industries/gnuxon.git
cd gnuxon
```

3. Open the project in **Android Studio (Otter or newer)**.

4. Let Gradle sync automatically.
   Minimum SDK: **34**
   Target SDK: **34**

5. Build the project:

   ```bash
   ./gradlew assembleDebug
   ```

6. Install on your device:

   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 🔧 Reproducible Builds

GNUXON supports reproducible builds for verification and F-Droid compatibility.

### Prerequisites for Reproducible Builds

- **Android Studio:** Otter (2025.2.1) or newer
- **Gradle:** 8.13.1 (as specified in gradle/wrapper/gradle-wrapper.properties)
- **JDK:** 17 (recommended)
- **Build Tools:** Use the versions specified in build.gradle.kts

### Building Reproducibly in Android Studio

1. Ensure you're using the correct JDK version:
   - Go to **File > Settings > Build, Execution, Deployment > Build Tools > Gradle**
   - Set **Gradle JDK** to JDK 17

2. Use the exact Gradle version:
   ```bash
   ./gradlew --version
   # Should show Gradle 8.13.1
   ```

3. Build the release APK:
   ```bash
   ./gradlew assembleRelease
   ```

4. Verify the build:
   - The APK will be in `app/build/outputs/apk/release/`
   - Use `apksigner` to verify the build is consistent

### Notes for Reproducibility

- Builds are deterministic when using the same Gradle, JDK, and build tools versions
- Timestamps are disabled in the build configuration
- F-Droid builds use the same configuration for verification
- For exact F-Droid reproducibility, use the build environment specified in `.fdroid.yml`

---

## 📱 Usage

* Launch GNUXON.
* On first run, grant required permissions (camera, microphone, storage).
* Press **Volume Up** to start recording — the UI will hide.
* Press **Volume Down** to stop recording.
* Videos are saved at:

  ```
  /storage/emulated/0/Movies/GNUXON/
  ```

---

## ❤️ A Message from RedHead

We believe privacy and freedom are human rights.
GNUXON was made for those who protect truth, freedom, and accountability — from the streets to the front lines.

> “We are your neighbors, your coworkers, your friends.
> We are also the silenced and forgotten.
> We are RedHead.”

---

## 🌐 Links

* 🔗 [RedHead Official Website](https://redheadindustries.xyz)
* 🐙 [GitHub Profile](https://github.com/redhead-industries)
* 🧾 [GPLv3 License Information](https://www.gnu.org/licenses/gpl-3.0.html)

---

**© 2025 RedHead Industries – RIIDF Branch**
*Free. Open. For everyone.*
