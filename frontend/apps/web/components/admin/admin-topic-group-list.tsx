"use client";

import { useState } from "react";
import Link from "next/link";
import { ArrowLeft, ArrowRight, ArrowUpRight, ChevronDown, Users } from "lucide-react";
import {
  TopicGroupDiscordStatus,
  useListAdminTopicGroupMembers,
  useListAdminTopicGroups,
  type AdminTopicGroupResponse,
} from "@pkka/api";
import { DiscordIcon } from "@pkka/icons/web";
import {
  formatEventDateLong,
  topicGroupMemberCountLabel,
  topicGroupMemberName,
} from "@pkka/domain";
import { Avatar } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

const PAGE_SIZE = 20;

const MISSING_CHANNEL_LABELS: Record<TopicGroupDiscordStatus, string> = {
  [TopicGroupDiscordStatus.LINKED]: "Brak linku do kanału na Discordzie",
  [TopicGroupDiscordStatus.MISSING]: "Kanał na Discordzie nie powstał",
  [TopicGroupDiscordStatus.NOT_CONFIGURED]: "Discord nie jest skonfigurowany",
};

function AdminTopicGroupMembers({ groupId }: { groupId: string }) {
  const { data, isLoading, isError } = useListAdminTopicGroupMembers(groupId);

  if (isLoading) return <Skeleton className="h-24 w-full" />;
  if (isError || data?.status !== 200) {
    return (
      <p className="text-destructive text-sm font-medium">Nie udało się załadować członków.</p>
    );
  }

  return (
    <ul className="border-border grid grid-cols-2 gap-x-2 gap-y-0.5 border-t pt-3 sm:grid-cols-3">
      {data.data.map((member) => {
        const name = topicGroupMemberName(member);
        return (
          <li key={member.id} className="min-w-0">
            <Link
              href={`/dashboard/admin/alumni/${member.id}`}
              className="hover:bg-muted focus-visible:ring-ring flex items-center gap-2 rounded-md px-1.5 py-1 focus-visible:ring-2 focus-visible:outline-none"
            >
              <Avatar fallback={name} alt="" size="sm" />
              <span className="text-foreground truncate text-sm font-medium">{name}</span>
              {member.coordinator ? (
                <span className="bg-primary/15 text-secondary-foreground dark:text-primary shrink-0 rounded-md px-2 py-0.5 text-xs font-semibold">
                  Koordynator
                </span>
              ) : null}
            </Link>
          </li>
        );
      })}
    </ul>
  );
}

function AdminTopicGroupItem({ group }: { group: AdminTopicGroupResponse }) {
  const [expanded, setExpanded] = useState(false);
  const coordinator = group.coordinator
    ? `${group.coordinator.firstName} ${group.coordinator.lastName}`
    : "Brak koordynatora";

  return (
    <Card className="gap-4 p-5 md:p-6">
      <div className="space-y-1">
        <h3 className="font-heading text-foreground text-xl leading-tight font-semibold">
          {group.name}
        </h3>
        <p className="text-muted-foreground text-sm">
          Koordynator: {coordinator} · Utworzona {formatEventDateLong(group.createdAt)}
        </p>
      </div>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <button
          type="button"
          disabled={group.memberCount === 0}
          aria-expanded={expanded}
          onClick={() => setExpanded(!expanded)}
          className="text-muted-foreground hover:text-foreground focus-visible:ring-ring -mx-2 flex items-center gap-2 rounded-md px-2 py-1 text-sm focus-visible:ring-2 focus-visible:outline-none disabled:pointer-events-none"
        >
          <Users className="size-4" />
          {topicGroupMemberCountLabel(group.memberCount)}
          {group.memberCount > 0 ? (
            <ChevronDown className={cn("size-4 transition-transform", expanded && "rotate-180")} />
          ) : null}
        </button>
        {group.discordChannelUrl ? (
          <Button asChild variant="outline" size="sm">
            <a href={group.discordChannelUrl} target="_blank" rel="noreferrer">
              <DiscordIcon data-icon="inline-start" className="size-4" />
              Otwórz kanał na Discordzie
              <ArrowUpRight data-icon="inline-end" />
            </a>
          </Button>
        ) : (
          <p className="text-muted-foreground text-sm">{MISSING_CHANNEL_LABELS[group.discord]}</p>
        )}
      </div>
      {expanded ? <AdminTopicGroupMembers groupId={group.id} /> : null}
    </Card>
  );
}

export function AdminTopicGroupList() {
  const [page, setPage] = useState(0);
  const { data: response, isLoading, isError } = useListAdminTopicGroups({ page, size: PAGE_SIZE });

  const pageData = response?.data;
  const groups = pageData?.content ?? [];
  const totalPages = pageData?.totalPages ?? 0;
  const currentPage = pageData?.number ?? page;

  if (isLoading) {
    return Array.from({ length: 3 }).map((_, i) => (
      <Skeleton key={i} className="h-32 w-full rounded-2xl" />
    ));
  }
  if (isError && !pageData) {
    return <p className="text-destructive font-medium">Nie udało się załadować grup.</p>;
  }
  if (groups.length === 0) {
    return <p className="text-muted-foreground">Nie ma jeszcze żadnej grupy.</p>;
  }

  return (
    <>
      {groups.map((group) => (
        <AdminTopicGroupItem key={group.id} group={group} />
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
