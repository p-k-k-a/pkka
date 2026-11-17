import { QuestionType, SurveyAudience, SurveyStatus, type SurveyQuestionRequest } from "@pkka/api";
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
// A grid with one row is a single choice; more than this stops fitting on a phone.
export const SURVEY_MIN_ROWS = 2;
export const SURVEY_MAX_ROWS = 15;

export const SURVEYS_PAGE_SIZE = 20;

const QUESTION_TYPE_LABELS: Record<QuestionType, string> = {
  SINGLE_CHOICE: "Jedna odpowiedź",
  MULTI_CHOICE: "Wiele odpowiedzi",
  TEXT: "Odpowiedź tekstowa",
  GRID: "Siatka (jedna odpowiedź w wierszu)",
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
  if (type === QuestionType.GRID) return "Wybierz jedną odpowiedź w każdym wierszu.";
  return "Odpowiedz własnymi słowami.";
}

const SURVEY_AUDIENCE_LABELS: Record<SurveyAudience, string> = {
  ALL_USERS: "Wszyscy zalogowani",
  VERIFIED_ALUMNI: "Zweryfikowani alumni",
};

export function surveyAudienceLabel(audience: SurveyAudience) {
  return SURVEY_AUDIENCE_LABELS[audience];
}

export const SURVEY_AUDIENCE_OPTIONS = Object.values(SurveyAudience).map((value) => ({
  value,
  label: SURVEY_AUDIENCE_LABELS[value],
}));

export function isSurveyAudience(value: unknown): value is SurveyAudience {
  return Object.values(SurveyAudience).includes(value as SurveyAudience);
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
  /** The answers to choose from; the columns of a grid. */
  options: SurveyOptionDraft[];
  /** Used by a grid only, kept for the other types so switching back restores them. */
  rows: SurveyOptionDraft[];
};

function listIssue(
  items: SurveyOptionDraft[],
  min: number,
  noun: { empty: string; few: string; same: string },
) {
  const labels = items.map((item) => item.label.trim());
  if (labels.some((label) => label === "")) return noun.empty;
  if (labels.length < min) return noun.few;
  const unique = new Set(labels.map((label) => label.toLocaleLowerCase("pl")));
  if (unique.size !== labels.length) return noun.same;
  return null;
}

/** The first thing wrong with a question in the survey builder, or null when it can be saved. */
export function surveyQuestionIssue(question: SurveyQuestionDraft) {
  if (question.content.trim() === "") return "Wpisz treść pytania.";
  if (!isChoiceQuestion(question.type)) return null;

  if (question.type === QuestionType.GRID) {
    return (
      listIssue(question.rows, SURVEY_MIN_ROWS, {
        empty: "Uzupełnij lub usuń puste wiersze.",
        few: `Dodaj co najmniej ${SURVEY_MIN_ROWS} wiersze.`,
        same: "Wiersze nie mogą się powtarzać.",
      }) ??
      listIssue(question.options, SURVEY_MIN_OPTIONS, {
        empty: "Uzupełnij lub usuń puste kolumny.",
        few: `Dodaj co najmniej ${SURVEY_MIN_OPTIONS} kolumny.`,
        same: "Kolumny nie mogą się powtarzać.",
      })
    );
  }
  return listIssue(question.options, SURVEY_MIN_OPTIONS, {
    empty: "Uzupełnij lub usuń puste odpowiedzi.",
    few: `Dodaj co najmniej ${SURVEY_MIN_OPTIONS} odpowiedzi do wyboru.`,
    same: "Odpowiedzi nie mogą się powtarzać.",
  });
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
    rows: question.type === QuestionType.GRID ? question.rows.map((row) => row.label.trim()) : [],
  };
}

// An answer travels the way the API stores it: the option id for a single choice, option
// ids joined with commas for a multiple choice, `rowId:optionId` pairs joined with commas
// for a grid, the text itself for a text question.
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

/** The option picked in each row of a grid answer, by row id. */
export function gridChoices(value: string): Record<string, string> {
  const choices: Record<string, string> = {};
  for (const pair of value === "" ? [] : value.split(",")) {
    const [rowId, optionId] = pair.split(":");
    if (rowId && optionId) choices[rowId] = optionId;
  }
  return choices;
}

export function setGridChoice(value: string, rowId: string, optionId: string) {
  const choices = { ...gridChoices(value), [rowId]: optionId };
  return Object.entries(choices)
    .map(([row, option]) => `${row}:${option}`)
    .join(",");
}

/** A grid counts as answered only once every row has an option picked. */
export function isSurveyQuestionAnswered(
  question: { type: QuestionType; rows?: { id: string }[] },
  value: string | undefined,
) {
  if (!isSurveyAnswerGiven(value)) return false;
  if (question.type !== QuestionType.GRID) return true;
  const choices = gridChoices(value ?? "");
  return (question.rows ?? []).every((row) => choices[row.id] !== undefined);
}
