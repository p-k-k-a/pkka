type PageResponse<T> = {
  data: { content?: T[]; number?: number; totalPages?: number };
};

export function nextPageParam(lastPage: PageResponse<unknown>) {
  const next = (lastPage.data.number ?? 0) + 1;
  return next < (lastPage.data.totalPages ?? 0) ? next : undefined;
}

export function pageItems<T>(pages: PageResponse<T>[] | undefined): T[] {
  return pages?.flatMap((page) => page.data.content ?? []) ?? [];
}

export const END_REACHED_THRESHOLD = 2;
