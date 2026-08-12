import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";

type VisibilityFieldProps = {
  id: string;
  label: string;
  value?: string;
  /** Shown instead of the value when the underlying account data is missing. */
  missingLabel?: string;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  disabled?: boolean;
};

export function VisibilityField({
  id,
  label,
  value,
  missingLabel = "Brak danych",
  checked,
  onCheckedChange,
  disabled,
}: VisibilityFieldProps) {
  const hasValue = Boolean(value);
  const toggle = (
    <div className="flex items-center gap-2">
      <Label htmlFor={id} className="text-muted-foreground text-[13px]">
        {checked ? "Widoczne" : "Ukryte"}
      </Label>
      <Switch
        id={id}
        // The visible label only reads "Widoczne"/"Ukryte", so the field name
        // has to come from here for screen readers.
        aria-label={`${label} — widoczne dla alumnów`}
        checked={checked}
        onCheckedChange={onCheckedChange}
        disabled={disabled || !hasValue}
      />
    </div>
  );

  return (
    <div className="border-border flex flex-col gap-2 border-b py-4 first:pt-0 last:border-b-0 last:pb-0 sm:flex-row sm:items-center sm:justify-between">
      <div className="flex min-w-0 flex-col gap-0.5">
        <span className="text-muted-foreground text-[11px] font-bold tracking-widest uppercase">
          {label}
        </span>
        <span className="text-foreground truncate text-sm font-medium">
          {hasValue ? value : <span className="text-muted-foreground">{missingLabel}</span>}
        </span>
      </div>

      {hasValue ? (
        toggle
      ) : (
        <Tooltip>
          {/* A disabled switch fires no events, so the wrapper carries the
              tooltip and stays keyboard reachable. */}
          <TooltipTrigger asChild>
            <span tabIndex={0}>{toggle}</span>
          </TooltipTrigger>
          <TooltipContent>Najpierw uzupełnij te dane na swoim koncie.</TooltipContent>
        </Tooltip>
      )}
    </div>
  );
}
