"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { Plus } from "lucide-react";
import { TopicGroupRequestStatus, useListMyTopicGroupRequests } from "@pkka/api";
import { Button } from "@/components/ui/button";
import { SectionShell } from "@/components/content/section-shell";
import { GroupList } from "@/components/topic-groups/group-list";
import { RequestCard } from "@/components/topic-groups/request-card";

export const NEW_GROUP_HREF = "/dashboard/groups/new";

/** The caller's latest request, unless it was approved: then there is nothing left to follow. */
export function useOpenRequest() {
  const { data: requestsResponse, ...query } = useListMyTopicGroupRequests();
  const latestRequest = requestsResponse?.data[0];
  const openRequest =
    latestRequest?.status === TopicGroupRequestStatus.APPROVED ? undefined : latestRequest;
  return { openRequest, ...query };
}

export function TopicGroupsContent() {
  const router = useRouter();
  const { openRequest } = useOpenRequest();
  const isUnderReview = openRequest?.status === TopicGroupRequestStatus.UNDER_REVIEW;

  return (
    <SectionShell
      title="Grupy tematyczne"
      description="Miejsce dla alumnów o podobnych zainteresowaniach. Dołącz do grupy, a dostaniesz dostęp do jej kanału na Discordzie."
      as="section"
      action={
        isUnderReview ? null : (
          <Button asChild size="xl">
            <Link href={NEW_GROUP_HREF}>
              <Plus data-icon="inline-start" />
              Stwórz nową grupę
            </Link>
          </Button>
        )
      }
    >
      <div className="space-y-12">
        {openRequest ? (
          <div className="max-w-2xl">
            <RequestCard request={openRequest} onPropose={() => router.push(NEW_GROUP_HREF)} />
          </div>
        ) : null}
        <GroupList />
      </div>
    </SectionShell>
  );
}
