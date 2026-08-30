"use client";

import { AlertTriangle } from "lucide-react";
import { ApiError, useGetMyProfile } from "@pkka/api";
import { ProfileView } from "@/components/profile/profile-view";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/lib/auth-context";

function ProfileSkeleton() {
  return (
    <div className="flex flex-col">
      <section className="bg-navy">
        <div className="mx-auto flex max-w-[1280px] items-center gap-6 px-4 py-10 md:px-10 md:py-16">
          <Skeleton className="bg-white-text/10 size-24 rounded-full md:size-28" />
          <div className="flex flex-col gap-3">
            <Skeleton className="bg-white-text/10 h-4 w-28" />
            <Skeleton className="bg-white-text/10 h-8 w-64" />
            <Skeleton className="bg-white-text/10 h-4 w-40" />
          </div>
        </div>
      </section>
      <section className="bg-background">
        <div className="mx-auto grid max-w-[1280px] gap-6 px-4 py-10 md:px-10 md:py-12 lg:grid-cols-3">
          <div className="flex flex-col gap-6 lg:col-span-2">
            <Skeleton className="h-40 rounded-xl" />
            <Skeleton className="h-32 rounded-xl" />
          </div>
          <div className="flex flex-col gap-6">
            <Skeleton className="h-48 rounded-xl" />
            <Skeleton className="h-36 rounded-xl" />
          </div>
        </div>
      </section>
    </div>
  );
}

export function ProfileContent() {
  const { user } = useAuth();
  const { data, isPending, isError, error, isFetching, refetch } = useGetMyProfile();

  if (!user) return null;

  if (isPending) return <ProfileSkeleton />;

  if (isError || !data?.data) {
    const notFound = (error as unknown as ApiError | null)?.status === 404;

    return (
      <div className="mx-auto w-full max-w-[720px] px-4 py-12 md:px-10">
        <Alert variant="destructive">
          <AlertTriangle />
          <AlertTitle>Nie udało się wczytać profilu</AlertTitle>
          <AlertDescription>
            {notFound
              ? "Nie znaleziono profilu powiązanego z Twoim kontem."
              : "Sprawdź połączenie i spróbuj ponownie."}
          </AlertDescription>
        </Alert>
        <Button
          type="button"
          variant="outline"
          className="mt-4"
          disabled={isFetching}
          onClick={() => void refetch()}
        >
          {isFetching ? "Ładowanie…" : "Spróbuj ponownie"}
        </Button>
      </div>
    );
  }

  return <ProfileView profile={data.data} />;
}
