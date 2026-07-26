import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

type SelectOption = { value: string; label: string };

type SelectFieldProps = {
  value: string | null;
  options: SelectOption[];
  placeholder: string;
  onChange: (value: string) => void;
};

function SelectField({ value, options, placeholder, onChange }: SelectFieldProps) {
  const selected = options.find((option) => option.value === value);

  return (
    <Select
      value={selected ? { value: selected.value, label: selected.label } : undefined}
      onValueChange={(option) => {
        if (option) onChange(option.value);
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
export type { SelectOption };
