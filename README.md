# Narsil for Android

An Android app for [Narsil](https://github.com/davshn/narsil-pet), the Claude Code pet. It replaces [narsil-pwa](https://github.com/davshn/narsil-pwa): it shows `claude.ai` in a WebView dressed by [narsil-claude](https://github.com/davshn/narsil-claude), and it shows Narsil's ntfy.sh alerts as native notifications.

Not affiliated with Anthropic. It is a personal-use app, installed from this repository.

## What it does

- **claude.ai with Narsil's colors and band.** The extension's content scripts and styles run unchanged inside the WebView, at document start, as Vivaldi runs them: the night-sea palette by night and the light one by day, the band in the sidebar and the strip above the box where you write. The app answers what the scripts ask of their browser (`app/src/main/assets/host.js`), and follows `<topic>-estado` on ntfy.sh in place of the extension's background, so the topic and the key never reach the page.
- **Alerts without a browser.** A foreground service follows `<topic>` on ntfy.sh, as the PWA's web push did:
  - a question or an error (priority 4 and up) goes to the **Te necesita** channel and buzzes its own way;
  - each "done" replaces the previous one of its conversation (its `conv-` tag);
  - ntfy "view" actions show as buttons, two at most;
  - a tap opens Claude Code inside the app; claude.ai links open in the app, any other in your browser;
  - sealed messages (`/narsil movil clave`) are opened with the key you set.
  It goes on from the last alert it showed, so one sent while the phone was off arrives once it is back (ntfy.sh keeps them 12 h), and it starts again with the phone.
- **One sign-in.** The session cookie is written to disk whenever the app goes to the background, so it stays until claude.ai ends it or you clear the app's data.

## Install

You need Android 10 or later, a JDK 17+ and the Android SDK (`ANDROID_HOME`), and the phone connected with USB debugging on.

```
./gradlew installDebug
```

Then:

1. Open **Narsil** and sign in to claude.ai **with your email**, the one of your account even if you made it with Google: Google's and Apple's sign-ins refuse to run inside an app's WebView, so their buttons only say so.
2. Long-press the app's icon and open **Ajustes** (or tap the band's notice in Claude's sidebar). Type the topic `/narsil movil` gives you and, if you made one, the key of `/narsil movil clave`. Tap **Guardar y recibir avisos** and allow notifications.
3. Tap **Permitir en segundo plano**: without it, Android cuts the connection while the phone sleeps and alerts come late.

Optional: to have claude.ai links from your email open in the app, go to Android's **Settings → Apps → Narsil → Open by default → Add link** and turn on `claude.ai`.

## Updating the extension

The app carries a copy of narsil-claude's content scripts and styles in `app/src/main/assets/extension` (its commit is in `SOURCE`). To take a newer one:

```
tools/sync-extension.sh ../narsil-claude
```

## Development

- `./gradlew testDebugUnitTest` runs the JVM tests: the sealing port against a vector sealed by the extension's own code, the alert rules, the state kept for the band and the script that hosts the extension.
- A debug build's page can be inspected from a computer at `chrome://inspect`. The page's console goes to logcat under the `NarsilWeb` tag.

## Security

- The topic and the key are kept in the app's private storage, never in its code.
- Without a key, anyone who knows your topic can read and publish on it: the app shows a picture only from Narsil's own assets, and text only as text.
- The extension's scripts and the channel to the app run on `https://claude.ai` only, in its top frame.

## License

[MIT](LICENSE)
