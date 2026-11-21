# Lull: Slack for Quest (unofficial)

A lightweight app that runs Slack on Meta Quest. It opens the Slack web app full-window as its own app in your library, so you don't have to juggle browser tabs.

> **Unofficial.** Not affiliated with, endorsed by, or sponsored by Slack Technologies. "Slack" is a trademark of its owner. Lull simply loads `app.slack.com`.

## Privacy
Lull doesn't collect, store, or send any data of its own. You sign in directly with Slack, and everything goes straight between your headset and Slack's servers. The only other request Lull makes is a check of this repo's latest release number, to tell you when an update is out.

## Features
- Opens straight into your Slack workspace
- Stays signed in between launches
- File uploads through the system picker
- Tells you when a new version is available
- Other links and downloads open in the Quest browser
- Resizable panel (defaults to 1280×800)

## Install
1. Download `Lull.apk` from the [latest release](https://github.com/GeoMatter/Lull/releases/latest), or get it on SideQuest.
2. Connect your Quest with USB debugging on and drag the APK into SideQuest.
3. In the headset, go to **Library → Unknown Sources → Lull**.

## Signing in
Use **your email and the code Slack sends you**. "Continue with Google" doesn't work inside Lull, even for Gmail accounts, because Google blocks sign-ins in embedded browsers.

## Known limits
- No push notifications while the app is closed, because Quest has no Google services.
- No huddles or calls, since mic and camera aren't wired up.
- Company SSO logins open in the Quest browser and may not hand back to the app.

## Releasing (maintainer notes)
Every push to `main` builds a test APK under **Actions**. To publish a release:
```
git tag v1.1
git push origin v1.1
```
GitHub builds a signed `Lull.apk`, attaches it to a new release, and installed copies of Lull will offer the update. Releases need the four signing secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) set in the repo's **Settings → Secrets and variables → Actions**.

## License
MIT
