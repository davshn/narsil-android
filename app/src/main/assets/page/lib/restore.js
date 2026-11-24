// When the extension goes away with claude.ai open (Vivaldi reloads or
// updates it, or Android unloads it), Chromium takes its styles off the page
// and never puts its scripts or styles back. Each time the background starts,
// a page whose scripts do not answer gets them again, group by group, in the
// manifest's order. Firefox puts them back itself and has no scripting API
// here, so it does nothing there.

self.narsilRestore = (() => {
  const PING = 'narsil-ping'

  async function answers(browser, tabId) {
    try {
      return (await browser.tabs.sendMessage(tabId, { type: PING })) === true
    } catch {
      return false
    }
  }

  async function restoreTab(browser, groups, tabId) {
    const target = { tabId }
    for (const group of groups) {
      if (group.css?.length) await browser.scripting.insertCSS({ target, files: group.css })
      if (group.js?.length) await browser.scripting.executeScript({ target, files: group.js })
    }
  }

  async function restorePages(browser) {
    if (!browser.scripting) return
    const groups = browser.runtime.getManifest().content_scripts || []
    const url = [...new Set(groups.flatMap((group) => group.matches))]
    if (url.length === 0) return
    let tabs = []
    try {
      tabs = await browser.tabs.query({ url })
    } catch {
      return
    }
    for (const tab of tabs) {
      if (await answers(browser, tab.id)) continue
      try {
        await restoreTab(browser, groups, tab.id)
      } catch {
        // A page it cannot reach (discarded, or still loading) gets them on its next load.
      }
    }
  }

  // Vivaldi on Android, starting again after Android closed it, may run only
  // some of the scripts on a page, or none of the styles: the scripts that run
  // answer the ping, so the page asks for what it misses. Each stylesheet sets
  // a mark on :root (--narsil-css-<name>: 1) for the page to tell.
  const MISSING = 'narsil-missing'

  const markOf = (file) => `--narsil-css-${file.split('/').pop().replace(/\.css$/, '')}`
  const manifestStyles = (manifest) => [...new Set((manifest.content_scripts || []).flatMap((group) => group.css || []))]

  // `read` gives a custom property of the page's root, '' when it is not set.
  function missingStyles(manifest, read) {
    return manifestStyles(manifest).filter((file) => read(markOf(file)).trim() === '')
  }

  const listOf = (value) => (Array.isArray(value) ? value : [])

  // The background's answer to a page asking for its styles (`files`) and for
  // the groups of scripts that did not run (`groups`, indexes in the
  // manifest's content_scripts): only the manifest's, styles first.
  function answerMissing(browser) {
    return (message, sender) => {
      if (message?.type !== MISSING || !browser.scripting || sender?.tab?.id === undefined) return undefined
      const manifest = browser.runtime.getManifest()
      const known = manifestStyles(manifest)
      const files = listOf(message.files).filter((file) => known.includes(file))
      const scripts = listOf(message.groups)
        .map((index) => manifest.content_scripts?.[index]?.js)
        .filter((js) => Array.isArray(js) && js.length > 0)
      if (files.length === 0 && scripts.length === 0) return undefined
      const target = { tabId: sender.tab.id, frameIds: [sender.frameId ?? 0] }
      return (async () => {
        if (files.length > 0) await browser.scripting.insertCSS({ target, files })
        for (const js of scripts) await browser.scripting.executeScript({ target, files: js })
        return true
      })().catch(() => false)
    }
  }

  return { PING, MISSING, restorePages, missingStyles, answerMissing }
})()
