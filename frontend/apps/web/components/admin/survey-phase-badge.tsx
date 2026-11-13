import { surveyPhaseLabel, type SurveyPhase } from "@pkka/domain";
import { cn } from "@/lib/utils";

const PHASE_CLASSES: Record<SurveyPhase, string> = {
  draft: "bg-accent/10 text-accent",
  active: "bg-band text-band-foreground",
  ended: "bg-muted-foreground/15 text-muted-foreground",
};

export function SurveyPhaseBadge({ phase }: { phase: SurveyPhase }) {
  return (
    <span
      className={cn(
        "inline-flex w-fit rounded-lg px-3 py-1 text-[11px] font-semibold tracking-widest uppercase",
        PHASE_CLASSES[phase],
      )}
    >
      {surveyPhaseLabel(phase)}
    </span>
  );
}
