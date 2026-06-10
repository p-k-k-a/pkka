export const DEFAULT_COVER_IMAGE = "/hero.png";

export function coverImageSrc(imageUrl?: string, fallback = DEFAULT_COVER_IMAGE) {
  return imageUrl?.startsWith("http") ? imageUrl : fallback;
}

export function remoteCoverImageSrc(imageUrl?: string) {
  return imageUrl?.startsWith("http") ? imageUrl : null;
}
