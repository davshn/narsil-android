# Narsil for Android

Objective: a native Android app that replaces narsil-pwa: claude.ai in a WebView dressed by the narsil-claude extension, plus Narsil's ntfy alerts as native notifications.
Branch: main (new repository). Delivery: ask-on-risk. Test runner: `./gradlew testDebugUnitTest` (JVM unit tests).

## Specs

- S1 (L1): "una app movil completa, que reciba las notificaciones y que dentro en un web view muestre la web de claude y modifique sus estilos como lo hace esta extension"
- S2 (L3): "los estilos que va a inyectar seran los de la extension narsil-claude" — the extension's content scripts and CSS run unchanged inside the WebView, with a `browser` shim the app answers natively.
- S3 (L2): it replaces the PWA; personal use, sideloaded APK; login once and the session stays (cookies flushed to disk).
- S4 (L2): the repository is public and named `narsil-android`: no topic or key in the code; they are typed in the app's settings.
- S5: alerts keep the PWA's rules: sealed messages opened with the key (`narsil1:`), priority >= 4 stays until touched and buzzes its own way, a "done" replaces the previous one of its `conv-` tag, ntfy "view" actions as buttons, a tap opens Claude Code inside the app.
- S6 (L3): "no olvides ir haciendo commits al repositorio a medida que vas trabajando".

## Tasks

- T1 S4,S5 inline: Gradle project and `Seal` port, tested against vectors from narsil-claude's seal.js.
- T2 S1,S2,S3 inline: WebView host for the extension (shim, CSS, state feed from `<topic>-estado`).
- T3 S1,S5 inline: foreground service following `<topic>`, notifications, boot start, settings screen.
- T4 S3 inline: build, install on the phone, check login and alerts; README.

## Log

- L1 (2026-11-23): "se me ocurrio crear una app movil completa, que reciba las notificaciones y que dentro en un web view muestre la web de claude y modifique sus estilos como lo hace esta extension, eso es viable?"
- L2 (2026-11-24): "seria para reemplazar la pwa, la friccion que me dices para iniciar sesion seria igual solo una vez, no? ... (igual seria para mi uso personal)" / "hazlo en un proyecto nuevo y probemos a ver que tal, el repositorio seria publico, te dejo conectado el celular para que la instales"
- L3 (2026-11-24): "no olvides ir haciendo commits al repositorio a medida que vas trabajando el repo se va a llamar narsil-android" / "recuerda que los estilos que va a inyectar seran los de la extension narsil-claude"
- Decision: the alert stream is followed by a `specialUse` foreground service, since Android 15 caps `dataSync` at 6 h a day; no Firebase, no ntfy app needed.
