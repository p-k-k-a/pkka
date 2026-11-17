"use client";

import { useState } from "react";
import {
  ArrowDown,
  ArrowUp,
  Circle,
  Copy,
  Minus,
  Plus,
  Square,
  Trash2,
  X,
  type LucideIcon,
} from "lucide-react";
import { QuestionType } from "@pkka/api";
import {
  QUESTION_TYPE_OPTIONS,
  SURVEY_MAX_OPTIONS,
  SURVEY_MAX_ROWS,
  SURVEY_MIN_OPTIONS,
  SURVEY_MIN_ROWS,
  SURVEY_OPTION_MAX,
  SURVEY_QUESTION_MAX,
  isChoiceQuestion,
  isQuestionType,
  type SurveyOptionDraft,
  type SurveyQuestionDraft,
} from "@pkka/domain";
import { FieldLabel } from "@/components/admin/field-label";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";

// Keys only tell rows apart on this screen, and `crypto.randomUUID` exists on https alone.
let draftKeys = 0;

export function newDraftKey() {
  draftKeys += 1;
  return `new-${draftKeys}`;
}

export function newSurveyOption(label = "") {
  return { key: newDraftKey(), label };
}

export function newSurveyQuestion(): SurveyQuestionDraft {
  return {
    key: newDraftKey(),
    content: "",
    type: QuestionType.SINGLE_CHOICE,
    options: [newSurveyOption(), newSurveyOption()],
    rows: [newSurveyOption(), newSurveyOption()],
  };
}

type DraftListProps = {
  label: string;
  items: SurveyOptionDraft[];
  mark: LucideIcon;
  /** What one item is called, e.g. "odpowiedź" or "wiersz", for placeholders and labels. */
  noun: string;
  questionNumber: number;
  addLabel: string;
  min: number;
  max: number;
  onChange: (items: SurveyOptionDraft[]) => void;
};

/** An editable list of labels: the answers of a choice, the rows or the columns of a grid. */
function DraftList({
  label,
  items,
  mark: Mark,
  noun,
  questionNumber,
  addLabel,
  min,
  max,
  onChange,
}: DraftListProps) {
  // The item added last takes focus, so a list can be typed without the mouse.
  const [itemToFocus, setItemToFocus] = useState<string | null>(null);
  const canAdd = items.length < max;
  const capitalised = noun.charAt(0).toUpperCase() + noun.slice(1);

  const add = () => {
    if (!canAdd) return;
    const item = newSurveyOption();
    setItemToFocus(item.key);
    onChange([...items, item]);
  };

  return (
    <div className="space-y-2">
      <FieldLabel>{label}</FieldLabel>
      <ul className="space-y-2">
        {items.map((item, index) => (
          <li key={item.key} className="flex items-center gap-2">
            <Mark className="text-muted-foreground size-4 shrink-0" aria-hidden="true" />
            <Input
              value={item.label}
              maxLength={SURVEY_OPTION_MAX}
              placeholder={`${capitalised} ${index + 1}`}
              aria-label={`Pytanie ${questionNumber}, ${noun} ${index + 1}`}
              autoFocus={item.key === itemToFocus}
              onChange={(changeEvent) =>
                onChange(
                  items.map((entry) =>
                    entry.key === item.key ? { ...entry, label: changeEvent.target.value } : entry,
                  ),
                )
              }
              onKeyDown={(keyEvent) => {
                if (keyEvent.key === "Enter") {
                  keyEvent.preventDefault();
                  add();
                }
              }}
            />
            <Button
              type="button"
              variant="ghost"
              size="icon"
              disabled={items.length <= min}
              aria-label={`Usuń: ${noun} ${index + 1} z pytania ${questionNumber}`}
              title="Usuń"
              onClick={() => onChange(items.filter((entry) => entry.key !== item.key))}
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
        disabled={!canAdd}
        onClick={add}
      >
        <Plus data-icon="inline-start" />
        {addLabel}
      </Button>
    </div>
  );
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
  const fieldId = `survey-question-${question.key}`;
  const issueId = `${fieldId}-issue`;
  const grid = question.type === QuestionType.GRID;
  const setOptions = (options: SurveyOptionDraft[]) => onChange({ ...question, options });
  const setRows = (rows: SurveyOptionDraft[]) => onChange({ ...question, rows });

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

      <div className="grid grid-cols-1 gap-5 md:grid-cols-[minmax(0,1fr)_260px]">
        <div className="space-y-2">
          <FieldLabel htmlFor={`${fieldId}-content`}>Treść pytania</FieldLabel>
          <Textarea
            id={`${fieldId}-content`}
            value={question.content}
            rows={2}
            maxLength={SURVEY_QUESTION_MAX}
            placeholder={
              grid
                ? "np. Jak oceniasz poszczególne elementy spotkania?"
                : "np. Jak oceniasz ostatnie spotkanie klubu?"
            }
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

      {grid ? (
        <div className="space-y-4">
          <p className="text-muted-foreground text-sm">
            W każdym wierszu wypełniający wybierze jedną z kolumn, np. wiersze to elementy
            spotkania, a kolumny to oceny.
          </p>
          <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
            <DraftList
              label="Wiersze"
              items={question.rows}
              mark={Minus}
              noun="wiersz"
              questionNumber={number}
              addLabel="Dodaj wiersz"
              min={SURVEY_MIN_ROWS}
              max={SURVEY_MAX_ROWS}
              onChange={setRows}
            />
            <DraftList
              label="Kolumny"
              items={question.options}
              mark={Circle}
              noun="kolumna"
              questionNumber={number}
              addLabel="Dodaj kolumnę"
              min={SURVEY_MIN_OPTIONS}
              max={SURVEY_MAX_OPTIONS}
              onChange={setOptions}
            />
          </div>
        </div>
      ) : isChoiceQuestion(question.type) ? (
        <DraftList
          label="Odpowiedzi do wyboru"
          items={question.options}
          mark={question.type === QuestionType.MULTI_CHOICE ? Square : Circle}
          noun="odpowiedź"
          questionNumber={number}
          addLabel="Dodaj odpowiedź"
          min={SURVEY_MIN_OPTIONS}
          max={SURVEY_MAX_OPTIONS}
          onChange={setOptions}
        />
      ) : (
        <p className="text-muted-foreground bg-muted dark:bg-background rounded-lg px-4 py-3 text-sm">
          Wypełniający wpisze odpowiedź własnymi słowami.
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
