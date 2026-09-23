import { PageHeader } from "@/components/ui/page-header";
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
    <PageHeader eyebrow="Wydział Informatyki AGH" title={title} description={body}>
      <View className="border-border bg-background mt-2 aspect-video w-full overflow-hidden rounded-2xl border">
        <Image source={alumniImage} contentFit="cover" style={{ width: "100%", height: "100%" }} />
      </View>
    </PageHeader>
  );
}

export { Hero };
