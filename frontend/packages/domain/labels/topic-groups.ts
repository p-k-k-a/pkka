import type { TopicGroupRequestStatus } from "@pkka/api";

export const TOPIC_GROUP_NAME_MAX_LENGTH = 200;
export const TOPIC_GROUP_DESCRIPTION_MAX_LENGTH = 2000;

export const TOPIC_GROUP_REQUEST_OPEN_MESSAGE =
  "Masz już wniosek w trakcie rozpatrywania. Kolejny złożysz po decyzji administracji.";
export const TOPIC_GROUP_NAME_TAKEN_MESSAGE =
  "Grupa o tej nazwie już istnieje. Wybierz inną nazwę.";
export const TOPIC_GROUP_REQUEST_SUBMIT_ERROR_MESSAGE =
  "Nie udało się wysłać wniosku. Spróbuj ponownie.";

const REQUEST_STATUS_LABELS: Record<TopicGroupRequestStatus, string> = {
  UNDER_REVIEW: "W trakcie rozpatrywania",
  APPROVED: "Zaakceptowany",
  REJECTED: "Odrzucony",
};

export function topicGroupRequestStatusLabel(status: TopicGroupRequestStatus): string {
  return REQUEST_STATUS_LABELS[status];
}
