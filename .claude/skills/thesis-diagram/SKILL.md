---
name: thesis-diagram
description: Draw figures for the PKKA engineering thesis (praca inżynierska) in the team's shared hand-drawn Excalidraw style, so every author's diagrams look the same. Use this whenever someone wants a diagram, rysunek, obraz, schemat, graf or architecture/component/flow picture for the thesis or docs/thesis — even if they just say "zrób rysunek do 3.2", "narysuj przepływ logowania", "dodaj diagram komponentów" or "excalidraw". Prefer it over the generic excalidraw-diagram skill for anything that goes into the thesis.
---

# Thesis diagrams (PKKA)

Figures for the thesis are drawn by four authors over many weeks. Readers notice when Obraz 3 looks nothing like Obraz 7, so every figure uses the same look. The look lives in code, in `scripts/pkka_excalidraw.py`; nobody picks colours or fonts by hand.

`examples/komponenty.py` is the reference figure, the one the team chose. Read it before you draw anything: it shows the layout rhythm, the sizes and the way the arrows are placed.

## The look and why

- **Hand-drawn.** Excalidraw's sketchy stroke (`roughness: 1`) and its Excalifont. The figures should look made by people at a whiteboard, not generated. Excalifont renders Polish letters correctly; the older Virgil font does not.
- **Black ink, pastel fills.** All strokes and text are `#1e1e1e`. Fills come from Excalidraw's own colour picker, so anyone who later opens the file on excalidraw.com and adds a box gets the same colours.
- **One colour per role** (`ROLE` in the library). The colour tells the reader what kind of thing a box is, the same way in every figure:

  | Role | Colour | For |
  |---|---|---|
  | `channel` | blue, hatched | how users reach the system: web app, mobile app, Discord |
  | `core` | yellow, solid | the backend. It is the only solid box, so the eye lands on it first |
  | `security` | red, hatched | IAM, logging in, access |
  | `notify` | violet, hatched | push, e-mail |
  | `data` | green, hatched | databases, file storage |
  | `external` | orange, hatched | other third-party services |
  | `actor` | no fill, ellipse | people: guest, logged-in user, verified alumn, administrator |

  Related actors sit together in a dashed `group` frame with a title (for example „Użytkownicy").
- **Plain arrows.** Solid black. Use a double-headed arrow (`both=True`) when the two sides talk to each other (request and response, read and write). Use a single head when one side only pushes, for example the backend handing work to notifications. Use a dashed arrow for something asynchronous or indirect, and give it a short grey `label`.

## Wording

- All text is in Polish and matches the thesis vocabulary: „grupy tematyczne" (never „koła zainteresowań"), „alumn", „administrator", „Warstwa IAM".
- Overview figures name roles, not products: „Warstwa IAM", „Bazy danych", „Warstwa powiadomień". Products (Keycloak, PostgreSQL, Expo, Brevo, Cloudflare R2) only appear in detailed figures, and only when the section around the figure talks about them.
- Box labels are short, two or three lines, with a parenthesis for the detail: „Backend\n(REST API)". The explanation goes in the thesis text under the figure, not on the drawing.
- Do not put a title on the figure. The caption („Obraz N: …") lives in the thesis.
- Check every claim on a diagram against the code in this repo before you draw it. A wrong arrow in a thesis is worse than a missing one.

## Workflow

1. Plan the figure: which boxes, which role each box has, which arrows. Keep it to about 10 boxes; past that the figure needs splitting into an overview and a detail figure.
2. Write a small build script next to the output, for example `docs/thesis/src/<name>.py`, modelled on `examples/komponenty.py`. Use only the library helpers (`box`, `actor`, `group`, `arrow`, `arrow_between`, `label`), so the style stays shared. Keeping the script lets the next person change the figure and rebuild it, instead of editing JSON by hand.
3. Lay things out on the reference grid: columns about 300–330 px apart, boxes 210×90, the backend larger (220×160, font 24), vertical step 135–145 px. Flow goes left to right: people → channels → backend → services.
4. Build and render:
   ```bash
   python3 docs/thesis/src/<name>.py docs/thesis/<name>.excalidraw
   uv run --with playwright==1.64.1 python .claude/skills/thesis-diagram/scripts/render.py docs/thesis/<name>.excalidraw
   ```
   On a new machine, run `uv run --with playwright==1.64.1 playwright install chromium` once first.
5. Open the PNG and look at it. Check that no label is clipped or touching a border, that arrows land on the box they mean, and that spacing is even. Fix the build script and render again until it is clean. This usually takes 1–3 rounds.
6. Hand over the `.excalidraw` file (editable on excalidraw.com) and the `.png`. For the thesis PDF, export SVG or PDF from excalidraw.com so the figure stays sharp in print.

## Files

- `scripts/pkka_excalidraw.py`: the style library (palette, helpers, `save`).
- `scripts/render.py`: `.excalidraw` → PNG via headless Chromium.
- `examples/komponenty.py`: the reference figure (component overview, section 3.1).
