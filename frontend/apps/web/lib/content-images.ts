export function remoteCoverImageSrc(imageUrl?: string) {
  return imageUrl?.startsWith("http") ? imageUrl : null;
}
