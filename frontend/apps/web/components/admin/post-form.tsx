"use client";

import { CoverUploadField, type CoverValue } from "@/components/admin/cover-upload-field";
import { FieldLabel } from "@/components/admin/field-label";
import { SectionTitle } from "@/components/content/section-title";
import { MarkdownEditor } from "@/components/editor/markdown-editor";
import { isStorageUnavailable, postContentRejectionMessage } from "@/lib/save-errors";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { IMAGE_UPLOAD, formatPublishedAt } from "@pkka/domain";
import {
  AdminUploadPurpose,
  getGetAdminPostQueryKey,
  getGetPostQueryKey,
  getListAdminPostsQueryKey,
  getListPostsQueryKey,
  presignAdminUpload,
  useCreateAdminPost,
  useFileUpload,
  useUpdateAdminPost,
  PostStatus,
  type AdminPostResponse,
  type PresignUpload,
} from "@pkka/api";
import { useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, CircleAlert } from "lucide-react";
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

const presignCover: PresignUpload = (file) =>
  presignAdminUpload({ purpose: AdminUploadPurpose.POST_COVER, ...file });
const presignContentImage: PresignUpload = (file) =>
  presignAdminUpload({ purpose: AdminUploadPurpose.POST_IMAGE, ...file });

type PostFormProps = {
  post?: AdminPostResponse;
};

export function PostForm({ post }: PostFormProps) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const isEditing = post !== undefined;
  const isPublished = post?.status === PostStatus.PUBLISHED;

  const [title, setTitle] = useState(post?.title ?? "");
  const [content, setContent] = useState(post?.content ?? "");
  // The API returns the stored cover as a signed URL and takes it back unchanged on save.
  const [cover, setCover] = useState<CoverValue>({
    reference: post?.coverImageUrl,
    previewUrl: post?.coverImageUrl,
  });
  const coverUpload = useFileUpload({ rule: IMAGE_UPLOAD, presign: presignCover });
  const contentImageUpload = useFileUpload({
    rule: IMAGE_UPLOAD,
    presign: presignContentImage,
  });
  const [postId, setPostId] = useState(post?.id);
  const [pendingStatus, setPendingStatus] = useState<PostStatus | null>(null);

  const createPost = useCreateAdminPost();
  const updatePost = useUpdateAdminPost();

  const isPending = pendingStatus !== null;
  const isError = createPost.isError || updatePost.isError;
  const saveError = createPost.error ?? updatePost.error;
  // A picture pasted in from another editor is only a preview in this browser: the file never
  // reached the storage, so the post would be saved with a dead image.
  const hasUnsentPicture = content.includes("](blob:");
  const canSave =
    title.trim().length > 0 &&
    content.trim().length > 0 &&
    !hasUnsentPicture &&
    !coverUpload.isUploading &&
    !contentImageUpload.isUploading &&
    !isPending;

  const handleSaveSuccess = (savedId: string, status: PostStatus) => {
    queryClient.invalidateQueries({ queryKey: getListAdminPostsQueryKey() });
    queryClient.invalidateQueries({ queryKey: getGetAdminPostQueryKey(savedId) });
    queryClient.invalidateQueries({ queryKey: getListPostsQueryKey() });
    if (post) queryClient.invalidateQueries({ queryKey: getGetPostQueryKey(post.slug) });
    setPendingStatus(null);
    if (status === PostStatus.PUBLISHED) {
      toast.success("Wpis opublikowany");
      router.push("/dashboard/posts");
    } else {
      setPostId(savedId);
      toast.success("Zapisano szkic");
    }
  };

  const save = (status: PostStatus) => {
    const data = { title: title.trim(), content, status, coverImageUrl: cover.reference } as const;
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
    <div className="bg-background px-4 py-10 md:px-10 md:py-16">
      <div className="mx-auto max-w-[1280px]">
        <Button asChild variant="ghost" size="sm" className="mb-8 -ml-2">
          <Link href="/dashboard/posts">
            <ArrowLeft data-icon="inline-start" />
            Wróć do listy wpisów
          </Link>
        </Button>

        <div className="mb-10 space-y-3">
          <p className="bg-accent/10 text-accent inline-flex rounded-lg px-3 py-1 text-xs font-semibold tracking-widest uppercase">
            {isEditing ? "Edycja" : "Nowy wpis"}
          </p>
          <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
            {isEditing ? "Edytuj wpis" : "Napisz wpis do aktualności"}
          </h1>
        </div>

        {isError ? (
          <p className="text-destructive mb-6 font-medium">
            {isStorageUnavailable(saveError)
              ? "Przechowywanie plików jest chwilowo niedostępne, więc nie da się zapisać wpisu z wgraną okładką. Spróbuj później albo usuń okładkę i zapisz bez niej."
              : (postContentRejectionMessage(saveError) ??
                "Nie udało się zapisać wpisu. Spróbuj ponownie.")}
          </p>
        ) : null}

        {hasUnsentPicture ? (
          <p className="text-destructive mb-6 font-medium">
            Jedno ze zdjęć w treści nie zostało przesłane. Usuń je i wstaw ponownie przyciskiem
            „Wstaw zdjęcie”.
          </p>
        ) : null}

        <div className="grid grid-cols-1 items-start gap-16 lg:grid-cols-[minmax(0,1fr)_320px]">
          <div className="space-y-12">
            <section className="space-y-5">
              <FieldLabel htmlFor="post-title">Tytuł</FieldLabel>
              <Input
                id="post-title"
                value={title}
                maxLength={300}
                placeholder="np. Relacja z debaty: AI w przemyśle"
                className="font-heading h-auto min-h-14 py-3 text-lg font-semibold md:text-[23px]"
                onChange={(event) => setTitle(event.target.value)}
              />
              <p className="text-muted-foreground text-xs">
                {!isEditing
                  ? "Adres wpisu wygenerujemy automatycznie z tytułu."
                  : `Adres wpisu: /${post.slug}. Nie zmienia się przy edycji.`}
              </p>
            </section>

            <section className="space-y-5">
              <SectionTitle>Treść</SectionTitle>
              <MarkdownEditor
                initialContent={post?.content ?? ""}
                onChange={setContent}
                imageUpload={{ upload: contentImageUpload, rule: IMAGE_UPLOAD }}
              />
            </section>

            <section className="space-y-5">
              <SectionTitle>Okładka</SectionTitle>
              <CoverUploadField
                value={cover}
                onChange={setCover}
                upload={coverUpload}
                rule={IMAGE_UPLOAD}
              />
            </section>
          </div>

          <div className="space-y-10">
            <Card className="gap-5 rounded-lg p-5 shadow-none">
              <div className="flex items-center gap-1.5">
                <SectionTitle>Status</SectionTitle>
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
                  <Button
                    type="button"
                    disabled={!canSave}
                    onClick={() => save(PostStatus.PUBLISHED)}
                  >
                    {pendingStatus === PostStatus.PUBLISHED ? "Zapisywanie…" : "Zapisz zmiany"}
                  </Button>
                ) : (
                  <>
                    <Button
                      type="button"
                      variant="outline"
                      disabled={!canSave}
                      onClick={() => save(PostStatus.DRAFT)}
                    >
                      {pendingStatus === PostStatus.DRAFT ? "Zapisywanie…" : "Zapisz szkic"}
                    </Button>
                    <Button
                      type="button"
                      disabled={!canSave}
                      onClick={() => save(PostStatus.PUBLISHED)}
                    >
                      {pendingStatus === PostStatus.PUBLISHED ? "Publikowanie…" : "Opublikuj"}
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

            <Card className="gap-5 rounded-lg p-5 shadow-none">
              <SectionTitle>Gdzie opublikować</SectionTitle>
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

            <Card className="gap-5 rounded-lg p-5 shadow-none">
              <SectionTitle>Powiadomienia</SectionTitle>
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
