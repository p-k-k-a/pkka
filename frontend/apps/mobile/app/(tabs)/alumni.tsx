import { AlumniDirectory } from "@/components/alumni-directory/alumni-directory";
import { RequireAlumni } from "@/components/auth/require-alumni";
import { TopicGroups } from "@/components/topic-groups/topic-groups";
import { UnderlineTabs } from "@/components/ui/underline-tabs";
import { cn } from "@/lib/utils";
import { useState } from "react";
import { View } from "react-native";

const SECTIONS = [
  { value: "directory", label: "Katalog" },
  { value: "groups", label: "Grupy" },
] as const;

type Section = (typeof SECTIONS)[number]["value"];

export default function AlumniScreen() {
  const [section, setSection] = useState<Section>("directory");

  return (
    <RequireAlumni>
      <View className="bg-background flex-1">
        <UnderlineTabs
          options={SECTIONS}
          value={section}
          onChange={setSection}
          className="px-5 pt-4"
        />
        <View className={cn("flex-1", section !== "directory" && "hidden")}>
          <AlumniDirectory />
        </View>
        <View className={cn("flex-1", section !== "groups" && "hidden")}>
          <TopicGroups />
        </View>
      </View>
    </RequireAlumni>
  );
}
