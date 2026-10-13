# Discord lokalnie: weryfikacja i `/konfiguracja`

Backend rozmawia z Discordem na dwa sposoby:

- **wychodząco** (token bota): sprawdza role członka przy wniosku, a przy `/konfiguracja` wysyła potwierdzenie na kanał.
  Działa z każdego komputera, bez niczego dodatkowego.
- **przychodząco** (webhook): Discord wysyła `/konfiguracja` na `POST /api/public/discord/interactions`.
  Discord musi dosięgnąć tego adresu po HTTPS, więc lokalnie potrzebny jest tunel (krok 4).

## 1. Dev-aplikacja Discord (raz)

Załóż **osobną** aplikację na dev (np. „pkka-dev”) — Interactions Endpoint URL jest jeden na aplikację, więc tunel
wpisany w produkcyjną zepsułby komendę na prodzie.

1. https://discord.com/developers/applications → *New Application*.
2. *General Information*: skopiuj **Application ID** i **Public Key**.
3. *Bot* → *Reset Token*: skopiuj token.
4. Bot prywatny: *Installation* → *Install Link* = **None**, potem *Bot* → **Public Bot** wyłączone.
5. Załóż własny serwer testowy z kanałem `#ogloszenia` i rolą `@Absolwent`.
6. Zaproś bota (uprawnienia: wyświetlanie kanału, wysyłanie wiadomości, osadzanie linków):
   `https://discord.com/oauth2/authorize?client_id=<APPLICATION_ID>&scope=bot+applications.commands&permissions=19456`

## 2. Zmienne w `backend/.env`

```properties
DISCORD_BOT_TOKEN=...
DISCORD_APPLICATION_ID=...
DISCORD_PUBLIC_KEY=...
```

## 3. Backend

Uruchom backend z IDE albo `./gradlew bootRun` w katalogu `backend`. W logu ma się pojawić `Registered /konfiguracja with Discord`.

## 4. Tunel (tylko do `/konfiguracja`)

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

## 5. `/konfiguracja`

- `/konfiguracja kanal:#ogloszenia rola:@Absolwent` → na kanale wiadomość potwierdzająca, u ciebie odpowiedź
  widoczna tylko dla ciebie. Komendy nie widać? Odśwież klienta Discorda (Ctrl+R).
- `/konfiguracja` bez opcji pokazuje bieżące ustawienia.
- Odmowy: `rola:@everyone`, kanał, na którym bot nie ma uprawnień.
- Baza: `SELECT * FROM discord_settings;`

## Bez tunelu

Weryfikację da się sprawdzić bez kroku 4 — wystarczy wstawić konfigurację ręcznie (ID skopiujesz
po włączeniu *Tryb dewelopera* w Discordzie: prawy przycisk → *Kopiuj ID*):

```sql
INSERT INTO discord_settings (guild_id, announcements_channel_id, alumn_role_id)
VALUES ('<id serwera>', '<id kanału>', '<id roli>');
```

## 6. Weryfikacja przy wniosku

- Użytkownik musi być zalogowany **przez Discorda** (IdP w Keycloaku) — wtedy backend zna jego `discordId`.
- To konto Discord musi być na serwerze testowym i mieć rolę ustawioną w `/konfiguracja`.
- Złożony wniosek alumna od razu przechodzi na zaakceptowany, a użytkownik dostaje `verified-alumn`
  (Keycloak → *Users* → *Role mapping*; do nowego tokena trzeba się przelogować).
- Bez roli wniosek zostaje w kolejce do ręcznej oceny. Powód widać w logu (DEBUG na profilu dev).

## Sprzątanie

```bash
cd backend
docker compose stop discord-proxy discord-tunnel
```

Tunel żyje tylko, dopóki działa kontener — po zatrzymaniu adres przestaje istnieć.
