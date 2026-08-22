import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";

type SwitchRowProps = {
  id: string;
  label: string;
  description?: string;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  disabled?: boolean;
};

export function SwitchRow({
  id,
  label,
  description,
  checked,
  onCheckedChange,
  disabled,
}: SwitchRowProps) {
  return (
    <div className="border-border flex items-start justify-between gap-4 rounded-lg border p-4">
      <div className="flex flex-col gap-1">
        <Label htmlFor={id} className="text-foreground text-sm font-semibold">
          {label}
        </Label>
        {description ? (
          <p className="text-muted-foreground text-[13px] leading-relaxed">{description}</p>
        ) : null}
      </div>
      <Switch
        id={id}
        checked={checked}
        onCheckedChange={onCheckedChange}
        disabled={disabled}
        className="mt-1"
      />
    </div>
  );
}
