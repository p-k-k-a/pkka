import { AlumniProfileView } from "@/components/alumni/alumni-profile-view";
import { DiscordIcon } from "@/components/ui/svg-icons";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { Text } from "@/components/ui/text";
import { useAuth } from "@/lib/auth-context";
import { useProfile } from "@/lib/profile-context";
import { ApplicationResponseDtoStatus, useGetMine } from "@pkka/api";
import { useTheme } from "@react-navigation/native";
import { router } from "expo-router";
import { ClipboardList, LogOut } from "lucide-react-native";
import * as React from "react";
import { ActivityIndicator, RefreshControl, ScrollView, View } from "react-native";

type StatusConfig = {
  dotClass: string;
  badgeClass: string;
  textClass: string;
  label: string;
  heading: string;
  description: string;
};

const STATUS_CONFIG: Record<"UNDER_REVIEW" | "APPROVED" | "REJECTED", StatusConfig> = {
  UNDER_REVIEW: {
    dotClass: "bg-yellow-500",
    badgeClass: "border-yellow-500",
    textClass: "text-yellow-600",
    label: "Wniosek w trakcie rozpatrywania",
    heading: "Wniosek złożony",
    description:
      "Twój wniosek o członkostwo jest rozpatrywany. Powiadomimy Cię, gdy zostanie rozpatrzony.",
  },
  APPROVED: {
    dotClass: "bg-green-500",
    badgeClass: "border-green-500",
    textClass: "text-green-600",
    label: "Członek klubu",
    heading: "Witaj w Klubie!",
    description: "Twoje członkostwo zostało zatwierdzone. Masz pełny dostęp do społeczności.",
  },
  REJECTED: {
    dotClass: "bg-destructive",
    badgeClass: "border-destructive",
    textClass: "text-destructive",
    label: "Wniosek odrzucony",
    heading: "Wniosek odrzucony",
    description: "Twój wniosek nie został zaakceptowany.",
  },
};

function NoApplicationView({ colors }: { colors: ReturnType<typeof useTheme>["colors"] }) {
  return (
    <View className="gap-5">
      <View className="self-start flex-row items-center gap-2 rounded-full border border-destructive px-3 py-1.5">
        <View className="bg-destructive size-2 rounded-full" />
        <Text className="text-destructive text-xs font-semibold">Status: Niezweryfikowany</Text>
      </View>

      <View className="gap-2">
        <Text className="text-foreground text-3xl font-extrabold tracking-tight leading-9">
          Potwierdź status absolwenta
        </Text>
        <Text className="text-muted-foreground text-sm leading-6">
          Zweryfikuj konto, aby odblokować pełny dostęp do społeczności i wydarzeń.
        </Text>
      </View>

      <View className="gap-3 mt-2">
        <Button
          size="lg"
          className="w-full"
          onPress={() => {
            console.log("[todo] discord verification not yet implemented");
          }}
        >
          <DiscordIcon size={18} color={colors.background} />
          <Text className="font-bold">Zweryfikuj przez Discord</Text>
        </Button>

        <Button size="lg" className="w-full" onPress={() => router.push("/application")}>
          <ClipboardList size={18} color={colors.background} />
          <Text className="font-bold">Złóż wniosek ręcznie</Text>
        </Button>
      </View>
    </View>
  );
}

function ApplicationStatusView({
  status,
  rejectionReason,
  colors,
}: {
  status: "UNDER_REVIEW" | "APPROVED" | "REJECTED";
  rejectionReason?: string | null;
  colors: ReturnType<typeof useTheme>["colors"];
}) {
  const cfg = STATUS_CONFIG[status];

  return (
    <View className="gap-5">
      <View
        className={`self-start flex-row items-center gap-2 rounded-full border px-3 py-1.5 ${cfg.badgeClass}`}
      >
        <View className={`size-2 rounded-full ${cfg.dotClass}`} />
        <Text className={`text-xs font-semibold ${cfg.textClass}`}>{cfg.label}</Text>
      </View>

      <View className="gap-2">
        <Text className="text-foreground text-3xl font-extrabold tracking-tight leading-9">
          {cfg.heading}
        </Text>
        <Text className="text-muted-foreground text-sm leading-6">{cfg.description}</Text>
        {status === "REJECTED" && rejectionReason ? (
          <Text className="text-muted-foreground text-sm leading-6 italic">
            Powód: {rejectionReason}
          </Text>
        ) : null}
      </View>

      {status === "REJECTED" ? (
        <View className="gap-3 mt-2">
          <Button size="lg" className="w-full" onPress={() => router.push("/application")}>
            <ClipboardList size={18} color={colors.background} />
            <Text className="font-bold">Złóż wniosek ponownie</Text>
          </Button>
        </View>
      ) : null}
    </View>
  );
}

export function UserPanel() {
  const { logout } = useAuth();
  const { profile } = useProfile();
  const { colors } = useTheme();
  const { data, isLoading, isError, refetch } = useGetMine();
  const [refreshing, setRefreshing] = React.useState(false);

  const application = data?.data;
  const status = application?.status;
  const knownStatus =
    status === ApplicationResponseDtoStatus.UNDER_REVIEW ||
    status === ApplicationResponseDtoStatus.APPROVED ||
    status === ApplicationResponseDtoStatus.REJECTED
      ? (status as "UNDER_REVIEW" | "APPROVED" | "REJECTED")
      : null;

  const onRefresh = React.useCallback(async () => {
    setRefreshing(true);
    await refetch();
    setRefreshing(false);
  }, [refetch]);

  return (
    <ScrollView
      className="flex-1 bg-background"
      contentContainerClassName="px-4 py-8 gap-8"
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
    >
      <Text variant="h1" className="text-left text-4xl">
        Profil
      </Text>

      {isLoading && !refreshing ? (
        <ActivityIndicator />
      ) : isError || !application || !knownStatus ? (
        <NoApplicationView colors={colors} />
      ) : knownStatus === "APPROVED" ? (
        <AlumniProfileView profile={profile} onEdit={() => router.push("/alumni/profile-edit")} />
      ) : (
        <ApplicationStatusView
          status={knownStatus}
          rejectionReason={application.rejectionReason}
          colors={colors}
        />
      )}

      <Separator />

      <Button size="lg" className="w-full" onPress={logout}>
        <LogOut size={18} color={colors.background} />
        <Text className="font-bold">Wyloguj się</Text>
      </Button>
    </ScrollView>
  );
}
