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
    <div className="px-4 py-10 md:px-10 md:py-20">
      <div className="mx-auto grid max-w-[1280px] grid-cols-1 gap-16 lg:grid-cols-[minmax(0,1fr)_320px]">
        <div className="space-y-6">
          <Skeleton className="h-6 w-32 rounded-lg" />
          <div className="flex items-center gap-6">
            <Skeleton className="size-24 rounded-full" />
            <Skeleton className="h-12 w-full max-w-md rounded-lg" />
          </div>
          <Skeleton className="h-12 w-40 rounded-lg" />
          <Skeleton className="h-24 w-full rounded-lg" />
        </div>
        <Skeleton className="h-64 w-full rounded-lg" />
      </div>
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

  return <ProfileView variant="own" profile={data.data} />;
}
