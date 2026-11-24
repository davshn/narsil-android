// Narsil in claude.ai. In the sidebar, under the search: her level, the
// account's limits and ntfy.sh's count. Above the box where you write: the
// open conversation's state, context, cache and last event. Both in the page's
// own flow, so they cover nothing. Text only ever goes in as text.

(() => {
  const BAND_ID = 'narsil-band'
  const RECONNECT_MS = 3_000
  const REDRAW_MS = 15_000
  // Chromium puts an idle background worker to sleep after 30 s, and with it
  // the stream: a word on the port while the band shows keeps it awake.
  const KEEP_AWAKE_MS = 25_000
  const POLL_MS = 10_000
  const USAGE_MS = 60_000
  const L = self.narsilLive
  const N = self.narsilState
  const EVENT_ID = 'narsil-event'
  // Once per page observer: scripts put back in this same world run again.
  if (self.narsilBandLoaded === self.narsilWatch) return
  self.narsilBandLoaded = self.narsilWatch
  // What an earlier copy left on the page, when its world is gone with it.
  document.getElementById(BAND_ID)?.remove()
  document.getElementById(EVENT_ID)?.remove()

  function el(tag, className, text, ...children) {
    const node = document.createElement(tag)
    if (className) node.className = className
    if (text) node.textContent = text
    node.append(...children)
    return node
  }

  // Only what changed is written: part of this runs on the page's mutations,
  // and a write of its own would be one more.
  function setText(node, text) {
    if (node.textContent !== text) node.textContent = text
  }

  const packagedArt = browser.runtime.getURL('icons/icon-192.png')

  // A picture that fails is not asked for again: the packaged one stands in.
  function petImage(className) {
    const image = el('img', className)
    image.alt = ''
    let broken = null
    image.addEventListener('error', () => {
      if (image.getAttribute('src') === packagedArt) return
      broken = image.getAttribute('src')
      image.src = packagedArt
    })
    return (url) => {
      const wanted = url && url !== broken ? url : packagedArt
      if (image.getAttribute('src') !== wanted) image.src = wanted
      return image
    }
  }

  // The sidebar's band.
  const setPet = petImage('narsil-pet')
  const levelName = el('span', 'narsil-level-name')
  const levelXp = el('span', 'narsil-note')
  const levelFill = el('i')
  const head = el('div', 'narsil-head', '', setPet(null), el('div', 'narsil-level', '', el('div', 'narsil-meter-top', '', levelName, levelXp), el('div', 'narsil-bar', '', levelFill)))
  head.hidden = true
  const notice = el('button', 'narsil-notice')
  notice.type = 'button'
  notice.hidden = true
  const meters = el('div', 'narsil-meters')
  const band = el('div', '', '', head, notice, meters)
  band.id = BAND_ID

  // The open conversation's strip, above the box where you write.
  const setEventPet = petImage('narsil-event-pet')
  const eventChip = el('span', 'narsil-chip')
  const eventContext = el('span', 'narsil-note')
  const eventCache = el('span', 'narsil-note')
  const eventWhen = el('span', 'narsil-note')
  const eventText = el('div', 'narsil-event-text')
  const eventBody = el('div', 'narsil-event-body', '', el('div', 'narsil-event-top', '', eventChip, el('span', 'narsil-event-stats', '', eventContext, eventCache), eventWhen), eventText)
  const eventBar = el('div', '', '', setEventPet(null), eventBody)
  eventBar.id = EVENT_ID
  eventBar.addEventListener('click', () => eventBar.classList.toggle('narsil-open'))

  let port = null
  let latest = null
  let limits = null
  let live = null
  let polledFor = null
  let isPolling = false

  // As the page itself, with its session: a content script's own fetch is not.
  const pageFetch = typeof content !== 'undefined' && content.fetch ? content.fetch.bind(content) : fetch

  async function getJson(url) {
    const response = await pageFetch(url, { credentials: 'include', headers: { 'anthropic-version': '2023-06-01' } })
    if (!response.ok) throw new Error(`claude.ai respondió ${response.status}`)
    return JSON.parse(await response.text())
  }

  function meter({ label, percent, color, note }) {
    const fill = el('i')
    fill.style.width = `${Math.min(100, percent)}%`
    fill.style.background = color
    const value = el('b', '', `${percent} %`)
    value.style.color = color
    return el('div', 'narsil-meter', '', el('div', 'narsil-meter-top', '', el('span', '', label), value), el('div', 'narsil-bar', '', fill), ...(note ? [el('div', 'narsil-note', note)] : []))
  }

  function renderBand() {
    const now = Date.now() / 1000
    const view = latest?.type === 'model' ? N.viewOf(latest.model, now) : null
    const notes = { sealed: '🔒 Toca y pon la clave' }
    const message = latest?.type === 'no-topic' ? 'Toca y escribe tu topic' : notes[view?.kind]
    notice.hidden = !message
    setText(notice, message || '')
    const level = view?.level
    head.hidden = !level
    if (level) {
      setPet(L.themedArt(view.art, document.documentElement.dataset.mode === 'light'))
      setText(levelName, level.name)
      setText(levelXp, `${level.into}/${level.needed} XP`)
      levelFill.style.width = `${Math.min(100, level.percent)}%`
    }
    meters.replaceChildren(...[...L.limitsOf(limits, now), ...(view?.ntfy ? [view.ntfy] : [])].map(meter))
  }

  function drawEvent() {
    const id = L.sessionIdOf(location.pathname)
    if (id !== polledFor) {
      polledFor = id
      live = null
      pollLive()
    }
    const composer = document.querySelector('[data-cds=ChatComposer]')
    const view = composer && live?.id === id ? L.liveOf(live, L.cacheMinutesOf(latest?.model), Date.now() / 1000) : null
    if (!view) {
      eventBar.remove()
      return
    }
    setEventPet(L.petOf(live.session.status_bucket, latest?.model?.state?.art, document.documentElement.dataset.mode === 'light'))
    setText(eventChip, view.label)
    setText(eventContext, view.context ? `Contexto ${view.context.percent} %` : '')
    eventContext.style.color = view.context?.color ?? ''
    setText(eventCache, view.cache?.text ?? '')
    eventCache.style.color = view.cache?.color ?? ''
    setText(eventWhen, view.when)
    setText(eventText, view.text)
    eventText.style.display = view.text ? '' : 'none'
    eventBar.style.setProperty('--narsil-state', view.color)
    if (composer.previousElementSibling !== eventBar) composer.before(eventBar)
  }

  async function pollLive() {
    const id = L.sessionIdOf(location.pathname)
    if (!id || document.hidden || isPolling) return
    isPolling = true
    try {
      const [sessions, events] = await Promise.all([getJson(L.sessionsUrl()), getJson(L.eventsUrl(id))])
      const session = L.findSession(sessions.data, id)
      live = session ? { id, session, events: Array.isArray(events.data) ? events.data : [] } : null
    } catch {
      // What it last knew stands until the next try.
    } finally {
      isPolling = false
    }
    drawEvent()
  }

  async function pollUsage() {
    const url = L.usageUrl(document.cookie)
    if (!url || document.hidden) return
    try {
      limits = await getJson(url)
    } catch {
      return
    }
    renderBand()
  }

  // Under the search, found by where it sits and so in any language: on a phone
  // the sidebar is a sheet with the search in its header; on a wider screen
  // it is the only direct button of the list's first block. The page mounts
  // the sidebar late and again at times: she goes back under it.
  const PHONE_SEARCH = '.df-phone-sheet-header'
  const WIDE_SEARCH = '.dframe-nav-scroll > .shrink-0 > button'
  function place() {
    const search = document.querySelector(PHONE_SEARCH) || document.querySelector(WIDE_SEARCH)
    if (search && search.nextElementSibling !== band) search.after(band)
    drawEvent()
  }

  self.narsilWatch(place)

  notice.addEventListener('click', () => port?.postMessage({ type: 'settings' }))

  const timers = []

  // Once the extension is reloaded or removed, this copy stays on the page
  // with nothing behind it: it takes its parts away and stops.
  function isOrphan() {
    try {
      return !browser.runtime?.id
    } catch {
      return true
    }
  }

  // The background asks whether a page still has its scripts (lib/restore.js).
  function answerPing(message, _sender, reply) {
    if (message?.type === 'narsil-ping') reply(true)
  }

  // Vivaldi, starting again after Android closed it, may run these scripts
  // without the manifest's styles, or without the document_start group (the
  // first, which leaves self.narsilMode in this same world): the background
  // puts them on (lib/restore.js).
  const FIRST_GROUP = 0
  function askForMissing() {
    const R = self.narsilRestore
    const root = getComputedStyle(document.documentElement)
    const files = R.missingStyles(browser.runtime.getManifest(), (name) => root.getPropertyValue(name))
    const groups = self.narsilMode ? [] : [FIRST_GROUP]
    if (files.length > 0 || groups.length > 0) browser.runtime.sendMessage({ type: R.MISSING, files, groups }).catch(() => undefined)
  }

  // On the page observer's stop: the extension went away or a newer copy came.
  let isStopped = false
  function stop() {
    isStopped = true
    timers.forEach(clearInterval)
    try {
      browser.runtime.onMessage.removeListener(answerPing)
    } catch {
      // Gone with the extension.
    }
    const old = port
    port = null
    try {
      old?.disconnect()
    } catch {
      // Gone with the extension.
    }
    band.remove()
    eventBar.remove()
  }
  self.narsilWatch.onStop(stop)

  function connect() {
    if (isStopped || port || document.hidden) return
    if (isOrphan()) {
      self.narsilWatch.stop()
      return
    }
    try {
      port = browser.runtime.connect({ name: 'narsil-band' })
    } catch {
      self.narsilWatch.stop()
      return
    }
    port.onMessage.addListener((message) => {
      latest = message
      renderBand()
      drawEvent()
    })
    port.onDisconnect.addListener(() => {
      port = null
      setTimeout(connect, RECONNECT_MS)
    })
  }

  function disconnect() {
    port?.disconnect()
    port = null
  }

  document.addEventListener('visibilitychange', () => {
    if (isStopped) return
    if (document.hidden) {
      disconnect()
      return
    }
    connect()
    pollLive()
    pollUsage()
  })
  timers.push(
    setInterval(() => port?.postMessage({ type: 'awake' }), KEEP_AWAKE_MS),
    setInterval(pollLive, POLL_MS),
    setInterval(pollUsage, USAGE_MS),
    setInterval(() => {
      if (document.hidden) return
      renderBand()
      drawEvent()
    }, REDRAW_MS),
  )
  browser.runtime.onMessage.addListener(answerPing)
  askForMissing()
  renderBand()
  place()
  connect()
  pollUsage()
})()
