-- Version 2: the first full texts for the app. They are drafts awaiting approval by the AGH Data
-- Protection Officer, and say so at the top. Everyone who consented to version 1 must accept the
-- terms, the notice and the membership consent again; the newsletter consent stays valid.
INSERT INTO legal_documents (id, type, version, title, content, change_summary, requires_reacceptance, published_at)
VALUES
(gen_random_uuid(), 'TERMS', 2, 'Regulamin Klubu Alumnów Wydziału Informatyki AGH',
$md$> **PROJEKT** — treść do akceptacji przez Klub i Inspektora Ochrony Danych AGH.

## § 1. Postanowienia ogólne

1. Regulamin określa zasady członkostwa w Klubie Alumnów Wydziału Informatyki AGH (dalej: Klub) oraz korzystania z aplikacji Klubu w wersji webowej i mobilnej (dalej: Aplikacja).
2. Klub jest inicjatywą Wydziału Informatyki Akademii Górniczo-Hutniczej im. Stanisława Staszica w Krakowie, al. A. Mickiewicza 30, 30-059 Kraków (dalej: AGH).
3. Kontakt z Klubem: alumni@informatyka.agh.edu.pl.

## § 2. Członkostwo

1. Członkiem Klubu może zostać absolwent studiów prowadzonych przez Wydział Informatyki AGH, w tym studiów doktoranckich i podyplomowych.
2. Aby zostać członkiem, należy założyć konto w Aplikacji i złożyć wniosek. Wniosek weryfikuje administrator Klubu.
3. Do złożenia wniosku wymagane są: akceptacja Regulaminu, potwierdzenie zapoznania się z Informacją o przetwarzaniu danych osobowych oraz zgoda na przetwarzanie danych osobowych w celu członkostwa w Klubie.
4. Administrator może odrzucić wniosek, w szczególności gdy podane dane nie potwierdzają statusu absolwenta.

## § 3. Konto i profil

1. Użytkownik odpowiada za aktualność danych w swoim profilu.
2. Profil członka jest widoczny dla innych zweryfikowanych członków Klubu. Użytkownik decyduje w ustawieniach profilu, czy pokazać imię i nazwisko, adres e-mail oraz konto Discord.
3. Zakazane jest udostępnianie konta innym osobom oraz publikowanie treści bezprawnych, obraźliwych lub reklamowych niezwiązanych z działalnością Klubu.

## § 4. Wydarzenia

1. Na wydarzenia zapisuje się w Aplikacji. Gdy brak wolnych miejsc, użytkownik trafia na listę rezerwową i otrzymuje miejsce, gdy ktoś zrezygnuje.
2. Z wydarzenia można się wypisać do chwili jego rozpoczęcia.
3. Link do wydarzenia online udostępniany jest wyłącznie osobom z potwierdzonym miejscem.

## § 5. Materiały

Materiały udostępniane członkom służą wyłącznie do użytku własnego i nie mogą być rozpowszechniane bez zgody ich autorów.

## § 6. Zakończenie członkostwa

1. Członek może w każdej chwili wystąpić z Klubu, wysyłając wiadomość na adres alumni@informatyka.agh.edu.pl.
2. Administrator może zakończyć członkostwo osoby, która rażąco narusza Regulamin.

## § 7. Zmiany Regulaminu

1. Każda zmiana Regulaminu publikowana jest jako nowa wersja z datą wejścia w życie.
2. Jeżeli zmiana wymaga ponownej akceptacji, Aplikacja poprosi o nią przy najbliższym logowaniu. Do czasu akceptacji korzystanie z Aplikacji jest wstrzymane.
$md$,
'Pierwsza pełna wersja Regulaminu w aplikacji Klubu.', TRUE, NOW()),

(gen_random_uuid(), 'PRIVACY_NOTICE', 2, 'Informacja o przetwarzaniu danych osobowych',
$md$> **PROJEKT** — treść do akceptacji przez Inspektora Ochrony Danych AGH.

Realizując obowiązek informacyjny z art. 13 ust. 1 i 2 Rozporządzenia Parlamentu Europejskiego i Rady (UE) 2016/679 (RODO), informujemy, że:

1. **Administrator.** Administratorem Pani/Pana danych osobowych jest Akademia Górniczo-Hutnicza im. Stanisława Staszica w Krakowie, al. A. Mickiewicza 30, 30-059 Kraków (dalej: AGH).
2. **Inspektor Ochrony Danych.** Kontakt z Inspektorem Ochrony Danych: iodo@agh.edu.pl, tel. 12 617 53 25.
3. **Cele i podstawy przetwarzania.**
   - członkostwo w Klubie Alumnów Wydziału Informatyki AGH i korzystanie z aplikacji Klubu (weryfikacja wniosku, profil, katalog członków, wydarzenia, materiały, ankiety, powiadomienia) — na podstawie Pani/Pana zgody (art. 6 ust. 1 lit. a RODO);
   - wysyłka newslettera Klubu drogą elektroniczną — na podstawie odrębnej, dobrowolnej zgody (art. 6 ust. 1 lit. a RODO);
   - wykazanie, że zgoda została udzielona — w związku z obowiązkiem z art. 7 ust. 1 RODO (art. 6 ust. 1 lit. c RODO).
4. **Zakres danych.**
   - dane konta: imię, nazwisko, adres e-mail;
   - dane z wniosku: wydział, kierunek i rodzaj studiów, rok ukończenia, numer telefonu, zainteresowania, preferowana forma spotkań, chęć współtworzenia Klubu;
   - dane profilu, jeśli je Pani/Pan poda: opis, stanowisko, firma, adresy LinkedIn i GitHub, gotowość do mentoringu, tagi, powiązane konto Discord;
   - dane o aktywności w Klubie: zapisy na wydarzenia, odpowiedzi w ankietach, zgłoszenia;
   - identyfikator urządzenia do powiadomień push, jeśli Pani/Pan na nie pozwoli.
5. **Odbiorcy.**
   - uprawnieni pracownicy AGH i administratorzy Klubu;
   - inni zweryfikowani członkowie Klubu — w zakresie danych profilu, które zdecyduje się Pani/Pan pokazać;
   - podmioty uprawnione na podstawie przepisów prawa;
   - dostawcy usług technicznych: Expo (powiadomienia push, z wykorzystaniem usług Google i Apple), Discord (jeśli powiąże Pani/Pan konto), [do uzupełnienia: dostawcy hostingu aplikacji i przechowywania plików].
6. **Przekazywanie poza EOG.** Część dostawców wymienionych w pkt 5 ma siedzibę w Stanach Zjednoczonych. Przekazanie odbywa się na podstawie [do uzupełnienia: decyzji Komisji Europejskiej stwierdzającej odpowiedni stopień ochrony (EU-US Data Privacy Framework) lub standardowych klauzul umownych].
7. **Okres przechowywania.** Dane przechowujemy do czasu wycofania zgody lub zakończenia członkostwa. Dane z odrzuconego wniosku: [do uzupełnienia]. Zapis udzielonych zgód przechowujemy przez okres, w którym może być potrzebny do wykazania ich udzielenia: [do uzupełnienia].
8. **Prawa.** Przysługuje Pani/Panu prawo dostępu do danych, ich sprostowania, usunięcia, ograniczenia przetwarzania oraz przenoszenia danych, w przypadkach i na warunkach określonych w RODO.
9. **Wycofanie zgody.** Każdą zgodę można wycofać w dowolnym momencie. Zgodę na newsletter wycofuje się w ustawieniach konta w aplikacji, a zgodę na przetwarzanie danych w celu członkostwa — wiadomością na adres alumni@informatyka.agh.edu.pl. Wycofanie zgody nie wpływa na zgodność z prawem przetwarzania dokonanego przed jej wycofaniem.
10. **Skarga.** Przysługuje Pani/Panu prawo wniesienia skargi do Prezesa Urzędu Ochrony Danych Osobowych (art. 77 RODO).
11. **Dobrowolność.** Podanie danych jest dobrowolne, ale bez zgody na przetwarzanie danych w celu członkostwa nie jest możliwe przyjęcie do Klubu.
12. **Profilowanie.** Dane nie podlegają zautomatyzowanemu podejmowaniu decyzji, w tym profilowaniu.
$md$,
'Pierwsza pełna wersja informacji, opisująca aplikację Klubu: zakres danych, odbiorców i sposób wycofania zgody.', TRUE, NOW()),

(gen_random_uuid(), 'MEMBERSHIP_CONSENT', 2, 'Zgoda na przetwarzanie danych osobowych',
$md$Wyrażam zgodę na przetwarzanie moich danych osobowych podanych we wniosku i w profilu przez Akademię Górniczo-Hutniczą im. Stanisława Staszica w Krakowie w celu członkostwa w Klubie Alumnów Wydziału Informatyki AGH i korzystania z aplikacji Klubu.$md$,
'Zgoda wskazuje teraz administratora i cel przetwarzania.', TRUE, NOW()),

(gen_random_uuid(), 'NEWSLETTER_CONSENT', 2, 'Zgoda na newsletter',
$md$Chcę otrzymywać na mój adres e-mail newsletter Klubu Alumnów Wydziału Informatyki AGH z informacjami o wydarzeniach i inicjatywach Klubu.$md$,
NULL, FALSE, NOW());
