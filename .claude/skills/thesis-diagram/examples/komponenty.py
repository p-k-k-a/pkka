"""Reference diagram: high-level component overview (thesis section 3.1).

Run from the repo root:
    python3 .claude/skills/thesis-diagram/examples/komponenty.py docs/thesis/komponenty.excalidraw
"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "scripts"))
from pkka_excalidraw import ROLE, Diagram  # noqa: E402

out = sys.argv[1] if len(sys.argv) > 1 else "docs/thesis/komponenty.excalidraw"
d = Diagram()

users = d.group(0, 40, 250, 400, "Użytkownicy")
for k, name in enumerate(["Gość\n(niezalogowany)", "Zalogowany\nużytkownik", "Zweryfikowany\nalumn", "Administrator"]):
    d.actor(20, 100 + k * 82, name)

web = d.box(320, 60, "Aplikacja\nwebowa", ROLE["channel"])
mob = d.box(320, 195, "Aplikacja\nmobilna", ROLE["channel"])
dc = d.box(320, 330, "Serwer Discord\n+ bot", ROLE["channel"])
be = d.box(630, 160, "Backend\n(REST API)", ROLE["core"], w=220, h=160, font=24)
iam = d.box(960, 50, "Warstwa IAM\n(tożsamość i dostęp)", ROLE["security"], w=230, font=19)
ntf = d.box(960, 195, "Warstwa\npowiadomień\n(push, e-mail)", ROLE["notify"], w=230, font=19)
db = d.box(960, 340, "Bazy danych", ROLE["data"], w=230)

fan = users.anchor("right")
for target in (web, mob, dc):
    d.arrow([fan, target.anchor("left")])
for client in (web, mob, dc):
    d.arrow([client.anchor("right"), (be.x - 4, be.cy + (client.cy - be.cy) * 0.5)], both=True)
d.arrow([(be.x + be.w + 4, be.cy - 45), iam.anchor("left")], both=True)
d.arrow([be.anchor("right"), ntf.anchor("left")])
d.arrow([(be.x + be.w + 4, be.cy + 45), db.anchor("left")], both=True)

print(d.save(out))
