import ReactMarkdown from "react-markdown";
import { cn } from "@/lib/utils";

type ProseContentProps = {
  content: string;
  className?: string;
};

export function ProseContent({ content, className }: ProseContentProps) {
  return (
    <div className={cn("rich-text space-y-5 leading-8", className)}>
      <ReactMarkdown>{content}</ReactMarkdown>
    </div>
  );
}
