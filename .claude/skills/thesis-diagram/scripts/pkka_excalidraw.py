"""House style for PKKA thesis diagrams (hand-drawn Excalidraw look).

Import this from a small build script, place elements with the helpers, then
call save(). Every helper bakes in the shared style, so diagrams drawn by
different people come out identical in look.

    import sys; sys.path.insert(0, ".claude/skills/thesis-diagram/scripts")
    from pkka_excalidraw import Diagram, ROLE

    d = Diagram()
    web = d.box(320, 60, "Aplikacja\\nwebowa", ROLE["channel"])
    be = d.box(630, 160, "Backend\\n(REST API)", ROLE["core"], w=220, h=160, font=24)
    d.arrow_between(web, be, both=True)
    d.save("docs/thesis/moj-diagram.excalidraw")
"""

from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path

INK = "#1e1e1e"
MUTED = "#495057"

# One colour per role. Excalidraw's own picker colours, so the files stay
# editable by hand without the palette drifting.
ROLE = {
    "channel": {"fill": "#a5d8ff", "style": "hachure"},  # web, mobile, Discord: how users reach us
    "core": {"fill": "#ffec99", "style": "solid"},  # backend: the one solid box, the eye lands here
    "security": {"fill": "#ffc9c9", "style": "hachure"},  # IAM, auth, access
    "notify": {"fill": "#d0bfff", "style": "hachure"},  # push, e-mail
    "data": {"fill": "#b2f2bb", "style": "hachure"},  # databases, file storage
    "external": {"fill": "#ffd8a8", "style": "hachure"},  # third-party services not covered above
    "actor": {"fill": "transparent", "style": "solid"},  # people, outside the system
}

DEFAULT_W, DEFAULT_H, DEFAULT_FONT = 210, 90, 20
LINE_HEIGHT = 1.25


@dataclass
class Shape:
    id: str
    x: float
    y: float
    w: float
    h: float

    @property
    def cx(self) -> float:
        return self.x + self.w / 2

    @property
    def cy(self) -> float:
        return self.y + self.h / 2

    def anchor(self, side: str, gap: float = 4) -> tuple[float, float]:
        return {
            "left": (self.x - gap, self.cy),
            "right": (self.x + self.w + gap, self.cy),
            "top": (self.cx, self.y - gap),
            "bottom": (self.cx, self.y + self.h + gap),
        }[side]


class Diagram:
    def __init__(self) -> None:
        self.elements: list[dict] = []
        self._seed = 1000
        self._ids: set[str] = set()

    def _next(self, prefix: str) -> tuple[str, int]:
        self._seed += 1
        i = f"{prefix}_{self._seed}"
        self._ids.add(i)
        return i, self._seed

    def _base(self, kind: str, prefix: str, x, y, w, h, **extra) -> dict:
        i, seed = self._next(prefix)
        e = {
            "type": kind, "id": i, "x": x, "y": y, "width": w, "height": h,
            "strokeColor": INK, "backgroundColor": "transparent", "fillStyle": "solid",
            "strokeWidth": 2, "strokeStyle": "solid", "roughness": 1, "opacity": 100,
            "angle": 0, "seed": seed, "groupIds": [], "boundElements": None,
            "isDeleted": False, "locked": False, "link": None,
        }
        e.update(extra)
        self.elements.append(e)
        return e

    def text(self, x, y, txt: str, font=18, w=None, color=INK, align="left", container=None) -> dict:
        lines = txt.split("\n")
        h = round(len(lines) * font * LINE_HEIGHT)
        w = w if w is not None else max(len(line) for line in lines) * font * 0.55
        return self._base(
            "text", "txt", x, y, w, h, text=txt, originalText=txt, fontSize=font,
            fontFamily=5, textAlign=align, verticalAlign="middle" if container else "top",
            strokeColor=color, containerId=container, lineHeight=LINE_HEIGHT,
        )

    def box(self, x, y, label: str, role=ROLE["channel"], w=DEFAULT_W, h=DEFAULT_H,
            font=DEFAULT_FONT, shape="rectangle", dashed=False) -> Shape:
        extra = {"roundness": {"type": 3}} if shape == "rectangle" else {}
        rect = self._base(
            shape, "box", x, y, w, h, backgroundColor=role["fill"], fillStyle=role["style"],
            strokeStyle="dashed" if dashed else "solid", **extra,
        )
        if label:
            th = round((label.count("\n") + 1) * font * LINE_HEIGHT)
            t = self.text(x + 5, y + (h - th) / 2, label, font, w=w - 10, align="center",
                          container=rect["id"])
            rect["boundElements"] = [{"id": t["id"], "type": "text"}]
        return Shape(rect["id"], x, y, w, h)

    def actor(self, x, y, label: str, w=210, h=70, font=16) -> Shape:
        return self.box(x, y, label, ROLE["actor"], w=w, h=h, font=font, shape="ellipse")

    def group(self, x, y, w, h, title: str, font=22) -> Shape:
        """Dashed frame that gathers related things (e.g. all user roles)."""
        frame = self._base("rectangle", "grp", x, y, w, h, strokeStyle="dashed",
                           roundness={"type": 3})
        self.text(x, y + 12, title, font, w=w, align="center")
        return Shape(frame["id"], x, y, w, h)

    def label(self, x, y, txt: str, font=16, w=None, align="left") -> dict:
        """Small grey note, e.g. next to an arrow."""
        return self.text(x, y, txt, font, w=w, color=MUTED, align=align)

    def arrow(self, points: list[tuple[float, float]], both=False, dashed=False) -> dict:
        x0, y0 = points[0]
        rel = [[px - x0, py - y0] for px, py in points]
        xs = [p[0] for p in rel]
        ys = [p[1] for p in rel]
        return self._base(
            "arrow", "arr", x0, y0, max(xs) - min(xs), max(ys) - min(ys), points=rel,
            strokeStyle="dashed" if dashed else "solid",
            startArrowhead="arrow" if both else None, endArrowhead="arrow",
        )

    def arrow_between(self, a: Shape, b: Shape, a_side="right", b_side="left",
                      both=False, dashed=False) -> dict:
        return self.arrow([a.anchor(a_side), b.anchor(b_side)], both=both, dashed=dashed)

    def save(self, path: str | Path) -> Path:
        path = Path(path)
        path.parent.mkdir(parents=True, exist_ok=True)
        doc = {
            "type": "excalidraw", "version": 2, "source": "https://excalidraw.com",
            "elements": self.elements,
            "appState": {"viewBackgroundColor": "#ffffff", "gridSize": 20},
            "files": {},
        }
        path.write_text(json.dumps(doc, ensure_ascii=False, indent=1))
        return path
