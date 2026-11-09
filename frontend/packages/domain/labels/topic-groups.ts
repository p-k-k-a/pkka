import type { TopicGroupRequestStatus } from "@pkka/api";

export const TOPIC_GROUP_NAME_MAX_LENGTH = 200;
export const TOPIC_GROUP_DESCRIPTION_MAX_LENGTH = 2000;

const NAME_TAKEN_TITLE = "Topic group name taken";

const REQUEST_STATUS_LABELS: Record<TopicGroupRequestStatus, string> = {
  UNDER_REVIEW: "W trakcie rozpatrywania",
  APPROVED: "Zaakceptowany",
  REJECTED: "Odrzucony",
};

export function topicGroupRequestStatusLabel(status: TopicGroupRequestStatus): string {
  return REQUEST_STATUS_LABELS[status];
}

export function topicGroupRequestSubmitErrorMessage(status: number | undefined, body: unknown) {
  if (status !== 409) return "Nie udało się wysłać wniosku. Spróbuj ponownie.";
  return (body as { title?: string } | null)?.title === NAME_TAKEN_TITLE
    ? "Grupa o tej nazwie już istnieje. Wybierz inną nazwę."
    : "Masz już wniosek w trakcie rozpatrywania. Kolejny złożysz po decyzji administracji.";
}
