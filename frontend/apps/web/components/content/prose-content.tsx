import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { cn } from "@/lib/utils";

type ProseContentProps = {
  content: string;
  className?: string;
};

export function ProseContent({ content, className }: ProseContentProps) {
  return (
    <div
      className={cn(
        "text-foreground/90 space-y-5 text-base leading-8",
        "[&_h1]:font-heading [&_h1]:text-foreground [&_h1]:text-2xl [&_h1]:font-semibold",
        "[&_h2]:font-heading [&_h2]:text-foreground [&_h2]:text-xl [&_h2]:font-semibold",
        "[&_h3]:font-heading [&_h3]:text-foreground [&_h3]:text-lg [&_h3]:font-semibold",
        "[&_a]:text-accent [&_a]:underline [&_a]:underline-offset-4",
        "[&_ul]:list-disc [&_ul]:space-y-1 [&_ul]:pl-6",
        "[&_ol]:list-decimal [&_ol]:space-y-1 [&_ol]:pl-6",
        "[&_blockquote]:border-primary [&_blockquote]:text-muted-foreground [&_blockquote]:border-l-4 [&_blockquote]:pl-4 [&_blockquote]:italic",
        "[&_code]:bg-muted [&_code]:rounded-sm [&_code]:px-1.5 [&_code]:py-0.5 [&_code]:font-mono [&_code]:text-sm",
        "[&_pre]:bg-muted [&_pre]:overflow-x-auto [&_pre]:rounded-lg [&_pre]:p-4 [&_pre_code]:bg-transparent [&_pre_code]:p-0",
        "[&_hr]:border-border",
        "[&_img]:my-2 [&_img]:max-w-full [&_img]:rounded-lg",
        "[&_td]:border-border [&_th]:border-border [&_table]:w-full [&_table]:border-collapse [&_td]:border [&_td]:px-3 [&_td]:py-2 [&_th]:border [&_th]:px-3 [&_th]:py-2 [&_th]:text-left",
        className,
      )}
    >
      <ReactMarkdown remarkPlugins={[remarkGfm]}>{content}</ReactMarkdown>
    </div>
  );
}
