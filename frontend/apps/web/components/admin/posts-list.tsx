"use client";

import { PostStatusBadge } from "@/components/admin/post-status-badge";
import { SectionShell } from "@/components/content/section-shell";
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
import { formatPublishedAt } from "@pkka/domain";
import { isAdmin } from "@pkka/domain";
import {
  getListAdminPostsQueryKey,
  useDeleteAdminPost,
  useListAdminPosts,
  type AdminPostListItemResponse,
  type ListAdminPostsParams,
} from "@pkka/api";
import { useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, ArrowRight, Pencil, Plus, Trash2 } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

const PAGE_SIZE = 20;

function PostCardSkeleton() {
  return (
    <Card className="gap-3 p-5 md:p-6">
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0 space-y-1">
          <Skeleton className="h-4 w-44" />
          <Skeleton className="h-7 w-64" />
          <Skeleton className="h-4 w-28" />
        </div>
        <Skeleton className="h-5 w-24 rounded-4xl" />
      </div>
      <div className="flex items-center justify-end gap-2">
        <Skeleton className="h-[34px] w-24 rounded-md" />
        <Skeleton className="h-[34px] w-24 rounded-md" />
      </div>
    </Card>
  );
}

type StatusFilter = "ALL" | NonNullable<ListAdminPostsParams["status"]>;

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
      <SectionShell title="Blog" as="section">
        <Skeleton className="h-40 w-full max-w-3xl rounded-2xl" />
      </SectionShell>
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
    <SectionShell title="Blog" description="Wpisy na blogu klubu Alumna." as="section">
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <Tabs
          value={statusFilter}
          onValueChange={(value) => {
            setStatusFilter(value as StatusFilter);
            setPage(0);
          }}
        >
          <TabsList>
            <TabsTrigger value="ALL">Wszystkie</TabsTrigger>
            <TabsTrigger value="PUBLISHED">Opublikowane</TabsTrigger>
            <TabsTrigger value="DRAFT">Szkice</TabsTrigger>
          </TabsList>
        </Tabs>
        <Button asChild>
          <Link href="/dashboard/posts/new">
            <Plus data-icon="inline-start" />
            Nowy wpis
          </Link>
        </Button>
      </div>

      {isListLoading ? (
        <div className="space-y-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <PostCardSkeleton key={i} />
          ))}
        </div>
      ) : isError ? (
        <p className="text-destructive font-medium">Nie udało się załadować wpisów.</p>
      ) : posts.length === 0 ? (
        <p className="text-muted-foreground">
          {statusFilter === "ALL" ? "Brak wpisów." : "Brak wpisów o tym statusie."}
        </p>
      ) : (
        <div className="space-y-4">
          <p className="text-muted-foreground text-sm">
            {totalPages > 1
              ? `Wyświetlono ${firstShown}–${lastShown} z ${totalElements} wpisów, strona ${currentPage + 1} z ${totalPages}.`
              : `Wyświetlono ${totalElements} ${totalElements === 1 ? "wpis" : "wpisów"}.`}
          </p>

          {posts.map((post) => {
            const { dateLabel } = formatPublishedAt(
              post.status === "PUBLISHED" ? post.publishedAt : post.createdAt,
            );

            return (
              <Card key={post.id} className="gap-3 p-5 md:p-6">
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0 space-y-1">
                    <p className="text-muted-foreground text-xs font-semibold tracking-widest uppercase">
                      {post.status === "PUBLISHED" ? "Opublikowano" : "Utworzono"}: {dateLabel}
                    </p>
                    <h2 className="text-foreground truncate text-lg font-bold">{post.title}</h2>
                    <p className="text-muted-foreground/70 font-mono text-xs">/{post.slug}</p>
                  </div>
                  <PostStatusBadge status={post.status} />
                </div>
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <p className="text-muted-foreground min-w-0 truncate text-xs font-semibold tracking-widest uppercase">
                    Autor: {post.authorDisplayName}
                  </p>
                  <div className="flex items-center gap-2">
                    <Button asChild variant="outline" size="sm">
                      <Link href={`/dashboard/posts/${post.id}`}>
                        <Pencil data-icon="inline-start" />
                        Edytuj
                      </Link>
                    </Button>
                    <Button
                      type="button"
                      variant="destructive"
                      size="sm"
                      onClick={() => setPostToDelete(post)}
                    >
                      <Trash2 data-icon="inline-start" />
                      Usuń
                    </Button>
                  </div>
                </div>
              </Card>
            );
          })}

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
              className="bg-destructive hover:bg-destructive/90 text-white"
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
    </SectionShell>
  );
}
