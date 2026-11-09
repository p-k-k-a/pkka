"use client";

import { useState } from "react";
import { TopicGroupRequestStatus, useListMyTopicGroupRequests } from "@pkka/api";
import { Skeleton } from "@/components/ui/skeleton";
import { SectionShell } from "@/components/content/section-shell";
import { RequestCard } from "@/components/topic-groups/request-card";
import { RequestForm } from "@/components/topic-groups/request-form";

export function TopicGroupsContent() {
  const { data, isLoading, isError, refetch } = useListMyTopicGroupRequests();
  const [isProposing, setIsProposing] = useState(false);

  const latest = data?.data[0];
  const underReview = latest?.status === TopicGroupRequestStatus.UNDER_REVIEW;
  const showForm = !latest || (isProposing && !underReview);

  return (
    <SectionShell
      title="Grupy tematyczne"
      description="Miejsce dla alumnów o podobnych zainteresowaniach. Jeśli brakuje grupy dla Ciebie, zaproponuj własną. Administracja rozpatrzy wniosek, a po akceptacji zostaniesz właścicielem grupy."
      as="section"
    >
      <div className="max-w-2xl">
        {isLoading ? (
          <Skeleton className="h-72 w-full rounded-2xl" />
        ) : isError && !data ? (
          <p className="text-destructive font-medium">
            Nie udało się załadować Twojego wniosku. Odśwież stronę i spróbuj ponownie.
          </p>
        ) : (
          <div className="space-y-6">
            {latest ? (
              <RequestCard request={latest} onPropose={() => setIsProposing(true)} />
            ) : null}
            {showForm ? (
              <RequestForm
                onSubmitted={async () => {
                  await refetch();
                  setIsProposing(false);
                }}
                onConflict={() => void refetch()}
              />
            ) : null}
          </div>
        )}
      </div>
    </SectionShell>
  );
}
