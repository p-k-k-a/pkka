/**
 * Class-string recipes shared by both apps. Restricted to the Tailwind v3/v4
 * portable subset — web is on v4, mobile on v3 + NativeWind.
 *
 * Any file here must be listed in `apps/mobile/tailwind.config.js` `content`
 * and covered by the `@source` directive in `apps/web/app/globals.css`, otherwise
 * classes used only here are never generated.
 */

/** Small uppercase kicker above a heading or beside a value. */
export const EYEBROW = "text-xs font-semibold uppercase tracking-widest text-muted-foreground";

/** "Czytaj więcej" / "Szczegóły" style call-to-action label. */
export const CTA_LABEL = "text-xs font-bold uppercase tracking-widest text-foreground";

/** The tighter 10px kicker used inside dense cards. */
export const EYEBROW_SM =
  "text-eyebrow font-semibold uppercase tracking-widest text-muted-foreground";

/** Page content cap used by the public web layout. */
export const CONTENT_WIDTH = "max-w-[1280px]";
