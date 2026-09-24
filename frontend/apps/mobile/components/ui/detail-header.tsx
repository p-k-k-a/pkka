import { Text } from "@/components/ui/text";
import { cn } from "@/lib/utils";
import { useThemeColors } from "@/lib/theme";
import { ArrowLeft } from "lucide-react-native";
import { Pressable, View } from "react-native";

type DetailHeaderProps = {
  title: string;
  onBack?: () => void;
  className?: string;
};

function DetailHeader({ title, onBack, className }: DetailHeaderProps) {
  const theme = useThemeColors();

  return (
    <View
      className={cn(
        "flex-row items-center border-b border-border bg-background px-4 py-3",
        className,
      )}
    >
      <Pressable
        onPress={onBack}
        hitSlop={8}
        role="button"
        accessibilityLabel="Wróć"
        className="size-10 items-center justify-center rounded-full active:bg-muted"
      >
        <ArrowLeft size={22} color={theme.foreground} />
      </Pressable>
      <Text className="flex-1 text-center text-sm font-bold uppercase tracking-widest text-foreground">
        {title}
      </Text>
      <View className="size-10" />
    </View>
  );
}

export { DetailHeader };
