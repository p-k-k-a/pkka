import { SectionHeading } from "@/components/home/section-heading";
import { Text } from "@/components/ui/text";
import { Image } from "expo-image";
import * as React from "react";
import { View } from "react-native";

type HeroProps = {
  title: string;
  body: string;
};

const alumniImage = require("@/assets/images/klub_alumna_logo_mobile.png");

function Hero({ title, body }: HeroProps) {
  return (
    <View className="gap-5">
      <SectionHeading title={title} />
      <View className="w-full aspect-video rounded-2xl bg-muted overflow-hidden">
        <Image source={alumniImage} contentFit="cover" style={{ width: "100%", height: "100%" }} />
      </View>
      <Text variant="p" className="text-muted-foreground leading-6 mt-0">
        {body}
      </Text>
    </View>
  );
}

export { Hero };
