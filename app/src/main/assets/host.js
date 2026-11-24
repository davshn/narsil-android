// What narsil-claude's content scripts ask of their browser, answered by the app. `narsilHost` is the WebView's
// message channel to the app (WebViewCompat.addWebMessageListener, only on https://claude.ai): the app follows
// ntfy.sh in place of the extension's background and posts what it brought here, and the topic and the key never
// reach the page. The extension's styles go on as constructed style sheets, which the page's CSP does not govern.
// The app wraps this file, the styles and the scripts in one function, run at document start in the top frame only.

const host = self.narsilHost
const ASSETS = 'https://claude.ai/__narsil__/extension/'
const STYLE_FILES = ['skin.css', 'band.css']
const sheet = new CSSStyleSheet()
sheet.replaceSync(NARSIL_CSS)

// The page sets adoptedStyleSheets of its own at times: the sheet goes back on each change the watch sees.
function adopt() {
  if (!document.adoptedStyleSheets.includes(sheet)) document.adoptedStyleSheets = [...document.adoptedStyleSheets, sheet]
}
adopt()

const send = (message) => host.postMessage(JSON.stringify(message))
let current = null

host.addEventListener('message', (event) => {
  let message
  try {
    message = JSON.parse(event.data)
  } catch {
    return
  }
  current?.deliver(message)
})

function listeners() {
  const list = []
  return { list, addListener: (f) => list.push(f), removeListener: (f) => list.splice(list.indexOf(f) >>> 0, 1) }
}

// One port at a time, as the band opens one: the app follows ntfy.sh while it is connected.
function connect({ name } = {}) {
  const onMessage = listeners()
  const onDisconnect = listeners()
  const port = {
    name,
    onMessage,
    onDisconnect,
    postMessage: (message) => {
      if (current === port) send({ kind: 'port', message })
    },
    disconnect: () => {
      if (current !== port) return
      current = null
      send({ kind: 'disconnect' })
    },
    deliver: (message) => onMessage.list.forEach((f) => f(message)),
  }
  current = port
  send({ kind: 'connect', name })
  return port
}

self.browser = {
  runtime: {
    id: 'narsil-android',
    getURL: (path) => ASSETS + path,
    getManifest: () => ({ content_scripts: [{ css: STYLE_FILES }] }),
    connect,
    // What the band asks for when its styles are gone: they are put back here, as the app holds them.
    sendMessage: async (message) => {
      if (message?.type === 'narsil-missing') adopt()
      return true
    },
    onMessage: listeners(),
  },
}
