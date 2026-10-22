import Link from "next/link";
import { CLUB_CONTACT_EMAIL, PRIVACY_PATH } from "@pkka/domain";

/** The short first layer of the privacy notice, shown wherever personal data is collected. */
export function PrivacyNoticeSummary() {
  return (
    <div className="bg-muted text-muted-foreground space-y-2 rounded-lg px-4 py-3 text-[13px] leading-relaxed">
      <p>
        Administratorem Twoich danych osobowych jest Akademia Górniczo-Hutnicza im. Stanisława
        Staszica w Krakowie, al. A. Mickiewicza 30, 30-059 Kraków. Kontakt z Inspektorem Ochrony
        Danych: <a href="mailto:iodo@agh.edu.pl">iodo@agh.edu.pl</a>.
      </p>
      <p>
        Każdą zgodę możesz wycofać w dowolnym momencie: zgodę na newsletter w ustawieniach profilu,
        a pozostałe pisząc na <a href={`mailto:${CLUB_CONTACT_EMAIL}`}>{CLUB_CONTACT_EMAIL}</a>.
        Wycofanie nie wpływa na zgodność z prawem przetwarzania przed jego dokonaniem.{" "}
        <Link
          href={PRIVACY_PATH}
          target="_blank"
          rel="noopener noreferrer"
          className="text-accent font-semibold underline"
        >
          Pełna informacja
        </Link>
      </p>
    </div>
  );
}
