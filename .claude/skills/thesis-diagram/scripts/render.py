"""Render an .excalidraw file to PNG next to it (or to --output).

    uv run --with playwright==1.64.1 python .claude/skills/thesis-diagram/scripts/render.py docs/thesis/x.excalidraw

First run on a machine also needs a browser:
    uv run --with playwright==1.64.1 playwright install chromium
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

PAGE = """<!doctype html><html><body style="margin:0;background:#fff">
<div id="out"></div>
<script type="module">
import { exportToSvg } from "https://esm.sh/@excalidraw/excalidraw@0.18.1";
window.renderDiagram = async (doc) => {
  const svg = await exportToSvg({
    elements: doc.elements,
    appState: { ...doc.appState, exportBackground: true, exportPadding: 24 },
    files: doc.files || {},
  });
  document.getElementById("out").appendChild(svg);
  const r = svg.getBoundingClientRect();
  return { width: Math.ceil(r.width), height: Math.ceil(r.height) };
};
window.ready = true;
</script></body></html>"""


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("file", type=Path)
    ap.add_argument("--output", type=Path)
    ap.add_argument("--scale", type=float, default=2)
    args = ap.parse_args()

    doc = json.loads(args.file.read_text(encoding="utf-8"))
    out = args.output or args.file.with_suffix(".png")

    from playwright.sync_api import sync_playwright

    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page(device_scale_factor=args.scale, viewport={"width": 4000, "height": 3000})
        page.set_content(PAGE)
        page.wait_for_function("window.ready === true", timeout=60000)
        size = page.evaluate("doc => window.renderDiagram(doc)", doc)
        page.wait_for_timeout(500)  # let the hand-drawn font finish loading
        page.locator("#out svg").screenshot(path=str(out))
        browser.close()
    print(out, f"{size['width']}x{size['height']}")


if __name__ == "__main__":
    main()
