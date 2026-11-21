"use client";

import type { MaterialType } from "@pkka/api";
import { MATERIAL_TYPE_OPTIONS, isMaterialType } from "@pkka/domain";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";

const ALL = "ALL";

type MaterialTypeTabsProps = {
  value: MaterialType | null;
  onChange: (type: MaterialType | null) => void;
};

export function MaterialTypeTabs({ value, onChange }: MaterialTypeTabsProps) {
  return (
    <Tabs
      value={value ?? ALL}
      onValueChange={(next) => onChange(isMaterialType(next) ? next : null)}
    >
      <TabsList>
        <TabsTrigger value={ALL}>Wszystkie</TabsTrigger>
        {MATERIAL_TYPE_OPTIONS.map((option) => (
          <TabsTrigger key={option.value} value={option.value}>
            {option.label}
          </TabsTrigger>
        ))}
      </TabsList>
    </Tabs>
  );
}
