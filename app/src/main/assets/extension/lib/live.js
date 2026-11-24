// What the strip above the prompt makes of what claude.ai says of a session:
// the same data the page itself reads, so it needs no message from the PC.
// These addresses are not a public API and may change.

self.narsilLive = (() => {
  const ORIGIN = 'https://claude.ai'
  const DEFAULT_CACHE_MINUTES = 60
  const STATES = {
    working: { label: 'TRABAJANDO', color: '#3B8BFF' },
    blocked: { label: 'TE NECESITA', color: '#F5B400' },
    completed: { label: 'TERMINADA', color: '#22B573' },
  }
  const AT_REST = { label: 'EN REPOSO', color: '#8A90A6' }
  const ART = 'https://raw.githubusercontent.com/davshn/narsil-pet/main/assets/'
  const POSES = { working: 'trabajando', blocked: 'necesita', completed: 'terminado' }

  const sessionIdOf = (pathname) => /^\/code\/(session_[A-Za-z0-9]+)/.exec(pathname)?.[1] ?? null
  const sessionsUrl = () => `${ORIGIN}/v1/code/sessions?statuses=active&statuses=paused&limit=50`
  const eventsUrl = (id) => `${ORIGIN}/v1/code/sessions/${id}/events?limit=60&sort_order=desc`

  // The address says session_x and the list may say cse_x of the same one.
  const bareId = (id) => String(id).replace(/^[a-z]+_/, '')
  const findSession = (list, id) => (Array.isArray(list) ? list.find((s) => bareId(s.id) === bareId(id)) ?? null : null)

  // How long the cache lasts is the mod's to know: an hour on a plan.
  function cacheMinutesOf(model) {
    const state = model?.state
    const fromSession = Array.isArray(state?.sessions) ? state.sessions.map((c) => c.cacheMinutes).find((m) => m > 0) : undefined
    return fromSession ?? (state?.cacheMinutes > 0 ? state.cacheMinutes : DEFAULT_CACHE_MINUTES)
  }

  const contentOf = (event) => (Array.isArray(event.payload?.message?.content) ? event.payload.message.content : [])
  const secondsOf = (event) => Date.parse(event.created_at) / 1000

  // `events`: the newest first. Thinking alone says nothing.
  function lastSaid(events) {
    for (const event of events) {
      if (event.event_type !== 'assistant') continue
      const content = contentOf(event)
      const words = content.filter((b) => b.type === 'text' && b.text?.trim()).map((b) => b.text.trim()).join('\n')
      if (words) return { text: words, at: secondsOf(event) }
      const tool = content.filter((b) => b.type === 'tool_use').pop()
      if (tool) return { text: `Usando ${tool.name}`, at: secondsOf(event) }
    }
    return null
  }

  // From the model's last message: the session's last event may be anything,
  // and opening the page makes some of its own.
  function cacheOf(events, cacheMinutes, now) {
    const last = events.find((e) => e.event_type === 'assistant')
    if (!last) return null
    const left = Math.max(0, Math.ceil((secondsOf(last) + cacheMinutes * 60 - now) / 60))
    return { text: left > 0 ? `🧊 ${left} min` : '🧊 fría', color: self.narsilState.levelColor(100 - (left / cacheMinutes) * 100) }
  }

  function contextOf(session) {
    const usage = session.external_metadata?.context_usage
    if (!(usage?.max_tokens > 0) || typeof usage.used_tokens !== 'number') return null
    const percent = Math.round((usage.used_tokens / usage.max_tokens) * 100)
    return { percent, color: self.narsilState.levelColor(percent) }
  }

  function liveOf({ session, events }, cacheMinutes, now) {
    const said = lastSaid(events)
    return {
      ...(STATES[session.status_bucket] ?? AT_REST),
      text: said?.text ?? '',
      when: said ? self.narsilState.ago(said.at, now) : '',
      cache: cacheOf(events, cacheMinutes, now),
      context: contextOf(session),
    }
  }

  // Her stage is the mod's to know: it comes in the art of its last state,
  // with the first stage at the root and each later one in its folder.
  function petOf(bucket, sentArt, isLight) {
    if (typeof sentArt !== 'string' || !sentArt.startsWith(ART)) return null
    const stage = /^etapa\d+\//.exec(sentArt.slice(ART.length))?.[0] ?? ''
    return `${ART}${stage}${POSES[bucket] ?? 'parada'}-${isLight ? 'claro' : 'oscuro'}.png`
  }

  // The mod sends her art in the dark variant: the light one sits beside it.
  const themedArt = (url, isLight) => (url && isLight ? url.replace(/-oscuro\.png$/, '-claro.png') : url)

  // The account the page has open is in its cookie.
  function usageUrl(cookie) {
    const org = /(?:^|; )lastActiveOrg=([0-9a-f-]{36})(?:;|$)/.exec(cookie || '')?.[1]
    return org ? `${ORIGIN}/api/organizations/${org}/usage` : null
  }

  function limitOf(label, window, now) {
    if (typeof window?.utilization !== 'number') return null
    const note = window.resets_at ? self.narsilState.resetLabel(Date.parse(window.resets_at) / 1000, now) : ''
    return { label, percent: Math.round(window.utilization), color: self.narsilState.levelColor(window.utilization), note }
  }

  const limitsOf = (usage, now) => [limitOf('5 horas', usage?.five_hour, now), limitOf('Semana', usage?.seven_day, now)].filter(Boolean)

  return { sessionIdOf, sessionsUrl, eventsUrl, findSession, cacheMinutesOf, liveOf, petOf, themedArt, usageUrl, limitsOf }
})()
