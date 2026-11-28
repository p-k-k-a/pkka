import { Button } from "@/components/ui/button";
import { Text } from "@/components/ui/text";
import * as React from "react";
import { View } from "react-native";

type CtaCardProps = {
  title: string;
  subtitle: string;
  primaryLabel: string;
  onPrimary?: () => void;
};

function CtaCard({ title, subtitle, primaryLabel, onPrimary }: CtaCardProps) {
  return (
    <View className="bg-band dark:border-border rounded-2xl p-7 dark:border dark:bg-card">
      <Text
        role="heading"
        aria-level="2"
        className="font-heading text-band-foreground text-[28px] font-semibold leading-tight tracking-tight"
      >
        {title}
      </Text>
      <Text className="text-band-foreground/80 mt-3 text-base leading-relaxed">{subtitle}</Text>
      <Button size="lg" className="mt-7 w-full" onPress={onPrimary}>
        <Text className="text-base font-semibold">{primaryLabel}</Text>
      </Button>
    </View>
  );
}

export { CtaCard };
