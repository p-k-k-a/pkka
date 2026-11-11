export function redirectSystemPath({ path }: { path: string }) {
  if (/\/events\/archive\/?(\?|$)/.test(path)) return "/(tabs)/events";
  return path.replace(/\/announcements\//, "/blog/");
}
