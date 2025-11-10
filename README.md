### RedHead operates independently and relies entirely on community donations to stay online and continue developing free and open-source projects. We support Bitcoin, Monero, and Stripe -- but we prefer bitcoin/monero donations, as Stripe is unreliable as of the current moment.
### You can find all of our donation methods on our website:
[redheadindustries.xyz](https://redheadindustries.xyz)

# GNUXON – The Free and Open-Source Bodycam App  
**Version:** v1.0b “Whizzbang – Milestone 1”  
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
git clone https://github.com/RedHeadIndustries/gnuxon.git
cd gnuxon
```

3. Open the project in **Android Studio (Arctic Fox or newer)**.

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
