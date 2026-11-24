# Narsil for Android

<p align="center">
  <img src="https://raw.githubusercontent.com/davshn/narsil-pet/main/assets/readme/greeting-claro.svg" width="150" alt="Narsil says hello (light mode)">
  <img src="https://raw.githubusercontent.com/davshn/narsil-pet/main/assets/readme/greeting-oscuro.svg" width="150" alt="Narsil says hello (dark mode)">
</p>

> **Narsil Workshop · [narsil.davshn.tech](https://narsil.davshn.tech)**
>
> A free workshop, in English, Spanish, Italian, French, and Brazilian Portuguese, that teaches step by step how to build Claude Code mods like [Narsil](https://github.com/davshn/narsil-pet) and [Narsil Check](https://github.com/davshn/narsil-check), and how to build a Narsil desk bot with a body. No sign-up, no analytics, and it never asks for tokens or keys.

Narsil for Android is the phone app of the [Narsil](https://github.com/davshn/narsil-pet) family: it brings `claude.ai` and Narsil's alerts together in one app. Inside, Claude wears Narsil's colors and shows her band, with the state of your conversations, her level, and your usage limits. Outside, Narsil tells you with a notification when a conversation finishes, needs you, or fails, even with the app closed.

It works on top of Claude's real web app, so you can approve permissions, answer questions, upload files, and read the whole conversation as always. It replaces narsil-pwa, whose alerts could not open Claude when tapped.

It is an **open source** app, released under the [MIT](#license) license. It is not affiliated with Anthropic.

> **Language note:** the app's interface and Narsil's words are in Spanish. This README keeps those labels as they appear on screen, with their English meaning next to them.

## Contents

- [Features](#features)
- [Installation](#installation)
- [Settings](#settings)
- [Supported systems](#supported-systems)
- [Other Narsil mods](#other-narsil-mods)
- [Security](#security)
- [Development](#development)
- [License](#license)

## Features

### Claude in Narsil's colors
Claude wears the night-sea palette of [Narsil Skin](https://github.com/davshn/narsil-skin): backgrounds, text, borders, code blocks, and the purple glow while it works. It changes on its own with your phone's clock: light from 6:00 to 17:59 and dark from 18:00 to 5:59, whatever appearance you picked in Claude. The system bars follow it too.

The colors and the band are those of Narsil en Claude, the extension for Firefox and Vivaldi: its scripts and styles run unchanged inside the app, from the very start of each page, so Claude's own colors never flash first.

### The band
Above the box where you write, a strip shows the open conversation:

- **Narsil**, in her current stage and in the pose of the conversation's state.
- **The state**:

  | State | When |
  | --- | --- |
  | **TRABAJANDO** (working) | Claude is thinking or running something. |
  | **TE NECESITA** (needs you) | There is a question, a plan to approve, or a permission to decide. |
  | **TERMINADA** (done) | Claude has finished its answer. |
  | **EN REPOSO** (at rest) | Nothing is going on in it. |

- **The context** the conversation has used, in green, amber, or red.
- **The cache** (`🧊 38 min`), counting down, and `🧊 fría` (*cold*) once it runs out.
- **The last event**: the last thing Claude said, or `Usando Bash` (*using Bash*) when it is only running a tool, and how long ago.

In the side menu, under the search, a block shows:

| Data | Where it comes from |
| --- | --- |
| **Narsil**, her level, and her experience ("Narsil ⭐ Nivel 28 · 30/2800 XP") | From the mod, through ntfy.sh |
| **5 horas** and **Semana** (*5 hours* and *week*), with their percentage and reset time | From Claude's own app, every minute |
| **ntfy hoy** (*ntfy today*): the day's messages and their reset | From the mod, through ntfy.sh |

What comes through ntfy.sh is only what the page cannot know: the mod sends it to `<your topic>-estado` when a turn ends. A tap on Narsil in the block opens the app's [settings](#settings).

### Alerts on your phone
While you write to Claude by Remote Control, the mod sends to your topic when a conversation finishes, needs you, or fails, and the app shows it as a notification, with Narsil in her stage and in the alert's pose:

| Alert | How it arrives |
| --- | --- |
| A question or an error | On the **Te necesita** (*needs you*) channel: it pops up, buzzes its own way, and stays until you touch it. |
| A turn that finished | On the **Terminó** (*finished*) channel. Each one takes the place of the previous one of the same conversation, so they never pile up. |

- A tap opens Claude Code inside the app.
- The alert's links show as buttons, two at most: `claude.ai` ones open in the app, any other in your browser.
- The alerts keep arriving with the app closed and after restarting the phone. A connection to ntfy.sh stays open for them, with a quiet notification of its own, "Narsil escucha tus avisos" (*Narsil is listening for your alerts*).
- If the phone was off or without a connection, the alerts it missed arrive once it is back, as ntfy.sh keeps them for 12 hours.

You no longer need the ntfy app.

### Encrypted alerts
With `/narsil movil clave`, the mod encrypts everything it sends to your topic, alerts and states, with a key only your PC and your phone have. Anyone reading the topic only sees "Narsil" and unreadable text. Put the key in the app's [settings](#settings) and it opens them. If the key is not the one that sealed them, the alert says "🔒 Aviso cifrado: pon la clave en los ajustes de la app" (*encrypted alert: put the key in the app's settings*), and the band, "🔒 Toca y pon la clave" (*tap and put in the key*).

Each `/narsil movil clave` creates a **new** key: after running it, put the new one in the app.

### Sign in once
You sign in to Claude once and the app keeps the session, until Claude ends it or you clear the app's data. The session is saved each time the app goes to the background, so it survives Android closing the app.

### Files and voice
The **+** button in Claude opens the phone's file picker, photos included. The microphone works for dictating to Claude: the app asks for its permission the first time.

## Installation

You need an Android phone with Android 10 or later, and on your computer a JDK 17 or later and the Android SDK (`ANDROID_HOME`). Connect the phone by USB with USB debugging on, and in a copy of this repository run:

```
./gradlew installDebug
```

Then:

1. Open **Narsil** and sign in to Claude **with your email**, the one of your account even if you made it with Google. Google and Apple do not allow signing in inside an app like this one: their buttons only say so.
2. In Claude Code, run `/narsil movil` to get your topic and, if you want the alerts encrypted, `/narsil movil clave`.
3. Open the app's [settings](#settings), write the topic and the key, and tap **Guardar y recibir avisos** (*save and receive alerts*). Allow notifications.
4. Tap **Permitir en segundo plano** (*allow in the background*): without it, Android cuts the connection while the phone sleeps and the alerts arrive late.

Optional: to have `claude.ai` links from your email open in the app, go to Android's **Settings → Apps → Narsil → Open by default → Add link** and turn on `claude.ai`.

To update it, pull the latest version of this repository and run the same command again: the session and the settings are kept.

## Settings

The settings hold your topic and your key, and only on this phone. They open from:

| Where | How |
| --- | --- |
| The band in Claude's side menu | A tap on Narsil, or on the notice that asks for the topic or the key. |
| The notification "Narsil escucha tus avisos" | A tap on it. |
| The app's icon | A long press and **Ajustes** (*settings*), if your launcher shows shortcuts. |

## Supported systems

| | Supported |
| --- | --- |
| Android | 10 or later |
| Android System WebView | One that runs scripts at the start of a page (any recent one, updated from Google Play) |
| Sign-in | Email; not Google or Apple |
| iPhone | Not available |

Some phones (Infinix, Xiaomi, Huawei, and others) close apps in the background on their own. If the alerts stop arriving, check that **Permitir en segundo plano** is on and that the phone's own battery saver lets Narsil run.

## Other Narsil mods

The app is one of several pieces. Each one installs on its own:

| | What it is |
| --- | --- |
| [Narsil](https://github.com/davshn/narsil-pet) | The Claude Code mod: a band above the prompt, voice, alerts, and the ntfy.sh messages this app shows. **The app needs it** for the alerts, Narsil's level, and her stage. |
| [Narsil Check](https://github.com/davshn/narsil-check) | A side pane that checks the project you are working on. |
| [Narsil Skin](https://github.com/davshn/narsil-skin) | Color themes and animated wallpapers for the Claude desktop app. |
| Narsil en Claude | The extension for Firefox and Vivaldi on Android whose colors and band this app carries. |
| [Narsil Watch](https://github.com/davshn/narsil-watch) | Narsil on a Wear OS watch. |
| narsil-pwa | The web app this one replaces. |

## Security

- The topic and the key are kept in the app's private storage, never in its code or in the page: the band gets only what they bring.
- Without a key, anyone who knows your topic can read and publish on it. The app shows text from it only as text, and a picture only from Narsil's own drawings.
- The extension's scripts and the channel to the app run only on `https://claude.ai`, in the page itself and not in its frames. Any other address opens in your browser.
- The app pretends nothing: Claude, Google, and Cloudflare see it as the Android app it is.

## Development

Contributions are welcome: open an issue or a pull request on [GitHub](https://github.com/davshn/narsil-android).

```
./gradlew testDebugUnitTest
./gradlew installDebug
```

The tests check the encryption against an alert sealed by the extension's own code, the alert rules, the state the band keeps, and the script that loads the extension. A debug build's page can be inspected from a computer at `chrome://inspect`; its console goes to logcat with the `NarsilWeb` tag.

- `app/src/main/assets/host.js`: what the extension's scripts ask of their browser, answered by the app.
- `app/src/main/assets/extension/`: the copy of the extension's scripts and styles. `tools/sync-extension.sh ../narsil-claude` takes a newer one, and `SOURCE` says which commit it came from.
- `ExtensionScript.kt`: puts `host.js`, the styles, and the scripts together, in the extension's order.
- `MainActivity.kt`: Claude in the WebView, the files and the microphone, and where each link goes.
- `StateFeed.kt` and `StateModel.kt`: the band's state, from `<your topic>-estado`.
- `AlertService.kt`, `NtfyStream.kt`, `Alert.kt`, and `Notifier.kt`: the alerts, from the topic to the notification.
- `Seal.kt`: the `/narsil movil clave` encryption (HMAC-SHA256 in counter mode, with a tag).
- `SettingsActivity.kt` and `Settings.kt`: the topic, the key, and what the app keeps.
- `odd/tasks/android-app.md`: the requirements and how each one was checked.

## License

Narsil for Android is free software under the [MIT](LICENSE) license.

In short: you can use, copy, modify, and share it, also in your own projects, as long as you keep the copyright notice and the license. It comes with no warranty. See the [LICENSE](LICENSE) file for the full text.

Copyright © 2026 Davshn
