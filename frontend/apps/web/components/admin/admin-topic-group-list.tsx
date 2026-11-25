"use client";

import { useState } from "react";
import { ArrowLeft, ArrowRight, ArrowUpRight, MessageCircle, Users } from "lucide-react";
import {
  TopicGroupDiscordStatus,
  useListAdminTopicGroups,
  type AdminTopicGroupResponse,
} from "@pkka/api";
import { formatEventDateLong, topicGroupMemberCountLabel } from "@pkka/domain";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

const PAGE_SIZE = 20;

const MISSING_CHANNEL_LABELS: Record<TopicGroupDiscordStatus, string> = {
  [TopicGroupDiscordStatus.LINKED]: "Brak linku do kanału na Discordzie",
  [TopicGroupDiscordStatus.MISSING]: "Kanał na Discordzie nie powstał",
  [TopicGroupDiscordStatus.NOT_CONFIGURED]: "Discord nie jest skonfigurowany",
};

function AdminTopicGroupItem({ group }: { group: AdminTopicGroupResponse }) {
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
        <p className="text-muted-foreground flex items-center gap-2 text-sm">
          <Users className="size-4" />
          {topicGroupMemberCountLabel(group.memberCount)}
        </p>
        {group.discordChannelUrl ? (
          <Button asChild variant="outline" size="sm">
            <a href={group.discordChannelUrl} target="_blank" rel="noreferrer">
              <MessageCircle data-icon="inline-start" />
              Otwórz kanał na Discordzie
              <ArrowUpRight data-icon="inline-end" />
            </a>
          </Button>
        ) : (
          <p className="text-muted-foreground text-sm">{MISSING_CHANNEL_LABELS[group.discord]}</p>
        )}
      </div>
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
