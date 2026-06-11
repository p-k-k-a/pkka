export const AUTH_RETURN_STORAGE_KEY = "navigated_to_auth";

export function markAuthNavigation(): void {
  try {
    sessionStorage.setItem(AUTH_RETURN_STORAGE_KEY, "true");
  } catch {
    // sessionStorage unavailable (e.g. private browsing) — recovery won't run, but login still works
  }
}

// Runs in <head> before React hydrates. After OAuth redirect + browser back, bfcache restores a
// frozen page; this detects the flag and forces a fresh load.
export const authReturnRecoveryScript = `(function(){var k="${AUTH_RETURN_STORAGE_KEY}";function r(){try{if(sessionStorage.getItem(k)==="true"){sessionStorage.removeItem(k);window.location.replace(location.pathname+location.search+location.hash);return true;}}catch(e){}return false;}window.addEventListener("pageshow",function(e){if(e.persisted)r();});r();})();`;
