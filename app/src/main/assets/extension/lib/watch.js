// One observer of the page for all of the extension's scripts, and what they
// ask for runs once a frame at most: while Claude writes, claude.ai changes
// the page many times a frame, and an observer each would add up.
//
// It stops, and with it every script that asked it for something, once its
// extension goes away (the page keeps the scripts running, without their
// styles) or a newer copy of the scripts reaches the page, as when Chromium's
// background puts them back.

// A stopped one left in this world is not reused: scripts put back here get a new one.
self.narsilWatch = self.narsilWatch?.isAlive() ? self.narsilWatch : (() => {
  const STOP_EVENT = 'narsil-stop'
  const callbacks = []
  const stops = []
  let isQueued = false
  let isStopped = false

  function isOrphan() {
    try {
      return !browser.runtime?.id
    } catch {
      return true
    }
  }

  function stop() {
    if (isStopped) return
    isStopped = true
    observer.disconnect()
    callbacks.length = 0
    for (const callback of stops.splice(0)) callback()
  }

  function flush() {
    isQueued = false
    if (isStopped) return
    if (isOrphan()) {
      stop()
      return
    }
    for (const callback of callbacks) callback()
  }

  const observer = new MutationObserver(() => {
    if (isQueued || isStopped) return
    isQueued = true
    requestAnimationFrame(flush)
  })
  observer.observe(document.documentElement, { subtree: true, childList: true, attributes: true, attributeFilter: ['data-mode', 'href'] })

  // The page's DOM is the only thing two copies share: the one before hears it.
  document.dispatchEvent(new CustomEvent(STOP_EVENT))
  document.addEventListener(STOP_EVENT, stop)

  const watch = (callback) => {
    if (!isStopped) callbacks.push(callback)
  }
  watch.onStop = (callback) => {
    if (isStopped) callback()
    else stops.push(callback)
  }
  watch.stop = stop
  watch.isAlive = () => !isStopped && !isOrphan()
  return watch
})()
