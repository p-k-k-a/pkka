export type AppGuideIcon =
  | "news"
  | "calendar"
  | "users"
  | "user"
  | "folder"
  | "groups"
  | "bell"
  | "shield"
  | "megaphone"
  | "discord"
  | "badge"
  | "hash";

export type AppGuideFeature = {
  icon: AppGuideIcon;
  title: string;
  description: string;
};

export type AppGuideSection = {
  title: string;
  description?: string;
  features: AppGuideFeature[];
};

export const APP_GUIDE_INTRO = {
  title: "Czym jest PKKA?",
  paragraphs: [
    "Platforma Komunikacji Klubu Alumna to aplikacja społecznościowa absolwentów informatyki AGH. Dołączaj do wydarzeń, śledź aktualności klubu, szukaj mentorów i poznawaj innych absolwentów.",
  ],
};

export const APP_GUIDE_STEPS: { title: string; description: string }[] = [
  {
    title: "Załóż konto",
    description: "Zarejestruj się adresem e-mail albo zaloguj kontem Discord.",
  },
  {
    title: "Potwierdź, że jesteś absolwentem",
    description:
      "Wypełnij krótki wniosek o weryfikację. Jeśli masz już rolę absolwenta na serwerze Discord klubu, wystarczy przycisk „Zweryfikuj przez Discord”.",
  },
  {
    title: "Korzystaj z pełnego dostępu",
    description:
      "Wniosek sprawdza administrator klubu. Po akceptacji odblokujesz wszystko, co opisujemy poniżej.",
  },
];

export const APP_GUIDE_SECTIONS: AppGuideSection[] = [
  {
    title: "Dla zalogowanych",
    features: [
      {
        icon: "news",
        title: "Aktualności",
        description: "Publiczne ogłoszenia i wpisy klubu.",
      },
      {
        icon: "calendar",
        title: "Wydarzenia publiczne",
        description:
          "Zapisuj się na otwarte spotkania i warsztaty, dodawaj je do kalendarza i dołączaj do spotkań online.",
      },
      {
        icon: "bell",
        title: "Powiadomienia",
        description:
          "Na telefon i e-mail: o wydarzeniach, na które się zapisujesz, i o decyzji w sprawie Twojego wniosku.",
      },
    ],
  },
  {
    title: "Dla zweryfikowanych absolwentów",
    features: [
      {
        icon: "calendar",
        title: "Wydarzenia dla alumnów",
        description: "Zamknięte spotkania widoczne tylko dla zweryfikowanych absolwentów.",
      },
      {
        icon: "users",
        title: "Katalog absolwentów",
        description:
          "Szukaj ludzi po nazwisku, firmie i umiejętnościach. Sprawdź, kto chętnie zostanie mentorem.",
      },
      {
        icon: "user",
        title: "Twój profil",
        description:
          "Zdjęcie, doświadczenie, umiejętności i kontakty. Sam decydujesz, co widzą inni.",
      },
      {
        icon: "folder",
        title: "Materiały",
        description: "Nagrania i prezentacje z wydarzeń klubu.",
      },
      {
        icon: "groups",
        title: "Grupy tematyczne",
        description: "Dołącz do grupy o temacie, który Cię interesuje, albo zaproponuj nową.",
      },
    ],
  },
  {
    title: "Klub na Discordzie",
    description: "Platforma działa razem z serwerem Discord klubu.",
    features: [
      {
        icon: "discord",
        title: "Logowanie przez Discord",
        description: "Zaloguj się kontem Discord, którego już używasz.",
      },
      {
        icon: "shield",
        title: "Szybka weryfikacja",
        description:
          "Masz już rolę absolwenta na serwerze klubu? Zweryfikujesz się bez wypełniania wniosku.",
      },
      {
        icon: "badge",
        title: "Rola członka klubu",
        description:
          "Gdy Twoje konto jest połączone z Discordem, po weryfikacji dostajesz na serwerze rolę absolwenta.",
      },
      {
        icon: "hash",
        title: "Kanały grup tematycznych",
        description:
          "Każda grupa tematyczna ma prywatny kanał na serwerze. Dostęp dostajesz, gdy dołączasz do grupy.",
      },
    ],
  },
];

export const APP_GUIDE_ADMIN_SECTION: AppGuideSection = {
  title: "Dla administratorów",
  description: "Panel administratora jest dostępny w wersji webowej.",
  features: [
    {
      icon: "shield",
      title: "Wnioski o weryfikację",
      description: "Przeglądaj wnioski, akceptuj je albo odrzucaj z podaniem powodu.",
    },
    {
      icon: "megaphone",
      title: "Aktualności",
      description: "Pisz i publikuj ogłoszenia.",
    },
    {
      icon: "calendar",
      title: "Wydarzenia",
      description: "Twórz wydarzenia, sprawdzaj listę zapisanych i wysyłaj im wiadomości.",
    },
    {
      icon: "folder",
      title: "Materiały",
      description: "Dodawaj nagrania i prezentacje z wydarzeń.",
    },
    {
      icon: "users",
      title: "Alumni",
      description: "Przeglądaj profile członków klubu.",
    },
    {
      icon: "groups",
      title: "Grupy tematyczne",
      description: "Rozpatruj propozycje nowych grup.",
    },
  ],
};
