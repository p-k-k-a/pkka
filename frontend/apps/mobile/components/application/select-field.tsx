import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { LabelOption } from "@pkka/domain";

type SelectFieldProps<T extends string> = {
  value: T | null;
  options: readonly LabelOption<T>[];
  placeholder: string;
  onChange: (value: T) => void;
};

function SelectField<T extends string>({
  value,
  options,
  placeholder,
  onChange,
}: SelectFieldProps<T>) {
  const selected = options.find((option) => option.value === value);

  return (
    <Select
      value={selected ? { value: selected.value, label: selected.label } : undefined}
      onValueChange={(option) => {
        if (option) onChange(option.value as T);
      }}
    >
      <SelectTrigger className="h-12">
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        {options.map((option) => (
          <SelectItem key={option.value} label={option.label} value={option.value}>
            {option.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}

export { SelectField };
