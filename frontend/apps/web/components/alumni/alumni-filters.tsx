"use client";

import { useListUserTags } from "@pkka/api";
import {
  ALUMNI_YEAR_MAX,
  ALUMNI_YEAR_MIN,
  EMPTY_ALUMNI_FILTERS,
  countActiveAlumniFilters,
  type AlumniFilters,
} from "@pkka/domain";
import { TagPicker } from "@/components/profile/tag-picker";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";

const YEARS = Array.from(
  { length: ALUMNI_YEAR_MAX - ALUMNI_YEAR_MIN + 1 },
  (_, i) => ALUMNI_YEAR_MAX - i,
);

function FilterHeading({ htmlFor, children }: { htmlFor?: string; children: React.ReactNode }) {
  return (
    <Label
      htmlFor={htmlFor}
      className="text-muted-foreground text-[11px] font-bold tracking-widest uppercase"
    >
      {children}
    </Label>
  );
}

type AlumniFiltersPanelProps = {
  value: AlumniFilters;
  onChange: (filters: AlumniFilters) => void;
};

export function AlumniFiltersPanel({ value, onChange }: AlumniFiltersPanelProps) {
  const tagsQuery = useListUserTags();
  const [fromYear, toYear] = value.yearRange;
  const activeCount = countActiveAlumniFilters(value);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-2">
        <FilterHeading htmlFor="alumni-year-from">Rok ukończenia</FilterHeading>
        <div className="grid grid-cols-2 gap-2">
          <Select
            id="alumni-year-from"
            aria-label="Rok ukończenia od"
            value={fromYear}
            onChange={(event) =>
              onChange({ ...value, yearRange: [Number(event.target.value), toYear] })
            }
          >
            {YEARS.filter((year) => year <= toYear).map((year) => (
              <option key={year} value={year}>
                {year === ALUMNI_YEAR_MIN ? `od ${year}` : year}
              </option>
            ))}
          </Select>
          <Select
            aria-label="Rok ukończenia do"
            value={toYear}
            onChange={(event) =>
              onChange({ ...value, yearRange: [fromYear, Number(event.target.value)] })
            }
          >
            {YEARS.filter((year) => year >= fromYear).map((year) => (
              <option key={year} value={year}>
                {year === ALUMNI_YEAR_MAX ? `do ${year}` : year}
              </option>
            ))}
          </Select>
        </div>
      </div>

      <div className="flex flex-col gap-2">
        <FilterHeading>Umiejętności</FilterHeading>
        {tagsQuery.isPending ? (
          <p className="text-muted-foreground text-sm">Ładowanie umiejętności…</p>
        ) : tagsQuery.isError ? (
          <p className="text-destructive text-sm font-medium">
            Nie udało się załadować listy umiejętności.
          </p>
        ) : (
          <TagPicker
            availableTags={tagsQuery.data?.data ?? []}
            selectedIds={value.tagIds}
            onChange={(tagIds) => onChange({ ...value, tagIds })}
            placeholder="Wyszukaj umiejętności…"
          />
        )}
      </div>

      <div className="flex flex-col gap-2">
        <FilterHeading>Mentoring</FilterHeading>
        <div className="flex items-center gap-3">
          <Checkbox
            id="alumni-mentor-only"
            checked={value.mentorOnly}
            onCheckedChange={(checked) => onChange({ ...value, mentorOnly: checked === true })}
          />
          <Label htmlFor="alumni-mentor-only" className="text-foreground text-sm font-normal">
            Tylko chętni do mentoringu
          </Label>
        </div>
      </div>

      {activeCount > 0 ? (
        <Button
          type="button"
          variant="ghost"
          className="self-start px-0"
          onClick={() => onChange(EMPTY_ALUMNI_FILTERS)}
        >
          Wyczyść filtry ({activeCount})
        </Button>
      ) : null}
    </div>
  );
}
