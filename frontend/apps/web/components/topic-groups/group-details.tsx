"use client";

import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import {
  useGetTopicGroup,
  useListTopicGroupMembers,
  type TopicGroupMemberResponse,
} from "@pkka/api";
import { isAdmin, topicGroupMemberName } from "@pkka/domain";
import { Avatar } from "@/components/ui/avatar";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { DiscordBanner } from "@/components/topic-groups/discord-banner";
import { MembershipButton } from "@/components/topic-groups/membership-button";
import { useAuth } from "@/lib/auth-context";
import { CoordinatorBadge } from "@/components/topic-groups/coordinator-badge";

function MemberItem({ member }: { member: TopicGroupMemberResponse }) {
  const name = topicGroupMemberName(member);
  return (
    <li>
      <Link
        href={`/dashboard/alumni/${member.id}`}
        className="hover:bg-muted focus-visible:ring-ring flex items-center gap-3 p-3 focus-visible:ring-2 focus-visible:outline-none"
      >
        <Avatar fallback={name} alt="" size="md" />
        <span className="text-foreground min-w-0 truncate font-semibold">{name}</span>
        {member.coordinator ? <CoordinatorBadge label="Koordynator" /> : null}
      </Link>
    </li>
  );
}

function Members({ groupId, count }: { groupId: string; count: number }) {
  const { data: membersResponse, isLoading, isError } = useListTopicGroupMembers(groupId);

  return (
    <section className="space-y-4">
      <h2 className="font-heading text-foreground flex items-baseline gap-2 text-[23px] font-semibold">
        Członkowie
        <span className="text-muted-foreground text-base font-medium">{count}</span>
      </h2>
      {isLoading ? (
        <Skeleton className="h-32 w-full" />
      ) : isError || membersResponse?.status !== 200 ? (
        <p className="text-destructive font-medium">Nie udało się załadować członków.</p>
      ) : (
        <ul className="grid gap-1 sm:grid-cols-2">
          {membersResponse.data.map((member) => (
            <MemberItem key={member.id} member={member} />
          ))}
        </ul>
      )}
    </section>
  );
}

export function TopicGroupDetails({ slug }: { slug: string }) {
  const { user } = useAuth();
  const { data: groupResponse, isLoading, isError } = useGetTopicGroup(slug);
  const group = groupResponse?.status === 200 ? groupResponse.data : undefined;

  return (
    <div className="bg-background px-4 pt-6 pb-10 md:px-10 md:pt-8 md:pb-20">
      <div className="mx-auto max-w-[1280px] space-y-10">
        <Button asChild variant="ghost" size="sm" className="-ml-2">
          <Link href="/dashboard/groups">
            <ArrowLeft data-icon="inline-start" />
            Wróć do grup
          </Link>
        </Button>
        {isLoading ? (
          <Skeleton className="h-60 w-full max-w-3xl" />
        ) : isError || !group ? (
          <p className="text-destructive font-medium">
            Nie udało się załadować grupy. Mogła zostać usunięta.
          </p>
        ) : (
          <div className="max-w-3xl space-y-10">
            <div className="space-y-4">
              <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
                {group.name}
              </h1>
              {group.coordinator ? (
                <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm">
                  <CoordinatorBadge label="Koordynujesz" />
                  <span className="text-muted-foreground">
                    Żeby odejść, najpierw przekaż grupę innemu członkowi.
                  </span>
                </div>
              ) : null}
              {group.description ? (
                <p className="text-muted-foreground leading-relaxed whitespace-pre-line">
                  {group.description}
                </p>
              ) : null}
            </div>

            {group.coordinator ? null : <MembershipButton group={group} prominent />}

            {group.member ? <DiscordBanner /> : null}

            {group.member || isAdmin(user?.roles) ? (
              <Members groupId={group.id} count={group.memberCount} />
            ) : (
              <p className="text-muted-foreground">Dołącz do grupy, żeby zobaczyć jej członków.</p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
