export const DEFAULT_COVER_IMAGE = "/hero.png";

export function coverImageSrc(imageUrl?: string, fallback = DEFAULT_COVER_IMAGE) {
  return imageUrl?.startsWith("http") ? imageUrl : fallback;
}

export function remoteCoverImageSrc(imageUrl?: string) {
  return imageUrl?.startsWith("http") ? imageUrl : null;
}

// Files uploaded to the club's storage (Cloudflare R2) are served from here. The same host is
// listed in next.config.ts, which is what lets the image optimizer fetch them.
const STORAGE_HOST_SUFFIX = ".r2.cloudflarestorage.com";

/**
 * Whether next/image may optimize this URL. A cover can also be a link to any other site,
 * and the optimizer refuses hosts it was not configured for, so those are served as they are.
 */
export function isOptimizableImageUrl(imageUrl: string) {
  try {
    return new URL(imageUrl).hostname.endsWith(STORAGE_HOST_SUFFIX);
  } catch {
    return false;
  }
}
