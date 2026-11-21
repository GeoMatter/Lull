# Lull: Slack for Quest (unofficial)

A lightweight Slack client for Meta Quest. It opens the Slack web app full-window as its own app in your library, so you don't have to juggle browser tabs.

> Not affiliated with, endorsed by, or sponsored by Slack Technologies. "Slack" is a trademark of its owner. Lull simply loads `app.slack.com`.

## Features
- Opens straight into your Slack workspace
- Stays signed in between launches
- File uploads through the system picker
- Other links and downloads open in the Quest browser
- Resizable panel (defaults to 1280×800)

## Build

**GitHub Actions (no Android Studio needed):** push this repo to GitHub, open the **Actions** tab, open the latest **Build APK** run and download **Lull-apk**.

**Locally:** open the folder in Android Studio and choose **Build → Build APK(s)**, or run:
```
gradlew.bat assembleRelease
```
The APK lands in `app/build/outputs/apk/release/`.

## Install on Quest
1. Connect the Quest to your PC with USB debugging on.
2. Drag the APK into SideQuest, or run:
   ```
   adb install -r app-release.apk
   ```
3. In the headset, go to **Library → Unknown Sources → Lull**.

## Known limits
- **Sign in with email.** Google sign-in blocks embedded browsers. Use email plus Slack's emailed code, or your password.
- **No push notifications** while the app is closed, because Quest has no Google services.
- **No huddles or calls**, since mic and camera aren't wired up.
- **Company SSO logins** open in the Quest browser and may not hand back to the app.
