import { Text } from "@/components/ui/text";
import { cn } from "@/lib/utils";
import * as React from "react";
import { View } from "react-native";

type SectionHeadingProps = {
  title: string;
  className?: string;
  centered?: boolean;
};

function SectionHeading({ title, className, centered = false }: SectionHeadingProps) {
  return (
    <View className={cn("gap-1", className)}>
      <Text
        role="heading"
        aria-level="2"
        className={cn(
          "font-heading text-foreground text-[28px] font-semibold leading-tight tracking-tight",
          centered && "text-center",
        )}
      >
        {title}
      </Text>
    </View>
  );
}

export { SectionHeading };
