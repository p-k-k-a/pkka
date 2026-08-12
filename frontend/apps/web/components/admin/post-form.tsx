"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, ImageIcon } from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import {
  getGetAdminPostQueryKey,
  getListAdminPostsQueryKey,
  useCreateAdminPost,
  useUpdateAdminPost,
  type AdminPostResponse,
} from "@pkka/api";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Textarea } from "@/components/ui/textarea";
import { MarkdownEditor } from "@/components/editor/markdown-editor";
import { PostStatusBadge } from "@/components/admin/post-status-badge";
import { ProseContent } from "@/components/content/prose-content";
import { formatPublishedAt } from "@/lib/format-published-at";

const EXCERPT_MAX = 500;

// TODO: publication channels other than the club website (mobile app push,
// Discord #ogłoszenia, newsletter) are not wired up yet — the checkboxes
// below are a mock of the target design.
const PUBLISH_CHANNELS = [
  { id: "www", label: "Strona WWW klubu", checked: true, disabled: true },
  { id: "mobile", label: "Aplikacja mobilna", checked: true, disabled: true },
  { id: "discord", label: "Discord #ogłoszenia", checked: false, disabled: true },
  { id: "newsletter", label: "Newsletter", checked: false, disabled: true },
];

// TODO: notifications are not implemented yet — mock of the target design.
const NOTIFICATIONS = [
  { id: "push-publish", label: "Push o publikacji", checked: true, disabled: true },
  { id: "email-followup", label: "Email follow-up", checked: false, disabled: true },
];

type PostFormProps = {
  post?: AdminPostResponse;
};

export function PostForm({ post }: PostFormProps) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const isEditing = post !== undefined;

  const [title, setTitle] = useState(post?.title ?? "");
  const [excerpt, setExcerpt] = useState(post?.excerpt ?? "");
  const [content, setContent] = useState(post?.content ?? "");
  const [published, setPublished] = useState(post?.status === "PUBLISHED");

  const onSaved = () => {
    queryClient.invalidateQueries({ queryKey: getListAdminPostsQueryKey() });
    if (post) {
      queryClient.invalidateQueries({ queryKey: getGetAdminPostQueryKey(post.id) });
    }
    router.push("/dashboard/posts");
  };

  const createPost = useCreateAdminPost({ mutation: { onSuccess: onSaved } });
  const updatePost = useUpdateAdminPost({ mutation: { onSuccess: onSaved } });

  const isPending = createPost.isPending || updatePost.isPending;
  const isError = createPost.isError || updatePost.isError;
  const status = published ? "PUBLISHED" : "DRAFT";
  const canSave = title.trim().length > 0 && content.trim().length > 0 && !isPending;

  const handleSave = () => {
    const data = { title: title.trim(), excerpt: excerpt.trim(), content, status } as const;
    if (isEditing) {
      updatePost.mutate({ id: post.id, data });
    } else {
      createPost.mutate({ data });
    }
  };

  const submitLabel = isPending
    ? "Zapisywanie…"
    : published
      ? isEditing
        ? "Zapisz i opublikuj"
        : "Opublikuj"
      : "Zapisz szkic";

  const previewDate = formatPublishedAt(post?.publishedAt ?? new Date().toISOString());

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
          <div className="space-y-2">
            <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
              {isEditing ? "Edytuj wpis" : "Nowy wpis"}
            </h1>
            <p className="text-muted-foreground text-sm">
              {published
                ? "Wpis będzie widoczny publicznie na stronie klubu."
                : "Szkic — widoczny tylko w panelu admina."}
            </p>
          </div>
          <Button type="button" size="lg" disabled={!canSave} onClick={handleSave}>
            {submitLabel}
          </Button>
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
                  {isEditing
                    ? `Adres wpisu: /${post.slug} (nie zmienia się przy edycji tytułu)`
                    : "Adres wpisu wygenerujemy automatycznie z tytułu."}
                </p>
              </div>

              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <Label
                    htmlFor="post-excerpt"
                    className="text-muted-foreground text-xs font-semibold tracking-widest uppercase"
                  >
                    Krótki opis
                  </Label>
                  <span className="text-muted-foreground text-xs tabular-nums">
                    {excerpt.length}/{EXCERPT_MAX}
                  </span>
                </div>
                <Textarea
                  id="post-excerpt"
                  value={excerpt}
                  maxLength={EXCERPT_MAX}
                  rows={3}
                  placeholder="Pojawi się na kartach w aplikacji i banerach"
                  onChange={(event) => setExcerpt(event.target.value)}
                />
              </div>
            </section>

            <section className="space-y-3">
              <h2 className="text-foreground font-heading text-lg font-semibold">Treść</h2>
              <Tabs defaultValue="editor">
                <TabsList>
                  <TabsTrigger value="editor">Edytor</TabsTrigger>
                  <TabsTrigger value="preview">Podgląd</TabsTrigger>
                </TabsList>
                <TabsContent value="editor">
                  <MarkdownEditor initialContent={post?.content ?? ""} onChange={setContent} />
                </TabsContent>
                <TabsContent value="preview">
                  <div className="border-input min-h-64 rounded-lg border px-4 py-3">
                    {content.trim() ? (
                      <ProseContent content={content} />
                    ) : (
                      <p className="text-muted-foreground text-sm">
                        Podgląd pojawi się po dodaniu treści.
                      </p>
                    )}
                  </div>
                </TabsContent>
              </Tabs>
              <p className="text-muted-foreground text-xs">
                Treść zapisujemy jako Markdown — tak samo wyświetli się w aplikacji mobilnej.
              </p>
            </section>

            <section className="space-y-3">
              <h2 className="text-foreground font-heading text-lg font-semibold">Okładka</h2>
              {/* TODO: cover image upload is not supported by the API yet — mock of the target design. */}
              <div className="border-border text-muted-foreground flex min-h-40 cursor-not-allowed items-center justify-center rounded-lg border-2 border-dashed">
                <div className="flex items-center gap-2 text-xs font-semibold tracking-widest uppercase">
                  <ImageIcon className="size-4" aria-hidden="true" />
                  Okładka 1200×630 — wkrótce
                </div>
              </div>
            </section>
          </Card>

          <div className="space-y-6">
            <Card className="gap-4 p-5">
              <h2 className="text-foreground font-heading text-base font-semibold">Status</h2>
              <div className="flex items-center justify-between gap-3">
                <Label htmlFor="post-published" className="text-sm font-medium">
                  Opublikowany
                </Label>
                <Switch id="post-published" checked={published} onCheckedChange={setPublished} />
              </div>
              <PostStatusBadge status={status} />
              {post?.publishedAt ? (
                <p className="text-muted-foreground text-xs">
                  Pierwsza publikacja: {formatPublishedAt(post.publishedAt).dateLabel}
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
              <p className="text-muted-foreground text-xs">Pozostałe kanały wkrótce.</p>
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

            <Card className="gap-3 p-5">
              <h2 className="text-muted-foreground text-xs font-semibold tracking-widest uppercase">
                Podgląd karty
              </h2>
              <div className="border-border rounded-lg border p-4">
                <div className="bg-muted text-muted-foreground mb-3 flex h-24 items-center justify-center rounded-md text-xs font-semibold tracking-widest uppercase">
                  Okładka
                </div>
                <p className="text-muted-foreground text-xs">
                  {previewDate.dateLabel} · {published ? "Opublikowany" : "Szkic"}
                </p>
                <p className="text-foreground truncate text-sm font-bold">
                  {title.trim() || "Tytuł wpisu"}
                </p>
                <p className="text-muted-foreground line-clamp-2 text-xs">
                  {excerpt.trim() || "Krótki opis wpisu pojawi się tutaj."}
                </p>
              </div>
            </Card>
          </div>
        </div>
      </div>
    </div>
  );
}
