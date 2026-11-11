export function redirectSystemPath({ path }: { path: string }) {
  return /\/events\/archive\/?(\?|$)/.test(path) ? "/(tabs)/events" : path;
}
