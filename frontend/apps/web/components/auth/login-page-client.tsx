"use client";

import { useAuth } from "@/lib/auth-context";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { LoginForm } from "@/components/auth/login-form";
import { Skeleton } from "@/components/ui/skeleton";

export function LoginPageClient() {
  const { user, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && user) {
      router.replace("/dashboard");
    }
  }, [isLoading, user, router]);

  if (isLoading) {
    return (
      <div className="mx-auto max-w-lg px-6 py-12">
        <Skeleton className="h-96 w-full rounded-[24px]" />
      </div>
    );
  }

  if (user) {
    return null;
  }

  return <LoginForm />;
}
