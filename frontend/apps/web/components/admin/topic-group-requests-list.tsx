"use client";

import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, ArrowRight } from "lucide-react";
import {
  getListAdminTopicGroupRequestsQueryKey,
  TopicGroupRequestStatus,
  useListAdminTopicGroupRequests,
} from "@pkka/api";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { TopicGroupRequestItem } from "@/components/admin/topic-group-request-item";

const PAGE_SIZE = 20;

export function TopicGroupRequestsList() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);

  const {
    data: response,
    isLoading,
    isError,
  } = useListAdminTopicGroupRequests({
    status: TopicGroupRequestStatus.UNDER_REVIEW,
    page,
    size: PAGE_SIZE,
  });

  const pageData = response?.data;
  const requests = pageData?.content ?? [];
  const totalPages = pageData?.totalPages ?? 0;
  const currentPage = pageData?.number ?? page;

  const onReviewed = () => {
    if (requests.length === 1 && page > 0) setPage(page - 1);
    void queryClient.invalidateQueries({ queryKey: getListAdminTopicGroupRequestsQueryKey() });
  };

  if (isLoading) {
    return Array.from({ length: 3 }).map((_, i) => (
      <Skeleton key={i} className="h-36 w-full rounded-2xl" />
    ));
  }
  if (isError && !pageData) {
    return <p className="text-destructive font-medium">Nie udało się załadować wniosków.</p>;
  }
  if (requests.length === 0) {
    return <p className="text-muted-foreground">Brak wniosków oczekujących na decyzję.</p>;
  }

  return (
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
  );
}
