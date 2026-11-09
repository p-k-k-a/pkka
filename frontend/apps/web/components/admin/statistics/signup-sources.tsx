import { InfoTip } from "@/components/admin/statistics/info-tip";
import { Card } from "@/components/ui/card";

// TODO: replace with real per-channel counts once sign-ups record where they came from.
const MOCK_SHARES = [
  { label: "Discord", share: 0.45 },
  { label: "E-mail", share: 0.3 },
  { label: "Strona WWW", share: 0.17 },
  { label: "Aplikacja mobilna", share: 0.08 },
];

function splitByShares(total: number) {
  const counts = MOCK_SHARES.map((source) => Math.floor(source.share * total));
  counts[0] += total - counts.reduce((sum, count) => sum + count, 0);
  return counts;
}

export function SignupSources({
  signUps,
  layout = "stack",
}: {
  signUps: number;
  layout?: "stack" | "row";
}) {
  const counts = splitByShares(signUps);

  return (
    <Card className="gap-4 px-6 py-6">
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-2">
          <h2 className="font-heading text-foreground text-[23px] font-semibold tracking-tight">
            Źródła zapisów
          </h2>
          <InfoTip label="Źródła zapisów">
            Kanał, z którego przyszedł zapis. Na razie dane przykładowe.
          </InfoTip>
        </div>
        <span className="bg-accent/10 text-accent shrink-0 rounded-lg px-2 py-1 text-[11px] font-semibold tracking-widest uppercase">
          Przykładowe dane
        </span>
      </div>
      <ul
        className={
          layout === "row"
            ? "grid grid-cols-1 gap-x-8 gap-y-4 sm:grid-cols-2 lg:grid-cols-4"
            : "space-y-4"
        }
      >
        {MOCK_SHARES.map((source, index) => {
          const percent = signUps === 0 ? 0 : Math.round((counts[index] / signUps) * 100);
          return (
            <li key={source.label} className="space-y-2">
              <div className="flex items-baseline justify-between gap-3 text-sm">
                <span className="text-foreground">{source.label}</span>
                <span className="text-foreground font-semibold tabular-nums">
                  {counts[index]}{" "}
                  <span className="text-muted-foreground font-normal">({percent}%)</span>
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
