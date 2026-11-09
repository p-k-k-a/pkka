"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useListAlumniForAdmin, useListAlumniTagsForAdmin } from "@pkka/api";
import { isAdmin } from "@pkka/domain";
import { AlumniDirectoryView, useAlumniDirectoryState } from "@/components/alumni/alumni-directory";
import { ADMIN_ALUMNI_DIRECTORY_HREF } from "@/lib/alumni-search-params";
import { useAuth } from "@/lib/auth-context";

export function AdminAlumniDirectory() {
  const router = useRouter();
  const { isLoading, user } = useAuth();
  const admin = isAdmin(user?.roles);

  useEffect(() => {
    if (!isLoading && !admin) {
      router.replace("/dashboard");
    }
  }, [admin, isLoading, router]);

  const directory = useAlumniDirectoryState();
  const { data, isPending, isError, isPlaceholderData, refetch } = useListAlumniForAdmin(
    directory.params,
    { query: { enabled: admin, placeholderData: (previous) => previous } },
  );
  const tagsQuery = useListAlumniTagsForAdmin({ query: { enabled: admin } });
  return (
    <AlumniDirectoryView
      directory={directory}
      directoryHref={ADMIN_ALUMNI_DIRECTORY_HREF}
      tagsQuery={tagsQuery}
      pageData={data?.status === 200 ? data.data : undefined}
      isPending={isPending}
      isError={isError}
      isPlaceholderData={isPlaceholderData}
      refetch={refetch}
    />
  );
}
