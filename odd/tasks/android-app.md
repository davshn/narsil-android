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

- T1 S4,S5 inline: Gradle project and `Seal` port, tested against vectors from narsil-claude's seal.js. Done: 9051a31.
- T2 S1,S2,S3 inline: WebView host for the extension (shim, CSS, state feed from `<topic>-estado`). Done: 3bcea35, 53c55c1, bfa379a.
- T3 S1,S5 inline: foreground service following `<topic>`, notifications, boot start, settings screen. Done: 53c55c1.
- T4 S3 inline: build, install on the phone, check login and alerts; README. Pending: sign-in by the person, real topic.

## Log

- L1 (2026-11-23): "se me ocurrio crear una app movil completa, que reciba las notificaciones y que dentro en un web view muestre la web de claude y modifique sus estilos como lo hace esta extension, eso es viable?"
- L2 (2026-11-24): "seria para reemplazar la pwa, la friccion que me dices para iniciar sesion seria igual solo una vez, no? ... (igual seria para mi uso personal)" / "hazlo en un proyecto nuevo y probemos a ver que tal, el repositorio seria publico, te dejo conectado el celular para que la instales"
- L3 (2026-11-24): "no olvides ir haciendo commits al repositorio a medida que vas trabajando el repo se va a llamar narsil-android" / "recuerda que los estilos que va a inyectar seran los de la extension narsil-claude"
- Decision: the alert stream is followed by a `specialUse` foreground service, since Android 15 caps `dataSync` at 6 h a day; no Firebase, no ntfy app needed.
- Evidence (2026-11-24, Infinix X6730B, Android 15, WebView 157): through DevTools the page has narsilHostLoaded, a live narsilWatch, both style marks and the night ground rgb(15, 26, 44). With a throwaway topic, two done alerts of conv-a left one notification (tag done-conv-a, importance 3) and a priority 4 one its own (importance 4); accents arrive intact; a tap opened MainActivity on claude.ai/code. 24 JVM tests green.
- Evidence (2026-11-24, signed in by the person): file upload works. With a state published to the throwaway topic's -estado, the sidebar band showed level, XP, ntfy's count and Narsil's art from raw.githubusercontent.com (the page's CSP lets it load), and the strip above the composer showed TRABAJANDO, context, cache and her pose. Nothing showed before only because the app still had the throwaway topic.
