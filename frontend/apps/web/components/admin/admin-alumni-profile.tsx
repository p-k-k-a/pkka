"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useGetAlumniProfileForAdmin } from "@pkka/api";
import { isAdmin } from "@pkka/domain";
import { AlumniProfileState } from "@/components/alumni/alumni-profile-content";
import { ADMIN_ALUMNI_DIRECTORY_HREF } from "@/lib/alumni-search-params";
import { useAuth } from "@/lib/auth-context";

export function AdminAlumniProfile({ id }: { id: string }) {
  const router = useRouter();
  const { isLoading, user } = useAuth();
  const admin = isAdmin(user?.roles);

  useEffect(() => {
    if (!isLoading && !admin) {
      router.replace("/dashboard");
    }
  }, [admin, isLoading, router]);

  const { data, isPending, error, isFetching, refetch } = useGetAlumniProfileForAdmin(id, {
    query: { enabled: admin },
  });
  const profile = data?.status === 200 ? data.data : undefined;
  return (
    <AlumniProfileState
      directoryBase={ADMIN_ALUMNI_DIRECTORY_HREF}
      profile={
        profile && {
          ...profile,
          visibility: { name: true, email: true, discord: true, phone: true },
        }
      }
      isPending={isPending}
      isFetching={isFetching}
      error={error}
      refetch={refetch}
    />
  );
}
