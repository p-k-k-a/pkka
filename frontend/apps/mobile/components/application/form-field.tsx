import { Label } from "@/components/ui/label";
import { Text } from "@/components/ui/text";
import { cn } from "@/lib/utils";
import { View } from "react-native";

type FormFieldProps = {
  label: string;
  required?: boolean;
  className?: string;
  children: React.ReactNode;
};

function FormField({ label, required = false, className, children }: FormFieldProps) {
  return (
    <View className={cn("gap-2", className)}>
      <Label className="text-foreground text-xs font-bold uppercase tracking-wider">
        {label}
        {required ? <Text className="text-destructive text-xs font-bold">*</Text> : ""}
      </Label>
      {children}
    </View>
  );
}

export { FormField };
