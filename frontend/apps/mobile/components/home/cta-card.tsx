import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Text } from "@/components/ui/text";
import * as React from "react";
import { Pressable } from "react-native";

type CtaCardProps = {
  title: string;
  subtitle: string;
  primaryLabel: string;
  secondaryLabel?: string;
  onPrimary?: () => void;
  onSecondary?: () => void;
};

function CtaCard({
  title,
  subtitle,
  primaryLabel,
  secondaryLabel,
  onPrimary,
  onSecondary,
}: CtaCardProps) {
  return (
    <Card className="bg-foreground border-0 p-8 gap-0">
      <Text
        variant="h2"
        className="text-background text-center border-0 pb-0 text-2xl font-bold tracking-tight"
      >
        {title}
      </Text>
      <Text className="text-background/60 text-center text-sm leading-5 mt-3">{subtitle}</Text>
      <Button variant="secondary" className="mt-6 w-full" onPress={onPrimary}>
        <Text className="text-foreground text-xs font-bold tracking-widest uppercase">
          {primaryLabel}
        </Text>
      </Button>
      {secondaryLabel ? (
        <Pressable className="mt-4 items-center" onPress={onSecondary}>
          <Text className="text-background/60 text-xs font-semibold tracking-widest uppercase">
            {secondaryLabel}
          </Text>
        </Pressable>
      ) : null}
    </Card>
  );
}

export { CtaCard };
