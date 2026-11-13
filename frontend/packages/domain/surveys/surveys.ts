import { QuestionType, SurveyStatus, type SurveyQuestionRequest } from "@pkka/api";
import { pluralPl } from "../format/plural";

// Mirrors the constraints on the backend's survey requests.
export const SURVEY_TITLE_MAX = 300;
export const SURVEY_OPTION_MAX = 500;
// The backend stores a question as unbounded text; this keeps it a question, not an essay.
export const SURVEY_QUESTION_MAX = 500;

// A choice between fewer than two answers is no question. The backend accepts one.
export const SURVEY_MIN_OPTIONS = 2;
// Keeps a survey short enough to finish; the backend sets no limit of its own.
export const SURVEY_MAX_QUESTIONS = 30;
export const SURVEY_MAX_OPTIONS = 15;

// The backend stores a text answer without a limit; this one keeps it readable in the results.
export const SURVEY_TEXT_ANSWER_MAX = 2000;

export const SURVEYS_PAGE_SIZE = 20;

const QUESTION_TYPE_LABELS: Record<QuestionType, string> = {
  SINGLE_CHOICE: "Jedna odpowiedź",
  MULTI_CHOICE: "Wiele odpowiedzi",
  TEXT: "Odpowiedź tekstowa",
};

export function questionTypeLabel(type: QuestionType) {
  return QUESTION_TYPE_LABELS[type];
}

export const QUESTION_TYPE_OPTIONS = Object.values(QuestionType).map((value) => ({
  value,
  label: QUESTION_TYPE_LABELS[value],
}));

export function isQuestionType(value: unknown): value is QuestionType {
  return Object.values(QuestionType).includes(value as QuestionType);
}

export function isChoiceQuestion(type: QuestionType) {
  return type !== QuestionType.TEXT;
}

/** What the respondent is told to do with a question of this type. */
export function questionTypeHint(type: QuestionType) {
  if (type === QuestionType.SINGLE_CHOICE) return "Wybierz jedną odpowiedź.";
  if (type === QuestionType.MULTI_CHOICE) return "Możesz zaznaczyć kilka odpowiedzi.";
  return "Odpowiedz własnymi słowami.";
}

// The status alone doesn't say whether people can still answer: an ACTIVE survey past
// its deadline is over, exactly like a CLOSED one.
export type SurveyPhase = "draft" | "active" | "ended";

export function surveyPhase(status: SurveyStatus, endsAt: string, now = Date.now()): SurveyPhase {
  if (status === SurveyStatus.DRAFT) return "draft";
  if (status === SurveyStatus.ACTIVE && Date.parse(endsAt) > now) return "active";
  return "ended";
}

const SURVEY_PHASE_LABELS: Record<SurveyPhase, string> = {
  draft: "Szkic",
  active: "Aktywna",
  ended: "Zakończona",
};

export function surveyPhaseLabel(phase: SurveyPhase) {
  return SURVEY_PHASE_LABELS[phase];
}

export function formatSurveyCount(count: number) {
  return `${count} ${pluralPl(count, "ankieta", "ankiety", "ankiet")}`;
}

export function formatQuestionCount(count: number) {
  return `${count} ${pluralPl(count, "pytanie", "pytania", "pytań")}`;
}

/** A submission is one person's filled-in survey, not a single answer. */
export function formatSubmissionCount(count: number) {
  return `${count} ${pluralPl(count, "wypełnienie", "wypełnienia", "wypełnień")}`;
}

export function formatAnswerCount(count: number) {
  return `${count} ${pluralPl(count, "odpowiedź", "odpowiedzi", "odpowiedzi")}`;
}

/** Share of the people who filled in the survey, as a whole percent. */
export function submissionShare(count: number, submissionCount: number) {
  return submissionCount === 0 ? 0 : Math.round((count / submissionCount) * 100);
}

export type SurveyOptionDraft = { key: string; label: string };

export type SurveyQuestionDraft = {
  key: string;
  content: string;
  type: QuestionType;
  options: SurveyOptionDraft[];
};

/** The first thing wrong with a question in the survey builder, or null when it can be saved. */
export function surveyQuestionIssue(question: SurveyQuestionDraft) {
  if (question.content.trim() === "") return "Wpisz treść pytania.";
  if (!isChoiceQuestion(question.type)) return null;

  const labels = question.options.map((option) => option.label.trim());
  if (labels.some((label) => label === "")) return "Uzupełnij lub usuń puste odpowiedzi.";
  if (labels.length < SURVEY_MIN_OPTIONS) {
    return `Dodaj co najmniej ${SURVEY_MIN_OPTIONS} odpowiedzi do wyboru.`;
  }
  const unique = new Set(labels.map((label) => label.toLocaleLowerCase("pl")));
  if (unique.size !== labels.length) return "Odpowiedzi nie mogą się powtarzać.";
  return null;
}

export function toSurveyQuestionRequest(question: SurveyQuestionDraft): SurveyQuestionRequest {
  return {
    content: question.content.trim(),
    type: question.type,
    // Options typed before switching a question to text stay in the draft, so switching
    // back restores them, but the API refuses a text question that carries any.
    options: isChoiceQuestion(question.type)
      ? question.options.map((option) => option.label.trim())
      : [],
  };
}

// An answer travels the way the API stores it: the option id for a single choice, option
// ids joined with commas for a multiple choice, the text itself for a text question.
export function selectedOptionIds(value: string) {
  return value === "" ? [] : value.split(",");
}

export function toggleSelectedOption(value: string, optionId: string, selected: boolean) {
  const others = selectedOptionIds(value).filter((id) => id !== optionId);
  return (selected ? [...others, optionId] : others).join(",");
}

export function isSurveyAnswerGiven(value: string | undefined) {
  return value !== undefined && value.trim() !== "";
}

/** How long a survey stays open, e.g. "3 dni"; null once the deadline has passed. */
export function surveyTimeLeft(endsAt: string, now = Date.now()) {
  const left = Date.parse(endsAt) - now;
  if (Number.isNaN(left) || left <= 0) return null;
  const hours = Math.floor(left / 3_600_000);
  const days = Math.floor(hours / 24);
  if (days >= 1) return `${days} ${pluralPl(days, "dzień", "dni", "dni")}`;
  if (hours >= 1) return `${hours} ${pluralPl(hours, "godzina", "godziny", "godzin")}`;
  return "mniej niż godzina";
}

export type SurveyProblem = "submitted" | "ended" | "missing" | "session" | "invalid" | "unknown";

/**
 * Why a survey could not be loaded or sent. Reads `status` and `body.reason` structurally,
 * the way `ApiError` carries them, since this package must not import the client's mutator.
 */
export function surveyProblem(error: unknown): SurveyProblem {
  const { status, body } = (error ?? {}) as { status?: unknown; body?: unknown };
  const reason = (body as { reason?: unknown } | null | undefined)?.reason;

  if (status === 401) return "session";
  if (reason === "ALREADY_SUBMITTED") return "submitted";
  if (reason === "NOT_ACTIVE") return "ended";
  if (status === 404) return "missing";
  // The answers no longer match the questions: the survey was edited after it was opened.
  if (status === 400) return "invalid";
  return "unknown";
}
