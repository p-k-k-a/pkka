export const AUTH_RETURN_STORAGE_KEY = "navigated_to_auth";
const DISCORD_VERIFICATION_STORAGE_KEY = "discord_verification";

export function markAuthNavigation(): void {
  try {
    sessionStorage.setItem(AUTH_RETURN_STORAGE_KEY, "true");
  } catch {
    // sessionStorage unavailable np. Private browsing.
  }
}

export function markDiscordVerification(): void {
  try {
    sessionStorage.setItem(DISCORD_VERIFICATION_STORAGE_KEY, "true");
  } catch {
    // sessionStorage unavailable np. Private browsing.
  }
}

export function takeDiscordVerification(): boolean {
  try {
    const marked = sessionStorage.getItem(DISCORD_VERIFICATION_STORAGE_KEY) === "true";
    sessionStorage.removeItem(DISCORD_VERIFICATION_STORAGE_KEY);
    return marked;
  } catch {
    return false;
  }
}

// Runs in <head> before React hydrates. After OAuth redirect + browser back, bfcache restores a
// frozen page; this detects the flag and forces a fresh load.
export const authReturnRecoveryScript = `(function(){var k="${AUTH_RETURN_STORAGE_KEY}";function r(){try{if(sessionStorage.getItem(k)==="true"){sessionStorage.removeItem(k);window.location.replace(location.pathname+location.search+location.hash);return true;}}catch(e){}return false;}window.addEventListener("pageshow",function(e){if(e.persisted)r();});r();})();`;
