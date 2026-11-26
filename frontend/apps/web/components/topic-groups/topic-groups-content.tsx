"use client";

import { useState } from "react";
import { TopicGroupRequestStatus, useListMyTopicGroupRequests } from "@pkka/api";
import { Skeleton } from "@/components/ui/skeleton";
import { SectionShell } from "@/components/content/section-shell";
import { RequestCard } from "@/components/topic-groups/request-card";
import { RequestForm } from "@/components/topic-groups/request-form";

export function TopicGroupsContent() {
  const { data: requestsResponse, isLoading, isError, refetch } = useListMyTopicGroupRequests();
  const [isProposing, setIsProposing] = useState(false);

  const latestRequest = requestsResponse?.data[0];
  const isUnderReview = latestRequest?.status === TopicGroupRequestStatus.UNDER_REVIEW;
  const showForm = !latestRequest || (isProposing && !isUnderReview);

  return (
    <SectionShell
      title="Grupy tematyczne"
      description="Miejsce dla alumnów o podobnych zainteresowaniach. Jeśli brakuje grupy dla Ciebie, zaproponuj własną. Administracja rozpatrzy wniosek, a po akceptacji zostaniesz koordynatorem grupy."
      as="section"
    >
      <div className="max-w-2xl">
        {isLoading ? (
          <Skeleton className="h-72 w-full rounded-2xl" />
        ) : isError && !requestsResponse ? (
          <p className="text-destructive font-medium">
            Nie udało się załadować Twojego wniosku. Odśwież stronę i spróbuj ponownie.
          </p>
        ) : (
          <div className="space-y-6">
            {latestRequest ? (
              <RequestCard request={latestRequest} onPropose={() => setIsProposing(true)} />
            ) : null}
            {showForm ? (
              <RequestForm
                onSubmitted={async () => {
                  await refetch();
                  setIsProposing(false);
                }}
                onConflict={async () => {
                  const { data: refreshedResponse } = await refetch();
                  if (refreshedResponse?.data[0]?.status === TopicGroupRequestStatus.UNDER_REVIEW) {
                    setIsProposing(false);
                  }
                }}
              />
            ) : null}
          </div>
        )}
      </div>
    </SectionShell>
  );
}
