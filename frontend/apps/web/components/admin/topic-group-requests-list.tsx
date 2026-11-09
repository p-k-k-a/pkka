"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, ArrowRight } from "lucide-react";
import {
  getListAdminTopicGroupRequestsQueryKey,
  TopicGroupRequestStatus,
  useListAdminTopicGroupRequests,
} from "@pkka/api";
import { isAdmin } from "@pkka/domain";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { SectionShell } from "@/components/content/section-shell";
import { TopicGroupRequestItem } from "@/components/admin/topic-group-request-item";
import { useAuth } from "@/lib/auth-context";

const PAGE_SIZE = 20;

export function TopicGroupRequestsList() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { isLoading, user } = useAuth();
  const admin = isAdmin(user?.roles);
  const [page, setPage] = useState(0);

  useEffect(() => {
    if (!isLoading && !admin) {
      router.replace("/dashboard");
    }
  }, [admin, isLoading, router]);

  const {
    data: response,
    isLoading: isListLoading,
    isError,
  } = useListAdminTopicGroupRequests(
    { status: TopicGroupRequestStatus.UNDER_REVIEW, page, size: PAGE_SIZE },
    { query: { enabled: admin } },
  );

  if (isLoading || !admin) {
    return (
      <SectionShell title="Grupy tematyczne" as="section">
        <Skeleton className="h-40 w-full max-w-3xl rounded-2xl" />
      </SectionShell>
    );
  }

  const pageData = response?.data;
  const requests = pageData?.content ?? [];
  const totalElements = pageData?.totalElements ?? 0;
  const totalPages = pageData?.totalPages ?? 0;
  const currentPage = pageData?.number ?? page;

  const onReviewed = () => {
    if (requests.length === 1 && page > 0) setPage(page - 1);
    void queryClient.invalidateQueries({ queryKey: getListAdminTopicGroupRequestsQueryKey() });
  };

  return (
    <SectionShell
      title="Grupy tematyczne"
      description="Wnioski alumnów o nowe grupy. Po zatwierdzeniu powstaje grupa, a wnioskodawca zostaje jej właścicielem."
      as="section"
    >
      <div className="max-w-3xl space-y-4">
        <p className="text-accent text-xs font-semibold tracking-widest uppercase">
          Wnioski o nową grupę{totalElements > 0 ? ` · ${totalElements}` : ""}
        </p>

        {isListLoading ? (
          Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-36 w-full rounded-2xl" />
          ))
        ) : isError && !pageData ? (
          <p className="text-destructive font-medium">Nie udało się załadować wniosków.</p>
        ) : requests.length === 0 ? (
          <p className="text-muted-foreground">Brak wniosków oczekujących na decyzję.</p>
        ) : (
          <>
            {requests.map((request) => (
              <TopicGroupRequestItem key={request.id} request={request} onReviewed={onReviewed} />
            ))}

            {totalPages > 1 ? (
              <div className="flex items-center justify-between gap-4 pt-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={currentPage <= 0}
                  onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                >
                  <ArrowLeft data-icon="inline-start" />
                  Poprzednia
                </Button>
                <p className="text-muted-foreground text-sm">
                  Strona {currentPage + 1} z {totalPages}
                </p>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={currentPage >= totalPages - 1}
                  onClick={() => setPage((prev) => prev + 1)}
                >
                  Następna
                  <ArrowRight data-icon="inline-end" />
                </Button>
              </div>
            ) : null}
          </>
        )}
      </div>
    </SectionShell>
  );
}
