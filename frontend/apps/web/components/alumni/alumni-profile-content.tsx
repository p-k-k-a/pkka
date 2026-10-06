"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { AlertTriangle } from "lucide-react";
import { ApiError, useGetAlumniProfile } from "@pkka/api";
import { ProfileView } from "@/components/profile/profile-view";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ALUMNI_DIRECTORY_HREF } from "@/lib/alumni-search-params";

function AlumniProfileSkeleton() {
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

export function AlumniProfileContent({ id }: { id: string }) {
  const { data, isPending, error, isFetching, refetch } = useGetAlumniProfile(id);
  // The card passes the directory's criteria along, so going back restores them.
  const searchParams = useSearchParams().toString();
  const directoryHref = `${ALUMNI_DIRECTORY_HREF}${searchParams ? `?${searchParams}` : ""}`;

  if (isPending) return <AlumniProfileSkeleton />;

  const profile = data?.status === 200 ? data.data : undefined;
  if (!profile) {
    // Every non-2xx throws out of the mutator, so a missing alumn arrives as an ApiError.
    const notFound = error instanceof ApiError && error.status === 404;

    return (
      <div className="mx-auto w-full max-w-[720px] px-4 py-12 md:px-10">
        <Alert variant="destructive">
          <AlertTriangle />
          <AlertTitle>
            {notFound ? "Nie znaleziono profilu alumna" : "Nie udało się wczytać profilu"}
          </AlertTitle>
          <AlertDescription>
            {notFound
              ? "Ten profil nie istnieje albo nie jest już dostępny."
              : "Sprawdź połączenie i spróbuj ponownie."}
          </AlertDescription>
        </Alert>
        <div className="mt-4 flex gap-3">
          {notFound ? null : (
            <Button
              type="button"
              variant="outline"
              disabled={isFetching}
              onClick={() => void refetch()}
            >
              {isFetching ? "Ładowanie…" : "Spróbuj ponownie"}
            </Button>
          )}
          <Button asChild variant="ghost">
            <Link href={directoryHref}>Wróć do katalogu</Link>
          </Button>
        </div>
      </div>
    );
  }

  return <ProfileView variant="alumn" profile={profile} directoryHref={directoryHref} />;
}
