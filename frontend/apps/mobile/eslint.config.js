// https://docs.expo.dev/guides/using-eslint/
const { defineConfig } = require("eslint/config");
const expoConfig = require("eslint-config-expo/flat");

const PALETTE_CLASS =
  "/(^|[\\s:])(text|bg|border|ring|fill|stroke|decoration|outline|divide|from|via|to)-(white|black|(slate|gray|zinc|neutral|stone|red|orange|amber|yellow|lime|green|emerald|teal|cyan|sky|blue|indigo|violet|purple|fuchsia|pink|rose)-\\d{2,3})(?![\\w-])/";

module.exports = defineConfig([
  expoConfig,
  {
    ignores: ["dist/*"],
  },
  {
    files: ["app/**/*.{ts,tsx}", "components/**/*.{ts,tsx}"],
    rules: {
      "no-restricted-syntax": [
        "error",
        {
          selector: "MemberExpression[object.name='THEME'][property.name=/^(light|dark)$/]",
          message: "Use useThemeColors() so the colour follows the active scheme.",
        },
        {
          selector: "Literal[value=/^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/]",
          message: "Use a semantic token (className or useThemeColors()), not a hex literal.",
        },
        {
          selector: "Literal[value=/^(rgba?|hsla?)\\(/]",
          message: "Use a semantic token (className or useThemeColors()), not a colour function.",
        },
        {
          selector:
            "Literal[value=/^(black|white|red|green|blue|gray|grey|yellow|orange|purple|pink)$/i]",
          message: "Use a semantic token (className or useThemeColors()), not a named colour.",
        },
        {
          selector: `:matches(Literal[value=${PALETTE_CLASS}], TemplateElement[value.raw=${PALETTE_CLASS}])`,
          message:
            "Use a semantic token class (text-foreground, bg-card, ...), not a palette class.",
        },
      ],
    },
  },
]);
