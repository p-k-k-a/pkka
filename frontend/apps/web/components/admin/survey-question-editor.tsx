"use client";

import { useState } from "react";
import { ArrowDown, ArrowUp, Circle, Copy, Plus, Square, Trash2, X } from "lucide-react";
import { QuestionType } from "@pkka/api";
import {
  QUESTION_TYPE_OPTIONS,
  SURVEY_MAX_OPTIONS,
  SURVEY_MIN_OPTIONS,
  SURVEY_OPTION_MAX,
  SURVEY_QUESTION_MAX,
  isChoiceQuestion,
  isQuestionType,
  type SurveyQuestionDraft,
} from "@pkka/domain";
import { FieldLabel } from "@/components/admin/field-label";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";

export function newSurveyOption(label = "") {
  return { key: crypto.randomUUID(), label };
}

export function newSurveyQuestion(): SurveyQuestionDraft {
  return {
    key: crypto.randomUUID(),
    content: "",
    type: QuestionType.SINGLE_CHOICE,
    options: [newSurveyOption(), newSurveyOption()],
  };
}

type SurveyQuestionEditorProps = {
  question: SurveyQuestionDraft;
  number: number;
  isFirst: boolean;
  isLast: boolean;
  canRemove: boolean;
  canDuplicate: boolean;
  /** Shown only after a save attempt, so an untouched form isn't covered in errors. */
  issue: string | null;
  onChange: (question: SurveyQuestionDraft) => void;
  onMove: (direction: -1 | 1) => void;
  onDuplicate: () => void;
  onRemove: () => void;
};

export function SurveyQuestionEditor({
  question,
  number,
  isFirst,
  isLast,
  canRemove,
  canDuplicate,
  issue,
  onChange,
  onMove,
  onDuplicate,
  onRemove,
}: SurveyQuestionEditorProps) {
  // The option added last takes focus, so a list of answers can be typed without the mouse.
  const [optionToFocus, setOptionToFocus] = useState<string | null>(null);
  const fieldId = `survey-question-${question.key}`;
  const issueId = `${fieldId}-issue`;
  const choice = isChoiceQuestion(question.type);
  const OptionMark = question.type === QuestionType.MULTI_CHOICE ? Square : Circle;
  const canAddOption = question.options.length < SURVEY_MAX_OPTIONS;

  const setOptions = (options: SurveyQuestionDraft["options"]) =>
    onChange({ ...question, options });

  const addOption = () => {
    if (!canAddOption) return;
    const option = newSurveyOption();
    setOptionToFocus(option.key);
    setOptions([...question.options, option]);
  };

  return (
    <Card
      id={fieldId}
      className="scroll-mt-24 gap-5 rounded-lg p-5 shadow-none"
      aria-label={`Pytanie ${number}`}
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-accent text-xs font-semibold tracking-widest uppercase">
          Pytanie {number}
        </p>
        <div className="flex items-center gap-1">
          <Button
            type="button"
            variant="ghost"
            size="icon"
            disabled={isFirst}
            aria-label={`Przesuń pytanie ${number} wyżej`}
            title="Przesuń wyżej"
            onClick={() => onMove(-1)}
          >
            <ArrowUp />
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="icon"
            disabled={isLast}
            aria-label={`Przesuń pytanie ${number} niżej`}
            title="Przesuń niżej"
            onClick={() => onMove(1)}
          >
            <ArrowDown />
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="icon"
            disabled={!canDuplicate}
            aria-label={`Powiel pytanie ${number}`}
            title="Powiel"
            onClick={onDuplicate}
          >
            <Copy />
          </Button>
          <Button
            type="button"
            variant="ghost"
            size="icon"
            disabled={!canRemove}
            className="text-destructive hover:text-destructive"
            aria-label={`Usuń pytanie ${number}`}
            title="Usuń"
            onClick={onRemove}
          >
            <Trash2 />
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-5 md:grid-cols-[minmax(0,1fr)_220px]">
        <div className="space-y-2">
          <FieldLabel htmlFor={`${fieldId}-content`}>Treść pytania</FieldLabel>
          <Textarea
            id={`${fieldId}-content`}
            value={question.content}
            rows={2}
            maxLength={SURVEY_QUESTION_MAX}
            placeholder="np. Jak oceniasz ostatnie spotkanie klubu?"
            aria-invalid={issue !== null && question.content.trim() === ""}
            aria-describedby={issue ? issueId : undefined}
            onChange={(changeEvent) => onChange({ ...question, content: changeEvent.target.value })}
          />
        </div>
        <div className="space-y-2">
          <FieldLabel htmlFor={`${fieldId}-type`}>Rodzaj odpowiedzi</FieldLabel>
          <Select
            id={`${fieldId}-type`}
            value={question.type}
            onChange={(changeEvent) => {
              if (isQuestionType(changeEvent.target.value)) {
                onChange({ ...question, type: changeEvent.target.value });
              }
            }}
          >
            {QUESTION_TYPE_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </Select>
        </div>
      </div>

      {choice ? (
        <div className="space-y-2">
          <FieldLabel>Odpowiedzi do wyboru</FieldLabel>
          <ul className="space-y-2">
            {question.options.map((option, index) => (
              <li key={option.key} className="flex items-center gap-2">
                <OptionMark className="text-muted-foreground size-4 shrink-0" aria-hidden="true" />
                <Input
                  value={option.label}
                  maxLength={SURVEY_OPTION_MAX}
                  placeholder={`Odpowiedź ${index + 1}`}
                  aria-label={`Pytanie ${number}, odpowiedź ${index + 1}`}
                  autoFocus={option.key === optionToFocus}
                  onChange={(changeEvent) =>
                    setOptions(
                      question.options.map((item) =>
                        item.key === option.key
                          ? { ...item, label: changeEvent.target.value }
                          : item,
                      ),
                    )
                  }
                  onKeyDown={(keyEvent) => {
                    if (keyEvent.key === "Enter") {
                      keyEvent.preventDefault();
                      addOption();
                    }
                  }}
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  disabled={question.options.length <= SURVEY_MIN_OPTIONS}
                  aria-label={`Usuń odpowiedź ${index + 1} z pytania ${number}`}
                  title="Usuń odpowiedź"
                  onClick={() =>
                    setOptions(question.options.filter((item) => item.key !== option.key))
                  }
                >
                  <X />
                </Button>
              </li>
            ))}
          </ul>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="-ml-2"
            disabled={!canAddOption}
            onClick={addOption}
          >
            <Plus data-icon="inline-start" />
            Dodaj odpowiedź
          </Button>
        </div>
      ) : (
        <p className="text-muted-foreground bg-muted dark:bg-background rounded-lg px-4 py-3 text-sm">
          Alumn wpisze odpowiedź własnymi słowami.
        </p>
      )}

      {issue ? (
        <p id={issueId} className="text-destructive text-xs font-medium">
          {issue}
        </p>
      ) : null}
    </Card>
  );
}
