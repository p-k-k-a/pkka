"use client";

import Link from "next/link";
import { Users } from "lucide-react";
import { useListTopicGroups, type TopicGroupResponse } from "@pkka/api";
import { pluralPl, topicGroupMemberCountLabel } from "@pkka/domain";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { MembershipButton } from "@/components/topic-groups/membership-button";
import { CoordinatorBadge } from "@/components/topic-groups/coordinator-badge";

function GroupCard({ group }: { group: TopicGroupResponse }) {
  return (
    <Card className="bg-muted relative flex h-full flex-col gap-4 rounded-none p-6 shadow-none ring-0 transition-opacity has-[a:hover]:opacity-90">
      <div className="space-y-2">
        <h3 className="font-heading text-foreground text-lg leading-snug font-semibold">
          <Link
            href={`/dashboard/groups/${group.slug}`}
            className="focus-visible:ring-ring after:absolute after:inset-0 focus-visible:ring-2 focus-visible:outline-none"
          >
            {group.name}
          </Link>
        </h3>
        {group.description ? (
          <p className="text-muted-foreground line-clamp-3 text-sm leading-relaxed">
            {group.description}
          </p>
        ) : null}
      </div>
      <div className="mt-auto flex items-center justify-between gap-3 pt-2">
        <span className="text-muted-foreground inline-flex items-center gap-1.5 text-sm font-medium">
          <Users className="size-4" aria-hidden="true" />
          {topicGroupMemberCountLabel(group.memberCount)}
        </span>
        {group.coordinator ? <CoordinatorBadge label="Koordynujesz" /> : null}
        {group.member ? null : <MembershipButton group={group} className="relative z-10" />}
      </div>
    </Card>
  );
}

export function GroupList() {
  const { data: groupsResponse, isLoading, isError } = useListTopicGroups({ size: 100 });
  const groups = groupsResponse?.data.content;

  if (isLoading) {
    return (
      <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">
        <Skeleton className="h-48 rounded-none" />
        <Skeleton className="h-48 rounded-none" />
      </div>
    );
  }

  if (isError && !groups) {
    return (
      <p className="text-destructive font-medium">
        Nie udało się załadować grup. Odśwież stronę i spróbuj ponownie.
      </p>
    );
  }

  if (!groups?.length) {
    return (
      <p className="text-muted-foreground">Nie ma jeszcze żadnej grupy. Zaproponuj pierwszą.</p>
    );
  }

  return (
    <div className="space-y-12">
      <Section title="Twoje grupy" groups={groups.filter((group) => group.member)} />
      <Section title="Pozostałe grupy" groups={groups.filter((group) => !group.member)} />
    </div>
  );
}

function Section({ title, groups }: { title: string; groups: TopicGroupResponse[] }) {
  if (!groups.length) return null;
  return (
    <section className="space-y-6">
      <div className="flex items-baseline justify-between gap-4">
        <h2 className="font-heading text-foreground text-[23px] font-semibold">{title}</h2>
        <span className="text-muted-foreground text-sm">
          {groups.length} {pluralPl(groups.length, "grupa", "grupy", "grup")}
        </span>
      </div>
      <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3">
        {groups.map((group) => (
          <GroupCard key={group.id} group={group} />
        ))}
      </div>
    </section>
  );
}
