import { cssVars } from "@pkka/theme/css-vars";
import plugin from "tailwindcss/plugin";

// Loaded by globals.css via @plugin, not by app code: it runs in the PostCSS
// build, so @pkka/theme never reaches the browser bundle.
export default plugin(({ addBase }) => {
  addBase({
    ":root": cssVars("light", "hex"),
    ".dark": cssVars("dark", "hex"),
  });
});
