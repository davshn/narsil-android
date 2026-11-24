// What the band keeps of the state the mod sends through ntfy.sh, and what it
// shows of it: Narsil's level and art, and ntfy.sh's own count, which only the
// PC can ask for, as ntfy.sh counts by IP. A classic script: the background
// page loads it after seal.js, the content script to draw.

self.narsilState = (() => {
  const STALE_SECONDS = 3600
  // Without a key anyone who knows the topic can publish on it: its text only
  // ever goes in as text, and an image only comes from Narsil's own assets.
  const ART_PREFIX = 'https://raw.githubusercontent.com/davshn/narsil-pet/main/assets/'

  const isTopic = (topic) => typeof topic === 'string' && /^[-_A-Za-z0-9]{1,64}$/.test(topic)
  const statusTopic = (topic) => (isTopic(topic) ? `${topic}-estado` : null)
  const pollUrl = (topic) => (isTopic(topic) ? `https://ntfy.sh/${statusTopic(topic)}/json?poll=1&since=12h` : null)
  const streamUrl = (topic) => (isTopic(topic) ? `https://ntfy.sh/${statusTopic(topic)}/json` : null)

  const emptyModel = () => ({ state: null, isSealed: false })

  // What came from ntfy.sh, opened first when sealed.
  async function receive(model, raw, key) {
    if (raw?.event !== 'message') return model
    const m = await self.narsilSeal.unseal(key, raw)
    if (m.sealed) return { ...model, isSealed: true }
    let state
    try {
      state = JSON.parse(m.message)
    } catch {
      return model
    }
    if (!state || typeof state !== 'object') return model
    if ((model.state?.at ?? 0) > (state.at ?? 0)) return model
    return { state, isSealed: false }
  }

  // As the band colors a fill: green, then amber, then red.
  const levelColor = (percent) => (percent >= 85 ? '#F0435A' : percent >= 60 ? '#F5B400' : '#22B573')

  // As the band words a span: "45 min", "2 h 05 min", "3 d 4 h".
  function minutesLabel(minutes) {
    if (minutes >= 24 * 60) return `${Math.floor(minutes / 1440)} d ${Math.floor((minutes % 1440) / 60)} h`
    if (minutes >= 60) return `${Math.floor(minutes / 60)} h ${String(minutes % 60).padStart(2, '0')} min`
    return `${minutes} min`
  }

  function ago(seconds, now) {
    const minutes = Math.round((now - seconds) / 60)
    if (minutes < 1) return 'ahora mismo'
    if (minutes < 60) return `hace ${minutes} min`
    const hours = Math.round(minutes / 60)
    return hours < 24 ? `hace ${hours} h` : `hace ${Math.round(hours / 24)} d`
  }

  function resetLabel(resetsAt, now) {
    const left = Math.max(0, Math.round((resetsAt - now) / 60))
    return left > 0 ? `reinicia en ${minutesLabel(left)}` : 'ya se reinició'
  }

  // "⭐ Nivel 4 · 350/400 XP": the name, and how far into the level.
  function levelOf(xp) {
    const m = /^(.*?)\s*·\s*(\d+)\/(\d+)\s*XP/.exec(xp || '')
    if (!m) return null
    const [into, needed] = [Number(m[2]), Number(m[3])]
    return { name: `Narsil ${m[1]}`, into, needed, percent: Math.round((into / needed) * 100) }
  }

  function ntfyOf(s, now) {
    const row = Array.isArray(s.usage) ? s.usage.find((r) => r.key === 'ntfy') : null
    if (!row) return null
    const isStale = Boolean(s.at) && now - s.at > STALE_SECONDS
    const note = [row.note, row.resetsAt ? resetLabel(row.resetsAt, now) : '', isStale ? `dato de ${ago(s.at, now)}` : ''].filter(Boolean).join(' · ')
    return { label: row.label, percent: Math.round(row.percent), color: row.color, note }
  }

  function viewOf(model, now) {
    if (!model.state) return { kind: model.isSealed ? 'sealed' : 'empty' }
    const s = model.state
    return {
      kind: 'state',
      art: typeof s.art === 'string' && s.art.startsWith(ART_PREFIX) ? s.art : null,
      level: levelOf(s.xp),
      ntfy: ntfyOf(s, now),
    }
  }

  return { statusTopic, pollUrl, streamUrl, emptyModel, receive, viewOf, levelColor, ago, resetLabel }
})()
