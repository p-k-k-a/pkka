import { useThemeColors } from "@/lib/theme";
import * as WebBrowser from "expo-web-browser";
import { EnrichedMarkdownText, type MarkdownStyle } from "react-native-enriched-markdown";

const BLOCK_GAP = 20;
const LINE_HEIGHT = 32;

type ProseContentProps = {
  content: string;
};

export function ProseContent({ content }: ProseContentProps) {
  const theme = useThemeColors();

  const heading = (fontSize: number, color = theme.foreground) => ({
    fontSize,
    fontFamily: "Jost",
    fontWeight: "600",
    color,
    lineHeight: LINE_HEIGHT,
    marginTop: 0,
    marginBottom: BLOCK_GAP,
  });

  const markdownStyle: MarkdownStyle = {
    paragraph: {
      fontSize: 16,
      fontFamily: "Montserrat",
      color: theme.mutedForeground,
      lineHeight: LINE_HEIGHT,
      marginTop: 0,
      marginBottom: BLOCK_GAP,
    },
    h1: heading(24),
    h2: heading(20),
    h3: heading(18),
    h4: heading(16),
    h5: heading(14),
    h6: heading(14, theme.mutedForeground),
    link: { color: theme.accent, underline: true },
    list: {
      fontSize: 16,
      fontFamily: "Montserrat",
      color: theme.mutedForeground,
      lineHeight: LINE_HEIGHT,
      marginTop: 0,
      marginBottom: BLOCK_GAP,
      bulletColor: theme.mutedForeground,
      markerColor: theme.mutedForeground,
      markerFontWeight: "400",
      markerMinWidth: 16,
      gapWidth: 8,
      itemSpacing: 4,
    },
    blockquote: {
      fontSize: 16,
      fontFamily: "Montserrat",
      color: theme.mutedForeground,
      lineHeight: LINE_HEIGHT,
      marginTop: 0,
      marginBottom: BLOCK_GAP,
      borderColor: theme.primary,
      borderWidth: 4,
      gapWidth: 16,
      backgroundColor: "transparent",
    },
    code: {
      fontSize: 14,
      color: theme.mutedForeground,
      backgroundColor: theme.muted,
      borderColor: theme.muted,
    },
    codeBlock: {
      fontSize: 14,
      color: theme.mutedForeground,
      backgroundColor: theme.muted,
      borderColor: theme.muted,
      borderRadius: 8,
      padding: 16,
      marginTop: 0,
      marginBottom: BLOCK_GAP,
    },
    thematicBreak: {
      color: theme.border,
      height: 1,
      marginTop: 0,
      marginBottom: BLOCK_GAP,
    },
  };

  return (
    <EnrichedMarkdownText
      markdown={content}
      markdownStyle={markdownStyle}
      md4cFlags={{ latexMath: false }}
      onLinkPress={({ url }) => WebBrowser.openBrowserAsync(url)}
    />
  );
}
