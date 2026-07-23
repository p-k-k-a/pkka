import { cn } from "@/lib/utils";
import { Platform, TextInput } from "react-native";

function Input({
  className,
  ...props
}: React.ComponentProps<typeof TextInput> & React.RefAttributes<TextInput>) {
  return (
    <TextInput
      className={cn(
        "border-input bg-background text-foreground placeholder:text-muted-foreground h-12 w-full rounded-md border px-3 text-base",
        Platform.select({
          web: "focus-visible:border-ring focus-visible:ring-ring/50 outline-none transition-all focus-visible:ring-[3px]",
        }),
        props.editable === false && "opacity-50",
        className,
      )}
      {...props}
    />
  );
}

export { Input };
