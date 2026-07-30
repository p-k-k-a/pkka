import { Text } from "@/components/ui/text";
import { cn } from "@/lib/utils";
import { useTheme } from "@react-navigation/native";
import { ArrowLeft } from "lucide-react-native";
import { Pressable, View } from "react-native";

type DetailHeaderProps = {
  title: string;
  onBack?: () => void;
  className?: string;
};

function DetailHeader({ title, onBack, className }: DetailHeaderProps) {
  const { colors } = useTheme();

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
        className="size-10 items-center justify-center rounded-md active:bg-muted"
      >
        <ArrowLeft size={22} color={colors.text} />
      </Pressable>
      <Text className="flex-1 text-center text-base font-bold uppercase tracking-widest text-foreground">
        {title}
      </Text>
      <View className="size-10" />
    </View>
  );
}

export { DetailHeader };
