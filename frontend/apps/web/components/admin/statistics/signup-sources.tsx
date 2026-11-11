import type { SignUpSourceResponse } from "@pkka/api";
import { signUpSourceLabel } from "@pkka/domain";
import { InfoTip } from "@/components/admin/statistics/info-tip";
import { Card } from "@/components/ui/card";

export function SignUpSources({
  sources,
  layout = "stack",
}: {
  sources: SignUpSourceResponse[];
  layout?: "stack" | "row";
}) {
  const total = sources.reduce((sum, source) => sum + source.signUps, 0);

  return (
    <Card className="gap-4 px-6 py-6">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-2">
          <h2 className="font-heading text-foreground text-[23px] font-semibold tracking-tight">
            Źródła zapisów
          </h2>
          <InfoTip label="Źródła zapisów">
            Kanał, z którego przyszedł zapis: link z Discorda lub e-maila, strona WWW albo aplikacja
            mobilna. Liczy każdy zapis, także na listę rezerwową.
          </InfoTip>
        </div>
      </div>
      <ul
        className={
          layout === "row"
            ? "grid grid-cols-1 gap-x-8 gap-y-4 sm:grid-cols-2 lg:grid-cols-4"
            : "space-y-4"
        }
      >
        {sources.map(({ source, signUps }) => {
          const percent = total === 0 ? 0 : Math.round((signUps / total) * 100);
          return (
            <li key={source} className="space-y-2">
              <div className="flex items-baseline justify-between gap-3 text-sm">
                <span className="text-foreground">{signUpSourceLabel(source)}</span>
                <span className="text-foreground font-semibold tabular-nums">
                  {signUps} <span className="text-muted-foreground font-normal">({percent}%)</span>
                </span>
              </div>
              <div className="bg-chart-1/15 h-2 w-full overflow-hidden rounded-full" aria-hidden>
                <div className="bg-chart-1 h-full rounded-full" style={{ width: `${percent}%` }} />
              </div>
            </li>
          );
        })}
      </ul>
    </Card>
  );
}
