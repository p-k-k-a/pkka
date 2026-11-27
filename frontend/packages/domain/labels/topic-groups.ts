import type { TopicGroupMemberResponse, TopicGroupRequestStatus } from "@pkka/api";
import { pluralPl } from "../format/plural";

export const TOPIC_GROUP_NAME_MAX_LENGTH = 200;
export const TOPIC_GROUP_DESCRIPTION_MAX_LENGTH = 2000;
export const TOPIC_GROUP_REJECTION_REASON_MAX_LENGTH = 1000;

const NAME_TAKEN_TITLE = "Topic group name taken";

const REQUEST_STATUS_LABELS: Record<TopicGroupRequestStatus, string> = {
  UNDER_REVIEW: "W trakcie rozpatrywania",
  APPROVED: "Zaakceptowany",
  REJECTED: "Odrzucony",
};

export function topicGroupRequestStatusLabel(status: TopicGroupRequestStatus): string {
  return REQUEST_STATUS_LABELS[status];
}

export function isTopicGroupNameTaken(status: number | undefined, body: unknown) {
  return status === 409 && (body as { title?: string } | null)?.title === NAME_TAKEN_TITLE;
}

export function topicGroupRequestSubmitErrorMessage(status: number | undefined, body: unknown) {
  if (status !== 409) return "Nie udało się wysłać wniosku. Spróbuj ponownie.";
  return isTopicGroupNameTaken(status, body)
    ? "Grupa o tej nazwie już istnieje. Wybierz inną nazwę."
    : "Masz już wniosek w trakcie rozpatrywania. Kolejny złożysz po decyzji administracji.";
}

export function topicGroupRequestWithdrawErrorMessage(status: number | undefined) {
  if (status === 409)
    return "Administracja rozpatrzyła już ten wniosek, więc nie można go wycofać.";
  if (status === 404) return "Ten wniosek został już wycofany.";
  return "Nie udało się wycofać wniosku. Spróbuj ponownie.";
}

/** The request was withdrawn (404) or another admin already decided it (409 other than a name clash). */
export function isTopicGroupRequestAlreadyReviewed(status: number | undefined, body: unknown) {
  return status === 404 || (status === 409 && !isTopicGroupNameTaken(status, body));
}

export function topicGroupRequestReviewErrorMessage(status: number | undefined, body: unknown) {
  return isTopicGroupNameTaken(status, body)
    ? "Grupa o tej nazwie już istnieje. Odrzuć wniosek i poproś o inną nazwę."
    : "Nie udało się zapisać decyzji. Odśwież stronę i spróbuj ponownie.";
}

export function topicGroupMemberCountLabel(count: number) {
  return `${count} ${pluralPl(count, "członek", "członkowie", "członków")}`;
}

export function topicGroupMemberName(member: TopicGroupMemberResponse) {
  const name = [member.firstName, member.lastName].filter(Boolean).join(" ");
  return name || "Alumn";
}

export function topicGroupMemberShortName(member: TopicGroupMemberResponse) {
  if (!member.firstName) return "Alumn";
  const initial = member.lastName?.trim().charAt(0);
  return initial ? `${member.firstName} ${initial}.` : member.firstName;
}

export function topicGroupMembershipErrorMessage(status: number | undefined, joining: boolean) {
  if (status === 409) return "Najpierw przekaż grupę innemu członkowi.";
  if (status === 404) return "Ta grupa już nie istnieje.";
  return joining
    ? "Nie udało się dołączyć do grupy. Spróbuj ponownie."
    : "Nie udało się opuścić grupy. Spróbuj ponownie.";
}
