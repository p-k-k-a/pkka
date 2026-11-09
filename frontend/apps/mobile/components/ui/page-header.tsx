import { Text } from "@/components/ui/text";
import { cn } from "@/lib/utils";
import * as React from "react";
import { View } from "react-native";

type EyebrowProps = {
  children: string;
  onCard?: boolean;
  className?: string;
};

function Eyebrow({ children, onCard = false, className }: EyebrowProps) {
  return (
    <View
      className={cn(
        "self-start rounded-lg px-3 py-1",
        onCard ? "bg-accent/10 dark:bg-background" : "bg-background",
        className,
      )}
    >
      <Text className="text-accent text-xs font-semibold uppercase tracking-widest">
        {children}
      </Text>
    </View>
  );
}

type PageHeaderProps = {
  eyebrow?: string;
  title: string;
  description?: string;
  className?: string;
  children?: React.ReactNode;
};

function PageHeader({ eyebrow, title, description, className, children }: PageHeaderProps) {
  return (
    <View className={cn("bg-muted gap-5 px-5 pb-10 pt-8", className)}>
      {eyebrow ? <Eyebrow>{eyebrow}</Eyebrow> : null}
      <Text
        role="heading"
        aria-level="1"
        className="font-heading text-foreground text-[33px] font-semibold leading-tight tracking-tight"
      >
        {title}
      </Text>
      {description ? (
        <Text className="text-muted-foreground text-base leading-relaxed">{description}</Text>
      ) : null}
      {children}
    </View>
  );
}

export { Eyebrow, PageHeader };
