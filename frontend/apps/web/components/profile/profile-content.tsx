"use client";

import { ApiError, useGetMyProfile } from "@pkka/api";
import { ProfileView } from "@/components/profile/profile-view";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/lib/auth-context";
import { loadProfileMock } from "@/lib/profile-mock";

function ProfilePageFrame({ children }: { children: React.ReactNode }) {
  return (
    <section className="bg-background px-4 py-10 md:px-10 md:py-20">
      <div className="mx-auto max-w-[1280px]">{children}</div>
    </section>
  );
}

export function ProfileContent() {
  const { user } = useAuth();
  const { data, isPending, isError, error, refetch } = useGetMyProfile();

  if (!user) return null;

  if (isPending) {
    return (
      <ProfilePageFrame>
        <div className="mx-auto max-w-2xl space-y-6">
          <Skeleton className="h-10 w-40" />
          <Skeleton className="aspect-[4/3] w-full rounded-xl" />
          <Skeleton className="h-8 w-56" />
          <Skeleton className="h-24 w-full rounded-xl" />
        </div>
      </ProfilePageFrame>
    );
  }

  const apiError = error as unknown as ApiError | null;
  if (isError || !data?.data) {
    const notFound = apiError?.status === 404;
    return (
      <ProfilePageFrame>
        <div className="mx-auto max-w-2xl space-y-4">
          <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
            Profil
          </h1>
          <p className="text-destructive font-medium">
            {notFound
              ? "Nie znaleziono profilu powiązanego z Twoim kontem."
              : "Nie udało się wczytać profilu. Odśwież stronę i spróbuj ponownie."}
          </p>
          <button
            type="button"
            className="text-accent text-sm font-semibold underline"
            onClick={() => void refetch()}
          >
            Spróbuj ponownie
          </button>
        </div>
      </ProfilePageFrame>
    );
  }

  return (
    <ProfilePageFrame>
      <ProfileView me={user} profile={data.data} mock={loadProfileMock(user.sub)} />
    </ProfilePageFrame>
  );
}
