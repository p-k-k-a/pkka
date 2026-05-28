import {
  getGetAnnouncementsResponseMock,
  getGetAnnouncementsMockHandler,
  getGetBlogPostBySlugMockHandler,
  getGetBlogPostBySlugResponseMock,
} from "@pkka/api/msw";
import { enrichAnnouncements, enrichBlogPost } from "@/lib/mocks/enrich-announcements";

const isDev = process.env.NODE_ENV === "development";

let mswReady = !isDev;
let mswInitPromise: Promise<void> | null = null;

export function isMswReady() {
  return mswReady;
}

export function ensureDevMsw() {
  if (!isDev || mswReady) return Promise.resolve();
  if (mswInitPromise) return mswInitPromise;

  mswInitPromise = (async () => {
    const { faker } = await import("@faker-js/faker");
    const { setupWorker } = await import("msw/browser");

    const mockPool = getGetAnnouncementsResponseMock().slice(0, 6);
    const enrichedPool = enrichAnnouncements(mockPool, faker);

    const worker = setupWorker(
      getGetAnnouncementsMockHandler(() => enrichedPool),
      getGetBlogPostBySlugMockHandler(async (info) => {
        const slug = info.params.slug as string;
        const existing = enrichedPool.find((item) => item.slug === slug);

        if (existing) {
          return {
            ...existing,
            publishedAt: new Date().toISOString(),
          };
        }

        return enrichBlogPost(getGetBlogPostBySlugResponseMock({ slug }), faker);
      }),
    );

    await worker.start({ onUnhandledRequest: "bypass" });
    mswReady = true;
  })();

  return mswInitPromise;
}
