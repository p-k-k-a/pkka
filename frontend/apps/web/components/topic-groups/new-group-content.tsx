"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { TopicGroupRequestStatus } from "@pkka/api";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { RequestCard } from "@/components/topic-groups/request-card";
import { RequestForm } from "@/components/topic-groups/request-form";
import { useOpenRequest } from "@/components/topic-groups/topic-groups-content";

const GROUPS_HREF = "/dashboard/groups";

export function NewGroupContent() {
  const router = useRouter();
  const { openRequest, isLoading } = useOpenRequest();

  return (
    <div className="bg-background px-4 pt-6 pb-10 md:px-10 md:pt-8 md:pb-20">
      <div className="mx-auto max-w-[1280px] space-y-10">
        <Button asChild variant="ghost" size="sm" className="-ml-2">
          <Link href={GROUPS_HREF}>
            <ArrowLeft data-icon="inline-start" />
            Wróć do grup
          </Link>
        </Button>
        <div className="max-w-2xl space-y-10">
          <div className="space-y-3">
            <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
              Nowa grupa tematyczna
            </h1>
            <p className="text-muted-foreground leading-relaxed">
              Napisz, czym grupa będzie się zajmować i dla kogo jest. Administracja rozpatrzy
              wniosek, a po akceptacji zostaniesz koordynatorem grupy.
            </p>
          </div>
          {isLoading ? (
            <Skeleton className="h-72 w-full rounded-2xl" />
          ) : openRequest?.status === TopicGroupRequestStatus.UNDER_REVIEW ? (
            <RequestCard request={openRequest} />
          ) : (
            <RequestForm onSubmitted={() => router.push(GROUPS_HREF)} />
          )}
        </div>
      </div>
    </div>
  );
}
