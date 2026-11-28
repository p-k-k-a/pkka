import { ALUMNI_ONLY_POST_LABEL } from "@pkka/domain";

export function AlumniOnlyBadge() {
  return (
    <span className="bg-band text-band-foreground inline-flex w-fit rounded-lg px-3 py-1 text-[11px] font-semibold tracking-widest uppercase">
      {ALUMNI_ONLY_POST_LABEL}
    </span>
  );
}
