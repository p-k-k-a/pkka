export function redirectSystemPath({ path, initial }: { path: string; initial: boolean }) {
  if (!initial && /\/login#at=/.test(path)) return null;
  return /\/events\/archive\/?(\?|$)/.test(path) ? "/(tabs)/events" : path;
}
