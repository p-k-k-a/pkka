const PUBLIC_WITH_TWIN = /^\/(events|announcements)(\/|$)/;
const DASHBOARD_WITH_TWIN = /^\/dashboard\/(events|announcements)(\/|$)/;

function rest() {
  return `${window.location.search}${window.location.hash}`;
}

export function dashboardTwinOfCurrentPage() {
  const { pathname } = window.location;
  return PUBLIC_WITH_TWIN.test(pathname) ? `/dashboard${pathname}${rest()}` : "/dashboard";
}

export function publicTwinOfCurrentPage() {
  const { pathname } = window.location;
  return DASHBOARD_WITH_TWIN.test(pathname)
    ? `${pathname.slice("/dashboard".length)}${rest()}`
    : "/";
}
