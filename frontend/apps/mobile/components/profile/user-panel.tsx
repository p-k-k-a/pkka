import { AlumniProfileView } from "@/components/alumni/alumni-profile-view";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { DiscordIcon } from "@/components/ui/svg-icons";
import { Text } from "@/components/ui/text";
import { useAuth } from "@/lib/auth-context";
import {
  ApiError,
  ApplicationResponseStatus,
  ProfileResponse,
  useGetMine,
  useGetMyProfile,
} from "@pkka/api";
import { useTheme } from "@react-navigation/native";
import { router } from "expo-router";
import { ClipboardList, LogOut, RotateCcw } from "lucide-react-native";
import { useCallback, useState } from "react";
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
        <Button size="lg" className="w-full" disabled>
          <DiscordIcon size={18} color={colors.background} />
          <Text className="font-bold">Zweryfikuj przez Discord (wkrótce)</Text>
        </Button>

        <Button size="lg" className="w-full" onPress={() => router.push("/application")}>
          <ClipboardList size={18} color={colors.background} />
          <Text className="font-bold">Złóż wniosek ręcznie</Text>
        </Button>
      </View>
    </View>
  );
}

function StatusUnavailableView({
  colors,
  onRetry,
}: {
  colors: ReturnType<typeof useTheme>["colors"];
  onRetry: () => void;
}) {
  return (
    <View className="gap-5">
      <View className="self-start flex-row items-center gap-2 rounded-full border border-muted-foreground px-3 py-1.5">
        <View className="bg-muted-foreground size-2 rounded-full" />
        <Text className="text-muted-foreground text-xs font-semibold">Status: Nieznany</Text>
      </View>

      <View className="gap-2">
        <Text className="text-foreground text-3xl font-extrabold tracking-tight leading-9">
          Nie udało się wczytać statusu
        </Text>
        <Text className="text-muted-foreground text-sm leading-6">
          Sprawdź połączenie z internetem.
        </Text>
      </View>

      <View className="gap-3 mt-2">
        <Button size="lg" className="w-full" onPress={onRetry}>
          <RotateCcw size={18} color={colors.background} />
          <Text className="font-bold">Spróbuj ponownie</Text>
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

function AlumniProfileSection({
  profile,
  isPending,
  isError,
}: {
  profile: ProfileResponse | undefined;
  isPending: boolean;
  isError: boolean;
}) {
  if (isPending) return <ActivityIndicator />;
  if (isError || !profile) {
    return (
      <Text className="text-muted-foreground text-sm leading-6">
        Nie udało się wczytać profilu. Pociągnij w dół, aby odświeżyć.
      </Text>
    );
  }
  return <AlumniProfileView profile={profile} onEdit={() => router.push("/alumni/profile-edit")} />;
}

export function UserPanel() {
  const { logout } = useAuth();
  const { colors } = useTheme();
  const { data, isLoading, isError, error, refetch } = useGetMine();
  const [refreshing, setRefreshing] = useState(false);

  // GET /api/applications/me answers 404 when the user simply hasn't applied yet; every
  // other failure is a load error and must not be dressed up as "not verified". The cast
  // is needed because that 404 carries no schema, so the hook types its error as `void`.
  const loadError = error as unknown;
  const missingApplication = loadError instanceof ApiError && loadError.status === 404;
  const loadFailed = isError && !missingApplication;

  const application = data?.data;
  const status = application?.status;
  const knownStatus =
    status === ApplicationResponseStatus.UNDER_REVIEW ||
    status === ApplicationResponseStatus.APPROVED ||
    status === ApplicationResponseStatus.REJECTED
      ? (status as "UNDER_REVIEW" | "APPROVED" | "REJECTED")
      : null;

  const {
    data: profileData,
    // isLoading, not isPending: a disabled query stays "pending" forever.
    isLoading: profilePending,
    isError: profileError,
    refetch: refetchProfile,
  } = useGetMyProfile({ query: { enabled: knownStatus === "APPROVED" } });

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await Promise.all([refetch(), refetchProfile()]);
    setRefreshing(false);
  }, [refetch, refetchProfile]);

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
      ) : loadFailed ? (
        <StatusUnavailableView colors={colors} onRetry={() => void refetch()} />
      ) : !application || !knownStatus ? (
        <NoApplicationView colors={colors} />
      ) : knownStatus === "APPROVED" ? (
        <AlumniProfileSection
          profile={profileData?.data}
          isPending={profilePending && !refreshing}
          isError={profileError}
        />
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
