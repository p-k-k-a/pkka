const PAGES_WITH_TWIN = /^\/(events|announcements)(\/|$)/;

const withoutDashboard = (path: string) => path.replace(/^\/dashboard(?=\/)/, "");

export function currentPage() {
  return `${window.location.pathname}${window.location.search}${window.location.hash}`;
}

export function dashboardTwin(path: string) {
  const page = withoutDashboard(path);
  return PAGES_WITH_TWIN.test(page) ? `/dashboard${page}` : "/dashboard";
}

export function publicTwin(path: string) {
  const page = withoutDashboard(path);
  return PAGES_WITH_TWIN.test(page) ? page : "/";
}
