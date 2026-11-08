# Discord lokalnie: ogłoszenia, weryfikacja i `/setup`

Backend rozmawia z Discordem na dwa sposoby:

- **wychodząco** (token bota): publikuje, edytuje i usuwa ogłoszenia wydarzeń, sprawdza role członka przy wniosku,
  tworzy role i prywatne kanały kół zainteresowań.
  Działa z każdego komputera, bez niczego dodatkowego.
- **przychodząco** (webhook): Discord wysyła `/setup` na `POST /api/public/discord/interactions`.
  Discord musi dosięgnąć tego adresu po HTTPS, więc lokalnie potrzebny jest tunel (krok 4).

## 1. Dev-aplikacja Discord (raz)

Załóż **osobną** aplikację na dev (np. „pkka-dev”) — Interactions Endpoint URL jest jeden na aplikację, więc tunel
wpisany w produkcyjną zepsułby komendę na prodzie.

1. https://discord.com/developers/applications → *New Application*.
2. *General Information*: skopiuj **Application ID** i **Public Key**.
3. *Bot* → *Reset Token*: skopiuj token.
4. Bot prywatny: *Installation* → *Install Link* = **None**, potem *Bot* → **Public Bot** wyłączone.
5. Załóż własny serwer testowy z kanałem `#ogloszenia`, rolą `@Absolwent` i kategorią `Koła`.
6. Zaproś bota (uprawnienia: wyświetlanie kanału, wysyłanie wiadomości, osadzanie linków, zarządzanie kanałami,
   rolami i wiadomościami):
   `https://discord.com/oauth2/authorize?client_id=<APPLICATION_ID>&scope=bot+applications.commands&permissions=268463120`

   Bot zaproszony wcześniej z `permissions=19456` nie ma uprawnień do kół. Otwórz nowy link jeszcze raz,
   a Discord dopisze brakujące uprawnienia do roli bota. Bot potrzebuje zarządzania wiadomościami, bo może nadać
   właścicielowi koła tylko to uprawnienie, które sam ma.

## 2. Zmienne w `backend/.env`

```properties
DISCORD_BOT_TOKEN=...
DISCORD_APPLICATION_ID=...
DISCORD_PUBLIC_KEY=...
```

## 3. Backend

Uruchom backend z IDE albo `./gradlew bootRun` w katalogu `backend`. W logu ma się pojawić `Registered /setup with Discord`.

## 4. Tunel (tylko do `/setup`)

```bash
cd backend
docker compose --profile discord up -d
docker compose logs discord-tunnel | grep -o 'https://[a-z0-9-]*\.trycloudflare\.com'
```

Tunel prowadzi do proxy (`backend/discord-tunnel/Caddyfile`), które przepuszcza **tylko** `POST` na endpoint interakcji —
reszta backendu (Swagger, API) nie jest widoczna z internetu. Sprawdź:

```bash
curl -i https://<adres>/swagger-ui.html                            # 404
curl -i -X POST -H 'Content-Type: application/json' -d '{"type":1}' \
  https://<adres>/api/public/discord/interactions                  # 401 (brak podpisu Discorda)
```

502 zamiast 401 znaczy, że proxy nie dosięga backendu na hoście. Na Linuksie z ufw firewall blokuje ruch
z kontenerów — wpuść sieci Dockera na port 8080 (tylko je, nie internet ani LAN):

```bash
sudo ufw allow from 172.16.0.0/12 to any port 8080 proto tcp comment 'pkka: docker -> backend'
```

W portalu dev-aplikacji: *General Information* → **Interactions Endpoint URL** =
`https://<adres>/api/public/discord/interactions` → *Save*. Discord przy zapisie wysyła PING i celowo źle
podpisane żądanie — zapis przejdzie tylko, jeśli backend działa i ma poprawny `DISCORD_PUBLIC_KEY`.

Adres tunelu zmienia się przy każdym starcie — po restarcie wpisz go w portalu ponownie.

## 5. `/setup`

- `/setup channel:#ogloszenia role:@Absolwent groups_category:Koła` → na kanale wiadomość potwierdzająca, u ciebie
  odpowiedź widoczna tylko dla ciebie. Komendy nie widać? Odśwież klienta Discorda (Ctrl+R).
- `/setup` bez opcji pokazuje bieżące ustawienia.
- Odmowy: `role:@everyone`, kanał, na którym bot nie ma uprawnień.
- Baza: `SELECT * FROM discord_settings;`

## Bez tunelu

Ogłoszenia i weryfikację da się sprawdzić bez kroku 4 — wystarczy wstawić konfigurację ręcznie (ID skopiujesz
po włączeniu *Tryb dewelopera* w Discordzie: prawy przycisk → *Kopiuj ID*):

```sql
INSERT INTO discord_settings (guild_id, announcements_channel_id, alumn_role_id, groups_category_id)
VALUES ('<id serwera>', '<id kanału>', '<id roli>', '<id kategorii>');
```

## 6. Ogłoszenia wydarzeń

Utwórz w panelu admina wydarzenie dla odbiorców **publiczne** albo **wszyscy alumni**:

- na kanale pojawia się embed (Termin, Forma, Miejsce przy wydarzeniu stacjonarnym lub hybrydowym, Link na `WEB_BASE_URL/events/{id}`),
- edycja wydarzenia edytuje wiadomość, zmiana odbiorców na grupę albo usunięcie wydarzenia ją usuwa,
- wydarzenie dla grupy nie trafia na Discorda,
- baza: `SELECT * FROM event_discord_posts;`

## 7. Weryfikacja przy wniosku

- Użytkownik musi być zalogowany **przez Discorda** (IdP w Keycloaku) — wtedy backend zna jego `discordId`.
- To konto Discord musi być na serwerze testowym i mieć rolę ustawioną w `/setup`.
- Złożony wniosek alumna od razu przechodzi na zaakceptowany, a użytkownik dostaje `verified-alumn`
  (Keycloak → *Users* → *Role mapping*; do nowego tokena trzeba się przelogować).
- Bez roli wniosek zostaje w kolejce do ręcznej oceny. Powód widać w logu (DEBUG na profilu dev).

## 8. Koła zainteresowań

Zatwierdź wniosek o koło (`POST /api/admin/topic-group-requests/{id}/approve`, np. w Swaggerze):

- w kategorii z `groups_category` powstaje kanał nazwany slugiem koła i rola nazwana jak koło,
- kanał widzą tylko osoby z tą rolą i admini serwera,
- właściciel koła z podpiętym Discordem ma na kanale zarządzanie wiadomościami (*Edytuj kanał* → *Uprawnienia*),
- rola ląduje pod rolą bota, więc bot może ją potem nadawać.

Bez kategorii, bez tokenu bota albo przy błędzie Discorda koło powstaje bez roli i kanału, a powód widać w logu.
`GET /api/admin/topic-groups` pokazuje stan powiązania w polu `discord`: `LINKED`, `MISSING` albo `NOT_CONFIGURED`.
`POST /api/admin/topic-groups/{id}/discord` tworzy brakującą rolę lub kanał i niczego nie dubluje.

Baza: `SELECT name, discord_role_id, discord_channel_id FROM topic_groups;`

## Sprzątanie

```bash
cd backend
docker compose stop discord-proxy discord-tunnel
```

Tunel żyje tylko, dopóki działa kontener — po zatrzymaniu adres przestaje istnieć.
