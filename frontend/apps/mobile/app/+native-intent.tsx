export function redirectSystemPath({ path }: { path: string; initial: boolean }) {
  return /\/events\/archive\/?(\?|$)/.test(path) ? "/(tabs)/events" : path;
}
