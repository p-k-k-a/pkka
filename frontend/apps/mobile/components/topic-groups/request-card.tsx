import { RequestStatusBadge } from "@/components/topic-groups/request-status-badge";
import { Button } from "@/components/ui/button";
import { Text } from "@/components/ui/text";
import { TopicGroupRequestStatus, type TopicGroupRequestResponse } from "@pkka/api";
import { formatEventDateLong } from "@pkka/domain";
import { Pressable, View } from "react-native";

type RequestCardProps = {
  request: TopicGroupRequestResponse;
  onPropose: () => void;
};

export function RequestCard({ request, onPropose }: RequestCardProps) {
  const { status } = request;
  const dateLabel =
    status === TopicGroupRequestStatus.UNDER_REVIEW || !request.reviewedAt
      ? `Złożony ${formatEventDateLong(request.createdAt)}`
      : `Rozpatrzony ${formatEventDateLong(request.reviewedAt)}`;

  return (
    <View className="border-muted-foreground/25 dark:border-border bg-card gap-3 rounded-2xl border p-5">
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
        <Text className="border-border text-muted-foreground border-t pt-3 text-[13px] leading-relaxed">
          Decyzja administracji pojawi się w tym miejscu. Kolejny wniosek złożysz po rozpatrzeniu
          tego.
        </Text>
      ) : null}

      {status === TopicGroupRequestStatus.REJECTED ? (
        <>
          {request.rejectionReason ? (
            <View className="bg-muted dark:bg-background gap-1.5 rounded-md p-3">
              <Text className="text-foreground text-xs font-bold uppercase tracking-wider">
                Powód odrzucenia
              </Text>
              <Text className="text-muted-foreground text-sm leading-relaxed">
                {request.rejectionReason}
              </Text>
            </View>
          ) : null}
          <Button size="lg" className="mt-1 w-full" onPress={onPropose}>
            <Text className="text-base font-semibold">Zaproponuj ponownie</Text>
          </Button>
        </>
      ) : null}

      {status === TopicGroupRequestStatus.APPROVED ? (
        <>
          <Text className="text-muted-foreground text-sm leading-relaxed">
            Grupa została utworzona, a Ty jesteś jej właścicielem.
          </Text>
          <Pressable
            role="button"
            onPress={onPropose}
            className="min-h-11 justify-center self-start active:opacity-70"
          >
            <Text className="text-accent text-sm font-semibold underline">
              Zaproponuj kolejną grupę
            </Text>
          </Pressable>
        </>
      ) : null}
    </View>
  );
}
