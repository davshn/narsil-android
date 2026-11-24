// Light by day and dark by night, whatever claude.ai's own setting says: its
// data-mode attribute, on every element that carries one, as the page sets it
// again whenever it mounts one. Not every data-mode: data-mode=code means
// something else.

(() => {
  const MODES = '[data-mode=light],[data-mode=dark],[data-mode=system]'
  const CHECK_MS = 60_000
  function apply() {
    const mode = self.narsilMode.modeOfHour(new Date().getHours())
    for (const element of document.querySelectorAll(MODES)) if (element.dataset.mode !== mode) element.dataset.mode = mode
  }

  self.narsilWatch(apply)

  // The hour turns with no change on the page; a hidden page runs no frames.
  const timer = setInterval(apply, CHECK_MS)
  const onVisibility = () => apply()
  document.addEventListener('visibilitychange', onVisibility)
  self.narsilWatch.onStop(() => {
    clearInterval(timer)
    document.removeEventListener('visibilitychange', onVisibility)
  })
  apply()
})()
