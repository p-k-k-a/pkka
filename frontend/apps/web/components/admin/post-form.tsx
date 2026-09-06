"use client";

import { MarkdownEditor } from "@/components/editor/markdown-editor";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { formatPublishedAt } from "@/lib/format-published-at";
import {
  getGetAdminPostQueryKey,
  getListAdminPostsQueryKey,
  useCreateAdminPost,
  useUpdateAdminPost,
  type AdminPostResponse,
  type CreatePostRequestStatus,
} from "@pkka/api";
import { useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, CircleAlert, ImageIcon } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";

// TODO: publication channels other than the club platform (Discord #ogłoszenia,
// newsletter) are not wired up yet
const PUBLISH_CHANNELS = [
  { id: "platform", label: "Strona i aplikacja klubu", checked: true, disabled: true },
  { id: "discord", label: "Discord #ogłoszenia", checked: false, disabled: true },
  { id: "newsletter", label: "Newsletter", checked: false, disabled: true },
];

// TODO: notifications are not implemented yet
const NOTIFICATIONS = [
  { id: "push-publish", label: "Powiadomienie Push", checked: true, disabled: true },
  { id: "email", label: "Email", checked: false, disabled: true },
];

type PostFormProps = {
  post?: AdminPostResponse;
};

export function PostForm({ post }: PostFormProps) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const isEditing = post !== undefined;
  const isPublished = post?.status === "PUBLISHED";

  const [title, setTitle] = useState(post?.title ?? "");
  const [content, setContent] = useState(post?.content ?? "");
  const [postId, setPostId] = useState(post?.id);
  const [pendingStatus, setPendingStatus] = useState<CreatePostRequestStatus | null>(null);

  const createPost = useCreateAdminPost();
  const updatePost = useUpdateAdminPost();

  const isPending = pendingStatus !== null;
  const isError = createPost.isError || updatePost.isError;
  const canSave = title.trim().length > 0 && content.trim().length > 0 && !isPending;

  const handleSaveSuccess = (savedId: string, status: CreatePostRequestStatus) => {
    queryClient.invalidateQueries({ queryKey: getListAdminPostsQueryKey() });
    queryClient.invalidateQueries({ queryKey: getGetAdminPostQueryKey(savedId) });
    setPendingStatus(null);
    if (status === "PUBLISHED") {
      toast.success("Wpis opublikowany");
      router.push("/dashboard/posts");
    } else {
      setPostId(savedId);
      toast.success("Zapisano szkic");
    }
  };

  const save = (status: CreatePostRequestStatus) => {
    const data = { title: title.trim(), content, status } as const;
    setPendingStatus(status);

    if (postId !== undefined) {
      updatePost.mutate(
        { id: postId, data },
        {
          onSuccess: () => handleSaveSuccess(postId, status),
          onError: () => setPendingStatus(null),
        },
      );
    } else {
      createPost.mutate(
        { data },
        {
          onSuccess: (response) => handleSaveSuccess(response.data.id, status),
          onError: () => setPendingStatus(null),
        },
      );
    }
  };

  return (
    <div className="px-4 py-10 md:px-10">
      <div className="mx-auto max-w-[1280px]">
        <Button asChild variant="ghost" size="sm" className="mb-6 -ml-2">
          <Link href="/dashboard/posts">
            <ArrowLeft data-icon="inline-start" />
            Wróć do listy wpisów
          </Link>
        </Button>

        <div className="mb-8 flex flex-wrap items-start justify-between gap-4">
          <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
            {isEditing ? "Edytuj wpis" : "Nowy wpis"}
          </h1>
        </div>

        {isError ? (
          <p className="text-destructive mb-6 font-medium">
            Nie udało się zapisać wpisu. Spróbuj ponownie.
          </p>
        ) : null}

        <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
          <Card className="gap-8 p-5 md:p-8">
            <section className="space-y-5">
              <h2 className="text-foreground font-heading text-lg font-semibold">
                Podstawowe informacje
              </h2>

              <div className="space-y-2">
                <Label
                  htmlFor="post-title"
                  className="text-muted-foreground text-xs font-semibold tracking-widest uppercase"
                >
                  Tytuł wpisu
                </Label>
                <Input
                  id="post-title"
                  value={title}
                  maxLength={300}
                  placeholder="np. Relacja z debaty: AI w przemyśle"
                  onChange={(event) => setTitle(event.target.value)}
                />
                <p className="text-muted-foreground text-xs">
                  {!isEditing
                    ? "Adres wpisu wygenerujemy automatycznie z tytułu."
                    : `Adres wpisu: /${post.slug}. Nie zmienia się przy edycji.`}
                </p>
              </div>
            </section>

            <section className="space-y-3">
              <h2 className="text-foreground font-heading text-lg font-semibold">Treść</h2>
              <MarkdownEditor initialContent={post?.content ?? ""} onChange={setContent} />
            </section>

            <section className="space-y-3">
              <h2 className="text-foreground font-heading text-lg font-semibold">Okładka</h2>
              {/* TODO: cover image upload is not supported by the API yet */}
              <div className="bg-background border-border text-muted-foreground flex min-h-40 cursor-not-allowed items-center justify-center rounded-lg border-2 border-dashed">
                <div className="flex items-center gap-2 text-xs font-semibold tracking-widest uppercase">
                  <ImageIcon className="size-4" aria-hidden="true" />
                </div>
              </div>
            </section>
          </Card>

          <div className="space-y-6">
            <Card className="gap-4 p-5">
              <div className="flex items-center gap-1.5">
                <h2 className="text-foreground font-heading text-base font-semibold">Status</h2>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <button
                      type="button"
                      aria-label="Czym różni się szkic od opublikowanego wpisu?"
                      className="text-muted-foreground hover:text-foreground cursor-pointer transition-colors"
                    >
                      <CircleAlert className="size-4" aria-hidden="true" />
                    </button>
                  </TooltipTrigger>
                  <TooltipContent>
                    Szkic widzą tylko administratorzy. Możesz go zapisywać wiele razy. Publikacja
                    jest nieodwracalna i widoczna dla wszystkich.
                  </TooltipContent>
                </Tooltip>
              </div>
              <div className="flex flex-col gap-2">
                {isPublished ? (
                  <Button type="button" disabled={!canSave} onClick={() => save("PUBLISHED")}>
                    {pendingStatus === "PUBLISHED" ? "Zapisywanie…" : "Zapisz zmiany"}
                  </Button>
                ) : (
                  <>
                    <Button
                      type="button"
                      variant="outline"
                      disabled={!canSave}
                      onClick={() => save("DRAFT")}
                    >
                      {pendingStatus === "DRAFT" ? "Zapisywanie…" : "Zapisz szkic"}
                    </Button>
                    <Button type="button" disabled={!canSave} onClick={() => save("PUBLISHED")}>
                      {pendingStatus === "PUBLISHED" ? "Publikowanie…" : "Opublikuj"}
                    </Button>
                  </>
                )}
              </div>
              {isPublished ? (
                <p className="text-muted-foreground text-xs">
                  Opublikowano: {formatPublishedAt(post.publishedAt).dateLabel}
                </p>
              ) : null}
            </Card>

            <Card className="gap-4 p-5">
              <h2 className="text-foreground font-heading text-base font-semibold">
                Gdzie opublikować
              </h2>
              <div className="space-y-3">
                {PUBLISH_CHANNELS.map((channel) => (
                  <div key={channel.id} className="flex items-center gap-3">
                    <Checkbox
                      id={`channel-${channel.id}`}
                      checked={channel.checked}
                      disabled={channel.disabled}
                    />
                    <Label
                      htmlFor={`channel-${channel.id}`}
                      className="text-muted-foreground text-sm"
                    >
                      {channel.label}
                    </Label>
                  </div>
                ))}
              </div>
            </Card>

            <Card className="gap-4 p-5">
              <h2 className="text-foreground font-heading text-base font-semibold">
                Powiadomienia
              </h2>
              <div className="space-y-3">
                {NOTIFICATIONS.map((notification) => (
                  <div key={notification.id} className="flex items-center gap-3">
                    <Checkbox
                      id={`notification-${notification.id}`}
                      checked={notification.checked}
                      disabled={notification.disabled}
                    />
                    <Label
                      htmlFor={`notification-${notification.id}`}
                      className="text-muted-foreground text-sm"
                    >
                      {notification.label}
                    </Label>
                  </div>
                ))}
              </div>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
