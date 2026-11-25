import { RequestStatusBadge } from "@/components/topic-groups/request-status-badge";
import { Button } from "@/components/ui/button";
import { Text } from "@/components/ui/text";
import { useThemeColors } from "@/lib/theme";
import {
  ApiError,
  getListMyTopicGroupRequestsQueryKey,
  TopicGroupRequestStatus,
  useWithdrawTopicGroupRequest,
  type TopicGroupRequestResponse,
} from "@pkka/api";
import { formatEventDateLong, topicGroupRequestWithdrawErrorMessage } from "@pkka/domain";
import { useQueryClient } from "@tanstack/react-query";
import { Trash2 } from "lucide-react-native";
import { Alert, View } from "react-native";

type RequestCardProps = {
  request: TopicGroupRequestResponse;
};

export function RequestCard({ request }: RequestCardProps) {
  const { status } = request;
  const theme = useThemeColors();
  const queryClient = useQueryClient();
  const { mutateAsync: withdrawRequest, isPending: isWithdrawing } = useWithdrawTopicGroupRequest();

  const withdraw = async () => {
    try {
      await withdrawRequest({ id: request.id });
    } catch (error) {
      Alert.alert(
        "Nie wycofano wniosku",
        topicGroupRequestWithdrawErrorMessage(error instanceof ApiError ? error.status : undefined),
      );
    }
    await queryClient.invalidateQueries({ queryKey: getListMyTopicGroupRequestsQueryKey() });
  };

  const confirmWithdraw = () => {
    Alert.alert("Wycofać wniosek?", `Wniosek o grupę „${request.name}” zostanie usunięty.`, [
      { text: "Anuluj", style: "cancel" },
      { text: "Wycofaj", style: "destructive", onPress: () => void withdraw() },
    ]);
  };
  const dateLabel =
    status === TopicGroupRequestStatus.UNDER_REVIEW || !request.reviewedAt
      ? `Złożony ${formatEventDateLong(request.createdAt)}`
      : `Rozpatrzony ${formatEventDateLong(request.reviewedAt)}`;

  return (
    <View className="bg-muted gap-3 rounded-2xl p-5">
      <Text className="text-muted-foreground text-xs font-bold uppercase tracking-wider">
        Twój wniosek
      </Text>
      <View className="self-start">
        <RequestStatusBadge status={status} />
      </View>

      <Text
        role="heading"
        aria-level="2"
        className="font-heading text-foreground text-[23px] font-semibold leading-tight"
      >
        {request.name}
      </Text>

      {status === TopicGroupRequestStatus.UNDER_REVIEW ? (
        <Text className="text-muted-foreground text-sm leading-relaxed">{request.description}</Text>
      ) : null}

      <Text className="text-muted-foreground text-xs font-medium">{dateLabel}</Text>

      {status === TopicGroupRequestStatus.UNDER_REVIEW ? (
        <View className="border-border gap-1 border-t pt-3">
          <Text className="text-muted-foreground text-[13px] leading-relaxed">
            Decyzja administracji pojawi się w tym miejscu. Kolejny wniosek złożysz po rozpatrzeniu
            lub wycofaniu tego.
          </Text>
          <Button
            variant="outline"
            size="sm"
            onPress={confirmWithdraw}
            disabled={isWithdrawing}
            className="mt-2 w-full"
          >
            <Trash2 size={16} color={theme.destructive} />
            <Text className="text-destructive font-semibold">
              {isWithdrawing ? "Wycofywanie…" : "Wycofaj wniosek"}
            </Text>
          </Button>
        </View>
      ) : null}

      {status === TopicGroupRequestStatus.REJECTED && request.rejectionReason ? (
        <View className="bg-background gap-1.5 rounded-md p-3">
          <Text className="text-foreground text-xs font-bold uppercase tracking-wider">
            Powód odrzucenia
          </Text>
          <Text className="text-muted-foreground text-sm leading-relaxed">
            {request.rejectionReason}
          </Text>
        </View>
      ) : null}

      {status === TopicGroupRequestStatus.APPROVED ? (
        <Text className="text-muted-foreground text-sm leading-relaxed">
          Grupa została utworzona, a Ty jesteś jej właścicielem.
        </Text>
      ) : null}
    </View>
  );
}
