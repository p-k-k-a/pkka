import { AlumniProfileView } from "@/components/alumni/alumni-profile-view";
import { SettingsSection } from "@/components/profile/settings-section";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { Separator } from "@/components/ui/separator";
import { DiscordIcon } from "@pkka/icons/native";
import { Text } from "@/components/ui/text";
import { useAuth } from "@/lib/auth-context";
import {
  ApiError,
  ApplicationStatus,
  ProfileResponse,
  useGetMine,
  useGetMyProfile,
} from "@pkka/api";
import { useThemeColors, type THEME } from "@/lib/theme";
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

const STATUS_CONFIG: Record<ApplicationStatus, StatusConfig> = {
  UNDER_REVIEW: {
    dotClass: "bg-secondary-foreground",
    badgeClass: "border-secondary-foreground/30 bg-secondary",
    textClass: "text-secondary-foreground",
    label: "Wniosek w trakcie rozpatrywania",
    heading: "Wniosek złożony",
    description:
      "Twój wniosek o członkostwo jest rozpatrywany. Powiadomimy Cię, gdy zostanie rozpatrzony.",
  },
  APPROVED: {
    dotClass: "bg-primary-foreground",
    badgeClass: "border-primary bg-primary",
    textClass: "text-primary-foreground",
    label: "Członek klubu",
    heading: "Witaj w Klubie!",
    description: "Twoje członkostwo zostało zatwierdzone. Masz pełny dostęp do społeczności.",
  },
  REJECTED: {
    dotClass: "bg-destructive",
    badgeClass: "border-destructive bg-destructive/10",
    textClass: "text-foreground",
    label: "Wniosek odrzucony",
    heading: "Wniosek odrzucony",
    description: "Twój wniosek nie został zaakceptowany.",
  },
};

function DiscordVerifyButton({ colors }: { colors: (typeof THEME)["light"] }) {
  const { loginDiscord } = useAuth();

  return (
    <Button size="lg" variant="outline" className="w-full" onPress={loginDiscord}>
      <DiscordIcon size={18} color={colors.foreground} />
      <Text className="font-bold">Zweryfikuj przez Discord</Text>
    </Button>
  );
}

function NoApplicationView({ colors }: { colors: (typeof THEME)["light"] }) {
  return (
    <View className="gap-5">
      <View className="self-start flex-row items-center gap-2 rounded-full border border-destructive bg-destructive/10 px-3 py-1.5">
        <View className="bg-destructive size-2 rounded-full" />
        <Text className="text-foreground text-xs font-semibold">Status: Niezweryfikowany</Text>
      </View>

      <View className="gap-2">
        <Text className="font-heading text-foreground text-[28px] font-semibold leading-tight tracking-tight">
          Potwierdź status absolwenta
        </Text>
        <Text className="text-muted-foreground text-sm leading-6">
          Zweryfikuj konto, aby odblokować pełny dostęp do społeczności i wydarzeń.
        </Text>
      </View>

      <View className="gap-3 mt-2">
        <DiscordVerifyButton colors={colors} />

        <Button size="lg" className="w-full" onPress={() => router.push("/application")}>
          <ClipboardList size={18} color={colors.primaryForeground} />
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
  colors: (typeof THEME)["light"];
  onRetry: () => void;
}) {
  return (
    <View className="gap-5">
      <View className="self-start flex-row items-center gap-2 rounded-full border border-muted-foreground bg-muted px-3 py-1.5">
        <View className="bg-muted-foreground size-2 rounded-full" />
        <Text className="text-muted-foreground text-xs font-semibold">Status: Nieznany</Text>
      </View>

      <View className="gap-2">
        <Text className="font-heading text-foreground text-[28px] font-semibold leading-tight tracking-tight">
          Nie udało się wczytać statusu
        </Text>
        <Text className="text-muted-foreground text-sm leading-6">
          Sprawdź połączenie z internetem.
        </Text>
      </View>

      <View className="gap-3 mt-2">
        <Button size="lg" className="w-full" onPress={onRetry}>
          <RotateCcw size={18} color={colors.primaryForeground} />
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
  status: ApplicationStatus;
  rejectionReason?: string | null;
  colors: (typeof THEME)["light"];
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
        <Text className="font-heading text-foreground text-[28px] font-semibold leading-tight tracking-tight">
          {cfg.heading}
        </Text>
        <Text className="text-muted-foreground text-sm leading-6">{cfg.description}</Text>
        {status === ApplicationStatus.REJECTED && rejectionReason ? (
          <Text className="text-muted-foreground text-sm leading-6 italic">
            Powód: {rejectionReason}
          </Text>
        ) : null}
      </View>

      {status === ApplicationStatus.REJECTED ? (
        <View className="gap-3 mt-2">
          <Button size="lg" className="w-full" onPress={() => router.push("/application")}>
            <ClipboardList size={18} color={colors.primaryForeground} />
            <Text className="font-bold">Złóż wniosek ponownie</Text>
          </Button>
          <DiscordVerifyButton colors={colors} />
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
  const colors = useThemeColors();
  const { data, isLoading, isError, error, refetch } = useGetMine();
  const [refreshing, setRefreshing] = useState(false);

  // GET /api/applications/me answers 404 when the user simply hasn't applied yet; every
  // other failure is a load error and must not be dressed up as "not verified". The cast
  // is needed because that 404 carries no schema, so the hook types its error as `void`.
  const loadError = error as unknown;
  const missingApplication = loadError instanceof ApiError && loadError.status === 404;
  const loadFailed = isError && !missingApplication;

  const application = data?.data;
  const isApproved = application?.status === ApplicationStatus.APPROVED;

  const {
    data: profileData,
    // isLoading, not isPending: a disabled query stays "pending" forever.
    isLoading: profilePending,
    isError: profileError,
    refetch: refetchProfile,
  } = useGetMyProfile({ query: { enabled: isApproved } });

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await Promise.all([refetch(), refetchProfile()]);
    setRefreshing(false);
  }, [refetch, refetchProfile]);

  return (
    <ScrollView
      className="flex-1 bg-background"
      showsVerticalScrollIndicator={false}
      contentContainerClassName="pb-10"
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} />}
    >
      <PageHeader title="Profil" className="pb-8" />

      <View className="gap-8 px-5 pt-8">
        {isLoading && !refreshing ? (
          <ActivityIndicator />
        ) : loadFailed ? (
          <StatusUnavailableView colors={colors} onRetry={() => void refetch()} />
        ) : !application ? (
          <NoApplicationView colors={colors} />
        ) : isApproved ? (
          <AlumniProfileSection
            profile={profileData?.data}
            isPending={profilePending && !refreshing}
            isError={profileError}
          />
        ) : (
          <ApplicationStatusView
            status={application.status}
            rejectionReason={application.rejectionReason}
            colors={colors}
          />
        )}

        <SettingsSection />

        <Separator />

        <Button size="lg" variant="outline" className="w-full" onPress={logout}>
          <LogOut size={18} color={colors.destructive} />
          <Text className="font-bold">Wyloguj się</Text>
        </Button>
      </View>
    </ScrollView>
  );
}
