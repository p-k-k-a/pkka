"use client";

import { useState } from "react";
import { QuestionType, type QuestionResult } from "@pkka/api";
import { formatAnswerCount, questionTypeLabel, submissionShare } from "@pkka/domain";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";

const TEXT_ANSWERS_SHOWN = 5;

type SurveyQuestionResultProps = {
  result: QuestionResult;
  number: number;
  /** People who filled in the survey; each of them answered every question. */
  submissionCount: number;
};

export function SurveyQuestionResult({
  result,
  number,
  submissionCount,
}: SurveyQuestionResultProps) {
  const [showAllText, setShowAllText] = useState(false);
  const textAnswers = result.textAnswers ?? [];
  const shownTextAnswers = showAllText ? textAnswers : textAnswers.slice(0, TEXT_ANSWERS_SHOWN);

  return (
    <Card className="gap-5 px-6 py-6">
      <div className="space-y-2">
        <p className="text-muted-foreground text-xs font-semibold tracking-widest uppercase">
          Pytanie {number} · {questionTypeLabel(result.type)}
        </p>
        <h3 className="font-heading text-foreground text-lg leading-snug font-semibold break-words">
          {result.content}
        </h3>
      </div>

      {result.type === QuestionType.TEXT ? (
        textAnswers.length === 0 ? (
          <p className="text-muted-foreground text-sm">Brak odpowiedzi.</p>
        ) : (
          <div className="space-y-3">
            <ul className="space-y-2">
              {shownTextAnswers.map((answer, index) => (
                <li
                  key={index}
                  className="bg-muted dark:bg-background text-foreground rounded-lg px-4 py-3 text-sm break-words whitespace-pre-line"
                >
                  {answer}
                </li>
              ))}
            </ul>
            {textAnswers.length > TEXT_ANSWERS_SHOWN ? (
              <Button
                type="button"
                variant="ghost"
                size="sm"
                className="-ml-2"
                aria-expanded={showAllText}
                onClick={() => setShowAllText((prev) => !prev)}
              >
                {showAllText
                  ? "Pokaż mniej"
                  : `Pokaż wszystkie (${formatAnswerCount(textAnswers.length)})`}
              </Button>
            ) : null}
          </div>
        )
      ) : (
        <div className="space-y-4">
          <ul className="space-y-4">
            {(result.optionCounts ?? []).map((option) => {
              const percent = submissionShare(option.count, submissionCount);
              return (
                <li key={option.optionId} className="space-y-2">
                  <div className="flex items-baseline justify-between gap-3 text-sm">
                    <span className="text-foreground min-w-0 break-words">{option.label}</span>
                    <span className="text-foreground shrink-0 font-semibold tabular-nums">
                      {option.count}{" "}
                      <span className="text-muted-foreground font-normal">({percent}%)</span>
                    </span>
                  </div>
                  <div
                    className="bg-chart-1/15 h-2 w-full overflow-hidden rounded-full"
                    aria-hidden
                  >
                    <div
                      className="bg-chart-1 h-full rounded-full"
                      style={{ width: `${percent}%` }}
                    />
                  </div>
                </li>
              );
            })}
          </ul>
          {result.type === QuestionType.MULTI_CHOICE ? (
            <p className="text-muted-foreground text-xs">
              Można było zaznaczyć kilka odpowiedzi, więc procenty nie sumują się do 100.
            </p>
          ) : null}
        </div>
      )}
    </Card>
  );
}
