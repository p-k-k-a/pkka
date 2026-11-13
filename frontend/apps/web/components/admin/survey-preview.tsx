"use client";

import { useState } from "react";
import { isChoiceQuestion, type SurveyQuestionDraft } from "@pkka/domain";
import { SurveyQuestionField } from "@/components/surveys/survey-question-field";
import { Button } from "@/components/ui/button";

type SurveyPreviewProps = {
  title: string;
  description: string;
  questions: SurveyQuestionDraft[];
};

/** The survey the way an alumn will fill it in. Answers given here are never sent. */
export function SurveyPreview({ title, description, questions }: SurveyPreviewProps) {
  const [answers, setAnswers] = useState<Record<string, string>>({});

  return (
    <div className="space-y-10">
      <p className="bg-muted text-muted-foreground rounded-lg px-4 py-3 text-sm">
        Tak ankietę zobaczy alumn. Możesz ją przeklikać, odpowiedzi z podglądu nie są zapisywane.
      </p>
      <div className="space-y-4">
        <h2 className="font-heading text-foreground text-[23px] font-semibold tracking-tight break-words md:text-[28px]">
          {title.trim() || "Ankieta bez tytułu"}
        </h2>
        {description.trim() ? (
          <p className="text-muted-foreground max-w-2xl text-base leading-relaxed break-words whitespace-pre-line">
            {description.trim()}
          </p>
        ) : null}
      </div>
      <div className="max-w-2xl space-y-10">
        {questions.map((question, index) => (
          <SurveyQuestionField
            key={question.key}
            number={index + 1}
            question={{
              id: question.key,
              content: question.content.trim(),
              type: question.type,
              options: isChoiceQuestion(question.type)
                ? question.options
                    .filter((option) => option.label.trim() !== "")
                    .map((option) => ({ id: option.key, label: option.label.trim() }))
                : [],
            }}
            value={answers[question.key] ?? ""}
            onChange={(value) => setAnswers((prev) => ({ ...prev, [question.key]: value }))}
          />
        ))}
      </div>
      <Button type="button" size="xl" disabled>
        Wyślij odpowiedzi
      </Button>
    </div>
  );
}
