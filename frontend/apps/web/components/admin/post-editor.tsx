"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useGetAdminPost } from "@pkka/api";
import { Skeleton } from "@/components/ui/skeleton";
import { PostForm } from "@/components/admin/post-form";
import { useAuth } from "@/lib/auth-context";
import { isAdmin } from "@/lib/roles";

type PostEditorProps = {
  id?: string;
};

export function PostEditor({ id }: PostEditorProps) {
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
    isLoading: isPostLoading,
    isError,
  } = useGetAdminPost(id ?? "", { query: { enabled: admin && id !== undefined } });

  if (isLoading || !admin || (id !== undefined && isPostLoading)) {
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
    return <PostForm />;
  }

  const post = response?.data;
  if (isError || !post) {
    return (
      <div className="px-4 py-16 text-center">
        <p className="text-destructive font-medium">Nie udało się załadować wpisu.</p>
      </div>
    );
  }

  return <PostForm post={post} />;
}
