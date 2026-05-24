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
      <Text variant="h1" className={cn("text-left text-4xl", centered && "text-center")}>
        {title}
      </Text>
    </View>
  );
}

export { SectionHeading };
