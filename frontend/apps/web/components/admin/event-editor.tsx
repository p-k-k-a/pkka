"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useGetAdminEvent } from "@pkka/api";
import { Skeleton } from "@/components/ui/skeleton";
import { EventForm } from "@/components/admin/event-form";
import { useAuth } from "@/lib/auth-context";
import { isAdmin } from "@/lib/roles";

type EventEditorProps = {
  id?: string;
};

export function EventEditor({ id }: EventEditorProps) {
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
    isLoading: isEventLoading,
    isError,
  } = useGetAdminEvent(id ?? "", { query: { enabled: admin && id !== undefined } });

  if (isLoading || !admin || (id !== undefined && isEventLoading)) {
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
    return <EventForm />;
  }

  const event = response?.data;
  if (isError || !event) {
    return (
      <div className="px-4 py-16 text-center">
        <p className="text-destructive font-medium">Nie udało się załadować wydarzenia.</p>
      </div>
    );
  }

  return <EventForm event={event} />;
}
