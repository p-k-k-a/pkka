"use client";

import { QuestionType } from "@pkka/api";
import {
  gridChoices,
  questionTypeHint,
  selectedOptionIds,
  setGridChoice,
  toggleSelectedOption,
} from "@pkka/domain";
import { Checkbox } from "@/components/ui/checkbox";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "@/lib/utils";

export type SurveyFieldQuestion = {
  id: string;
  content: string;
  type: QuestionType;
  options?: { id: string; label: string }[];
  /** The rows of a grid, each answered with one of `options`. */
  rows?: { id: string; label: string }[];
};

type SurveyQuestionFieldProps = {
  question: SurveyFieldQuestion;
  /** 1-based position shown in front of the question. */
  number: number;
  /** In the API's answer format, see `selectedOptionIds` in `@pkka/domain`. */
  value: string;
  onChange: (value: string) => void;
  error?: string;
  disabled?: boolean;
};

const OPTION_ROW =
  "border-muted-foreground/25 dark:border-input hover:bg-muted/60 has-[[data-state=checked]]:border-primary has-[[data-state=checked]]:bg-primary/10 flex cursor-pointer items-start gap-3 rounded-lg border px-4 py-3 text-sm transition-colors has-[:disabled]:cursor-not-allowed";

// The default control border is the page's hairline colour, too faint to read as a control here.
const OPTION_MARK = "border-muted-foreground/60 dark:border-muted-foreground mt-0.5";

export function SurveyQuestionField({
  question,
  number,
  value,
  onChange,
  error,
  disabled,
}: SurveyQuestionFieldProps) {
  const options = question.options ?? [];
  const titleId = `survey-question-${question.id}-title`;
  const hintId = `survey-question-${question.id}-hint`;
  const errorId = `survey-question-${question.id}-error`;
  const describedBy = error ? `${hintId} ${errorId}` : hintId;

  return (
    <div id={`survey-question-${question.id}`} className="scroll-mt-24 space-y-4">
      <div className="space-y-1">
        <h3
          id={titleId}
          className="font-heading text-foreground flex gap-2 text-lg leading-snug font-semibold"
        >
          <span className="text-accent tabular-nums">{number}.</span>
          <span className="min-w-0 break-words">{question.content || "Pytanie bez treści"}</span>
        </h3>
        <p id={hintId} className="text-muted-foreground text-xs">
          {questionTypeHint(question.type)}
        </p>
      </div>

      {question.type === QuestionType.SINGLE_CHOICE ? (
        <RadioGroup
          value={value}
          onValueChange={onChange}
          disabled={disabled}
          aria-labelledby={titleId}
          aria-describedby={describedBy}
          aria-invalid={error ? true : undefined}
        >
          {options.map((option) => (
            <label key={option.id} className={OPTION_ROW}>
              <RadioGroupItem value={option.id} className={OPTION_MARK} />
              <span className="text-foreground min-w-0 break-words">{option.label}</span>
            </label>
          ))}
        </RadioGroup>
      ) : null}

      {question.type === QuestionType.MULTI_CHOICE ? (
        <div
          role="group"
          aria-labelledby={titleId}
          aria-describedby={describedBy}
          className="grid w-full gap-2"
        >
          {options.map((option) => (
            <label key={option.id} className={OPTION_ROW}>
              <Checkbox
                className={OPTION_MARK}
                disabled={disabled}
                checked={selectedOptionIds(value).includes(option.id)}
                onCheckedChange={(checked) =>
                  onChange(toggleSelectedOption(value, option.id, checked === true))
                }
              />
              <span className="text-foreground min-w-0 break-words">{option.label}</span>
            </label>
          ))}
        </div>
      ) : null}

      {question.type === QuestionType.GRID ? (
        <SurveyGrid
          question={question}
          value={value}
          onChange={onChange}
          disabled={disabled}
          showMissing={error !== undefined}
          labelledBy={titleId}
          describedBy={describedBy}
        />
      ) : null}

      {question.type === QuestionType.TEXT ? (
        <Textarea
          value={value}
          rows={4}
          disabled={disabled}
          placeholder="Twoja odpowiedź"
          aria-labelledby={titleId}
          aria-describedby={describedBy}
          aria-invalid={error ? true : undefined}
          onChange={(changeEvent) => onChange(changeEvent.target.value)}
        />
      ) : null}

      {error ? (
        <p id={errorId} className="text-destructive text-xs font-medium">
          {error}
        </p>
      ) : null}
    </div>
  );
}

type SurveyGridProps = {
  question: SurveyFieldQuestion;
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  /** Marks the rows still without an answer, once the form has pointed at this question. */
  showMissing: boolean;
  labelledBy: string;
  describedBy: string;
};

/** One radio group per row; the columns are the question's options. */
function SurveyGrid({
  question,
  value,
  onChange,
  disabled,
  showMissing,
  labelledBy,
  describedBy,
}: SurveyGridProps) {
  const options = question.options ?? [];
  const rows = question.rows ?? [];
  const choices = gridChoices(value);
  const columns = {
    gridTemplateColumns: `minmax(8rem, 1.6fr) repeat(${options.length}, minmax(4.5rem, 1fr))`,
  };

  return (
    // Scrolls sideways on its own on a narrow screen, keeping the row labels in view.
    <div
      role="group"
      aria-labelledby={labelledBy}
      aria-describedby={describedBy}
      className="border-muted-foreground/25 dark:border-input overflow-x-auto rounded-lg border"
    >
      <div className="grid min-w-fit text-sm" style={columns}>
        <div className="bg-background sticky left-0" aria-hidden="true" />
        {options.map((option) => (
          <div
            key={option.id}
            aria-hidden="true"
            className="text-muted-foreground px-2 py-3 text-center text-xs font-medium break-words"
          >
            {option.label}
          </div>
        ))}
        {rows.map((row) => {
          const rowLabelId = `survey-grid-${question.id}-${row.id}`;
          const missing = showMissing && choices[row.id] === undefined;
          return (
            <RadioGroup
              key={row.id}
              value={choices[row.id] ?? ""}
              onValueChange={(optionId) => onChange(setGridChoice(value, row.id, optionId))}
              disabled={disabled}
              aria-labelledby={rowLabelId}
              aria-invalid={missing ? true : undefined}
              className={cn(
                "border-muted-foreground/15 dark:border-input col-span-full grid grid-cols-subgrid gap-0 border-t",
                missing && "bg-destructive/5",
              )}
            >
              <span
                id={rowLabelId}
                className={cn(
                  "bg-background sticky left-0 flex items-center px-4 py-3 break-words",
                  missing ? "text-destructive font-medium" : "text-foreground",
                )}
              >
                {row.label}
              </span>
              {options.map((option) => (
                <label
                  key={option.id}
                  className="hover:bg-muted/60 flex cursor-pointer items-center justify-center py-3 transition-colors has-[:disabled]:cursor-not-allowed"
                >
                  <RadioGroupItem
                    value={option.id}
                    aria-label={option.label}
                    className="border-muted-foreground/60 dark:border-muted-foreground"
                  />
                </label>
              ))}
            </RadioGroup>
          );
        })}
      </div>
    </div>
  );
}
