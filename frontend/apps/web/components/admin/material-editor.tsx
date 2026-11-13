"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useGetAdminMaterial } from "@pkka/api";
import { Skeleton } from "@/components/ui/skeleton";
import { MaterialForm } from "@/components/admin/material-form";
import { useAuth } from "@/lib/auth-context";
import { isAdmin } from "@pkka/domain";

type MaterialEditorProps = {
  id?: string;
};

export function MaterialEditor({ id }: MaterialEditorProps) {
  const router = useRouter();
  const { isLoading, user } = useAuth();
  const admin = isAdmin(user?.roles);

  useEffect(() => {
    if (!isLoading && !admin) {
      router.replace("/dashboard");
    }
  }, [admin, isLoading, router]);

  const {
    data: response,
    isLoading: isMaterialLoading,
    isError,
  } = useGetAdminMaterial(id ?? "", { query: { enabled: admin && id !== undefined } });

  if (isLoading || !admin || (id !== undefined && isMaterialLoading)) {
    return (
      <div className="px-4 py-10 md:px-10">
        <div className="mx-auto max-w-[1280px] space-y-6">
          <Skeleton className="h-10 w-64 rounded-lg" />
          <Skeleton className="h-96 w-full rounded-2xl" />
        </div>
      </div>
    );
  }

  if (id === undefined) {
    return <MaterialForm />;
  }

  const material = response?.data;
  if (isError || !material) {
    return (
      <div className="px-4 py-16 text-center">
        <p className="text-destructive font-medium">Nie udało się załadować materiału.</p>
      </div>
    );
  }

  return <MaterialForm material={material} />;
}
