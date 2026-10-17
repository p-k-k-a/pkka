"use client";

import { PostStatusBadge } from "@/components/admin/post-status-badge";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useAuth } from "@/lib/auth-context";
import { formatPublishedAt, isAdmin } from "@pkka/domain";
import {
  getListAdminPostsQueryKey,
  useDeleteAdminPost,
  useListAdminPosts,
  PostStatus,
  type AdminPostListItemResponse,
} from "@pkka/api";
import { useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, ArrowRight, Calendar, Pencil, Plus, Trash2, UserRound } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

const PAGE_SIZE = 20;

type StatusFilter = "ALL" | PostStatus;

function formatPostCount(count: number) {
  const lastDigit = count % 10;
  const lastTwo = count % 100;
  if (count === 1) return "1 wpis";
  if (lastDigit >= 2 && lastDigit <= 4 && (lastTwo < 12 || lastTwo > 14)) return `${count} wpisy`;
  return `${count} wpisów`;
}

export function PostsList() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { isLoading, user } = useAuth();
  const admin = isAdmin(user?.roles);
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");
  const [postToDelete, setPostToDelete] = useState<AdminPostListItemResponse | null>(null);

  useEffect(() => {
    if (!isLoading && !admin) {
      router.replace("/dashboard");
    }
  }, [admin, isLoading, router]);

  const {
    data: response,
    isLoading: isListLoading,
    isError,
  } = useListAdminPosts(
    { ...(statusFilter === "ALL" ? {} : { status: statusFilter }), page, size: PAGE_SIZE },
    { query: { enabled: admin } },
  );

  const deletePost = useDeleteAdminPost({
    mutation: {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: getListAdminPostsQueryKey() });
        setPostToDelete(null);
      },
    },
  });

  if (isLoading || !admin) {
    return (
      <div className="px-4 py-10 md:px-10 md:py-20">
        <Skeleton className="mx-auto h-40 w-full max-w-3xl rounded-lg" />
      </div>
    );
  }

  const pageData = response?.data;
  const posts = pageData?.content ?? [];
  const totalElements = pageData?.totalElements ?? 0;
  const totalPages = pageData?.totalPages ?? 0;
  const currentPage = pageData?.number ?? page;
  const firstShown = currentPage * (pageData?.size ?? PAGE_SIZE) + 1;
  const lastShown = firstShown + posts.length - 1;

  return (
    <div>
      <section className="bg-background px-4 py-10 md:px-10 md:py-16">
        <div className="mx-auto max-w-[1280px] space-y-6">
          <div className="flex flex-wrap items-start justify-between gap-6">
            <div className="space-y-6">
              <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
                Aktualności
              </h1>
              <p className="text-muted-foreground max-w-2xl text-base leading-relaxed">
                Pisz, edytuj i usuwaj wpisy. Szkice widzą tylko administratorzy, a opublikowane
                wpisy trafiają do zakładki „Aktualności”.
              </p>
            </div>
            <Button asChild className="shrink-0">
              <Link href="/dashboard/posts/new">
                <Plus data-icon="inline-start" />
                Nowy wpis
              </Link>
            </Button>
          </div>
          <Tabs
            value={statusFilter}
            onValueChange={(value) => {
              setStatusFilter(value as StatusFilter);
              setPage(0);
            }}
          >
            <TabsList>
              <TabsTrigger value="ALL">Wszystkie</TabsTrigger>
              <TabsTrigger value={PostStatus.PUBLISHED}>Opublikowane</TabsTrigger>
              <TabsTrigger value={PostStatus.DRAFT}>Szkice</TabsTrigger>
            </TabsList>
          </Tabs>
        </div>
      </section>

      <section className="bg-background px-4 py-10 md:px-10 md:py-16">
        <div className="mx-auto max-w-[1280px]">
          {isListLoading ? (
            <div className="grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-3">
              {Array.from({ length: 3 }).map((_, i) => (
                <Skeleton key={i} className="bg-muted h-64 w-full rounded-none" />
              ))}
            </div>
          ) : isError ? (
            <p className="text-destructive font-medium">Nie udało się załadować wpisów.</p>
          ) : posts.length === 0 ? (
            <p className="text-muted-foreground">
              {statusFilter === "ALL"
                ? "Brak wpisów — napisz pierwszy."
                : "Brak wpisów o tym statusie."}
            </p>
          ) : (
            <div className="space-y-8">
              <p className="text-muted-foreground text-sm">
                {totalPages > 1
                  ? `Wyświetlono ${firstShown}–${lastShown} z ${totalElements} wpisów, strona ${currentPage + 1} z ${totalPages}.`
                  : `Wyświetlono ${formatPostCount(totalElements)}.`}
              </p>

              <div className="grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-3">
                {posts.map((post) => {
                  const published = post.status === PostStatus.PUBLISHED;
                  const { dateLabel } = formatPublishedAt(
                    published ? post.publishedAt : post.createdAt,
                  );

                  return (
                    <Card
                      key={post.id}
                      className="bg-muted flex h-full flex-col gap-6 overflow-visible rounded-none p-6 shadow-none ring-0"
                    >
                      <PostStatusBadge status={post.status} />
                      <h2 className="font-heading text-foreground text-lg leading-snug font-semibold">
                        {post.title}
                      </h2>
                      <ul className="text-muted-foreground space-y-3 text-sm">
                        <li className="flex items-start gap-3">
                          <Calendar className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
                          <span>
                            {published ? "Opublikowano" : "Utworzono"} {dateLabel}
                          </span>
                        </li>
                        <li className="flex items-start gap-3">
                          <UserRound className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
                          <span className="min-w-0 break-words">{post.authorDisplayName}</span>
                        </li>
                      </ul>
                      <div className="mt-auto flex flex-wrap items-center justify-end gap-2 pt-2">
                        <Button asChild variant="outline" size="sm">
                          <Link href={`/dashboard/posts/${post.id}`}>
                            <Pencil data-icon="inline-start" />
                            Edytuj
                          </Link>
                        </Button>
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          className="text-destructive hover:text-destructive"
                          onClick={() => setPostToDelete(post)}
                        >
                          <Trash2 data-icon="inline-start" />
                          Usuń
                        </Button>
                      </div>
                    </Card>
                  );
                })}
              </div>

              {totalPages > 1 ? (
                <div className="flex items-center justify-between gap-4 pt-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={currentPage <= 0}
                    onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                  >
                    <ArrowLeft data-icon="inline-start" />
                    Poprzednia
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={currentPage >= totalPages - 1}
                    onClick={() => setPage((prev) => prev + 1)}
                  >
                    Następna
                    <ArrowRight data-icon="inline-end" />
                  </Button>
                </div>
              ) : null}
            </div>
          )}
        </div>
      </section>

      <AlertDialog
        open={postToDelete !== null}
        onOpenChange={(open) => !open && setPostToDelete(null)}
      >
        <AlertDialogContent onOverlayClick={() => !deletePost.isPending && setPostToDelete(null)}>
          <AlertDialogHeader>
            <AlertDialogTitle>Usunąć wpis?</AlertDialogTitle>
            <AlertDialogDescription>
              „{postToDelete?.title}” zostanie trwale usunięty. Tej operacji nie można cofnąć.
            </AlertDialogDescription>
          </AlertDialogHeader>
          {deletePost.isError ? (
            <p className="text-destructive text-sm font-medium">Nie udało się usunąć wpisu.</p>
          ) : null}
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deletePost.isPending}>Anuluj</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              disabled={deletePost.isPending}
              onClick={(event) => {
                event.preventDefault();
                if (postToDelete) {
                  deletePost.mutate({ id: postToDelete.id });
                }
              }}
            >
              {deletePost.isPending ? "Usuwanie…" : "Usuń wpis"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
