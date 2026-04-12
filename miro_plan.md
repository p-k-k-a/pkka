# Thesis Plan — Actors & User Stories

## Actor: Niezalogowany użytkownik

| #   | Priority | Platform    | User Story                                                                                                |
| --- | -------- | ----------- | --------------------------------------------------------------------------------------------------------- |
| 1   | must     | Web, Mobile | Chcę widzieć publiczne wpisy na blogu, aby mieć dostęp do najnowszych informacji o wydziale informatyki.  |
| 2   | must     | Web, Mobile | Chcę móc zalogować się do istniejącego konta, aby mieć dostęp do platformy.                               |
| 3   | must     | Web, Mobile | Chcę zarejestrować konto w platformie, by móc złożyć wniosek o dołączenie do klubu alumna.                |
| 4   | must     | Web, Mobile | Chcę widzieć kalendarz z publicznymi wydarzeniami, aby mieć możliwość uczestniczenia w publicznych wydarzeniach. |

## Actor: Zalogowany użytkownik

| #   | Priority | Platform    | User Story                                                                                                                    |
| --- | -------- | ----------- | ----------------------------------------------------------------------------------------------------------------------------- |
| 1   | must     | Web, Mobile | Chcę łatwo znaleźć i wypełnić formularz aplikacyjny, aby zgłosić chęć dołączenia do platformy.                                |
| 2   | must     | Web, Mobile | Chcę być świadomy obowiązujących polityk w aplikacji, w tym RODO, by wiedzieć, że aplikacja jest zgodna z prawem i regulaminem AGH. |
| 3   | should   | Web, Mobile | Chcę sprawdzić swój status bycia alumnem, aby dowiedzieć się, czy mój wniosek został pozytywnie rozpatrzony.                  |
| 4   | should   | Web, Mobile | Chcę móc podlinkować swoje konto Discord, aby uzyskać możliwość automatycznej weryfikacji statusu bycia alumnem w przypadku bycia na serwerze Discord, aby uprościć system rejestracji. |

## Actor: Zweryfikowany alumn

| #   | Priority | Platform    | User Story                                                                                                                                                                                |
| --- | -------- | ----------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | must     | Web, Mobile | Chcę przefiltrować listę alumnów (np. po tagu umiejętności "Python", "DevOps" lub chęci do bycia mentorem), aby znaleźć odpowiedniego eksperta.                                           |
| 2   | must     | Web, Mobile | Chcę móc dołączyć do wydarzenia transmitowanego online za pomocą Teams/Zoom/Google Meets.                                                                                                 |
| 3   | must     | Mobile      | Chcę otrzymywać powiadomienia o nadchodzących wydarzeniach i dostawać powiadomienia PUSH, aby być na bieżąco z aktualnościami.                                                            |
| 4   | must     | Web, Mobile | Chcę móc zapisywać i wypisywać się z nadchodzących wydarzeń, aby zgłaszać swoją obecność na wydarzeniu.                                                                                   |
| 5   | must     | Web, Mobile | Chcę mieć dostęp do zgromadzonych materiałów klubu alumna, aby móc w dowolnym momencie obejrzeć nagrania i prezentacje z minionych debat oraz warsztatów.                                 |
| 6   | must     | Web, Mobile | Chcę mieć możliwość edycji swojego profilu użytkownika poprzez dodanie aktualnego stanowiska, firmy oraz linków do profilu na LinkedIn i GitHub, informując społeczność alumnów o mojej aktualnej sytuacji zawodowej. |
| 7   | must     | Web, Mobile | Chcę móc wyszukać alumnów po nazwie/tagach/stanowiskach itp., aby nawiązać kontakt na Discordzie.                                                                                         |
| 8   | must     | Web, Mobile | Chcę móc wylistować wszystkich alumnów.                                                                                                                                                   |
| 9   | should   | Web, Mobile | Chcę mieć możliwość przeglądania profilów i móc dodać innych alumnów do swoich kontaktów, aby nawiązać kontakt.                                                                           |
| 10  | should   | Web, Mobile | Chcę móc składać wniosek o stworzenie nowego koła zainteresowań, aby móc pozostać w kontakcie z alumnami o podobnych zainteresowaniach.                                                   |
| 11  | should   | Mobile      | Chcę móc dołączyć do istniejącego koła zainteresowań, aby śledzić i wymieniać się informacjami z pozostałymi członkami tego koła.                                                         |
| 12  | should   | Web, Mobile | Chcę móc przeglądać profil dowolnego alumna z udostępnionymi przez niego informacjami.                                                                                                    |
| 13  | should   | Web, Mobile | Chcę móc dołączyć do listy rezerwowej w przypadku braku wolnych miejsc na wydarzenie.                                                                                                     |
| 14  | should   | Web, Mobile | Chcę móc zobaczyć, ile jest dostępnych miejsc na wydarzenie.                                                                                                                              |
| 15  | could    | Mobile      | Chcę móc zgłaszać propozycje tematów, aby wpływać aktywnie na agendę wydziału.                                                                                                            |
| 16  | could    | Web, Mobile | Chcę móc dodać wydarzenie, na które jestem zapisany, do kalendarza, aby mieć informacje o tym, co robię danego dnia.                                                                      |
| 17  | could    | Web, Mobile | Chcę móc zatajać niektóre informacje w profilu, by pozostawać anonimowym.                                                                                                                 |
| 18  | could    | Mobile      | Chcę widzieć ogłoszenia na kanałach udostępnione przez innych i móc łatwo przenieść się do kanału w aplikacji Discord, aby sprawdzić szczegóły i konwersacje innych użytkowników.         |

## Actor: Administrator

| #   | Priority | Platform | User Story                                                                                                                              |
| --- | -------- | -------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | must     | Web      | Chcę weryfikować wnioski o dołączenie w bezpiecznym panelu administracyjnym, aby dbać o to, by w społeczności znajdowały się tylko uprawnione osoby. |
| 2   | must     | Web      | Chcę tworzyć wpisy o wydarzeniach w jednym centralnym panelu, aby system automatycznie zaktualizował kalendarz na WWW, w aplikacji i na Discordzie. |
| 3   | should   | Web      | Chcę przeprowadzać ankiety wśród alumnów, aby zbierać opinie wpływające na działanie Klubu.                                             |
| 4   | should   | Web      | Chcę tworzyć ukryte kanały dla koła zainteresowań, aby zapewnić im bezpieczną i poufną przestrzeń do wymiany doświadczeń.               |
| 5   | should   | Web      | Chcę mieć wgląd do statystyk danego wydarzenia (kanałów nadawczych itp.), aby móc je analizować i dostosowywać marketing.               |

## Actor: Bot

| #   | Priority | Platform | User Story                                                                                                                              |
| --- | -------- | -------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | should   | Discord  | Chcę pobierać nowe wydarzenia z aplikacji i pisać o nich na Discordzie, aby powiadamiać klubowiczów niekorzystających z aplikacji.      |
| 2   | should   | Discord  | Chcę automatycznie weryfikować, czy ktoś rejestrujący się jako alumn w aplikacji, gdy jest na Discordzie, jest również alumnem, aby przyspieszyć proces rejestracji. |
| 3   | should   | Discord  | Chcę synchronizować role użytkowników pomiędzy aplikacją a Discordem, aby oba kanały miały aktualne informacje.                         |
