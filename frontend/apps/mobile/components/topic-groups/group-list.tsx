import { GroupTile, Pill } from "@/components/topic-groups/group-visuals";
import { GroupPopover, type CardRect } from "@/components/topic-groups/group-popover";
import { useToggleMembership } from "@/components/topic-groups/use-toggle-membership";
import { Button } from "@/components/ui/button";
import { Text } from "@/components/ui/text";
import { useThemeColors } from "@/lib/theme";
import { type TopicGroupResponse } from "@pkka/api";
import { pluralPl, topicGroupMemberCountLabel } from "@pkka/domain";
import { ChevronRight, Plus, Users } from "lucide-react-native";
import { useRef, useState } from "react";
import { Pressable, View } from "react-native";

function GroupCard({
  group,
  onOpen,
}: {
  group: TopicGroupResponse;
  onOpen: (rect: CardRect) => void;
}) {
  const theme = useThemeColors();
  const ref = useRef<View>(null);
  const { toggle, isPending } = useToggleMembership(group);

  return (
    <Pressable
      ref={ref}
      role="button"
      accessibilityLabel={`Szczegóły grupy ${group.name}`}
      onPress={() =>
        ref.current?.measureInWindow((x, y, width, height) => onOpen({ x, y, width, height }))
      }
      className="border-muted-foreground/25 dark:border-border bg-card gap-3 rounded-[20px] border p-3.5 active:opacity-80"
    >
      <View className="flex-row items-center gap-3">
        <GroupTile name={group.name} />
        <View className="flex-1 gap-0.5">
          <Text className="text-foreground text-base font-semibold">{group.name}</Text>
          {group.description ? (
            <Text numberOfLines={1} className="text-muted-foreground text-[13px]">
              {group.description}
            </Text>
          ) : null}
        </View>
        <ChevronRight size={20} color={theme.mutedForeground} />
      </View>
      <View className="flex-row items-center gap-2">
        <Users size={16} color={theme.mutedForeground} />
        <Text className="text-muted-foreground flex-1 text-[13px]">
          {topicGroupMemberCountLabel(group.memberCount)}
        </Text>
        {group.coordinator ? (
          <Pill label="Koordynujesz" amber />
        ) : group.member ? (
          <Pill label="Należysz" />
        ) : (
          <Button size="sm" onPress={toggle} disabled={isPending} className="h-9 gap-1.5 px-3.5">
            <Plus size={16} color={theme.primaryForeground} />
            <Text className="text-[13px] font-semibold">Dołącz</Text>
          </Button>
        )}
      </View>
    </Pressable>
  );
}

function Section({
  title,
  groups,
  onOpen,
}: {
  title: string;
  groups: TopicGroupResponse[];
  onOpen: (group: TopicGroupResponse, rect: CardRect) => void;
}) {
  if (!groups.length) return null;
  return (
    <View className="gap-3">
      <View className="flex-row items-baseline justify-between">
        <Text role="heading" className="font-heading text-foreground text-xl font-semibold">
          {title}
        </Text>
        <Text className="text-muted-foreground text-[13px]">
          {groups.length} {pluralPl(groups.length, "grupa", "grupy", "grup")}
        </Text>
      </View>
      {groups.map((group) => (
        <GroupCard key={group.id} group={group} onOpen={(rect) => onOpen(group, rect)} />
      ))}
    </View>
  );
}

type GroupListProps = {
  groups: TopicGroupResponse[] | undefined;
  isError: boolean;
};

export function GroupList({ groups, isError }: GroupListProps) {
  const [open, setOpen] = useState<{ slug: string; rect: CardRect } | null>(null);
  const openGroup = open && groups?.find((group) => group.slug === open.slug);

  if (isError && !groups) {
    return (
      <Text className="text-muted-foreground py-8 text-center text-sm">
        Nie udało się załadować grup.
      </Text>
    );
  }

  const openPopover = (group: TopicGroupResponse, rect: CardRect) =>
    setOpen({ slug: group.slug, rect });

  return (
    <>
      <Section
        title="Twoje grupy"
        groups={groups?.filter((group) => group.member) ?? []}
        onOpen={openPopover}
      />
      <Section
        title="Odkryj"
        groups={groups?.filter((group) => !group.member) ?? []}
        onOpen={openPopover}
      />
      {open && openGroup ? (
        <GroupPopover group={openGroup} rect={open.rect} onClose={() => setOpen(null)} />
      ) : null}
    </>
  );
}
