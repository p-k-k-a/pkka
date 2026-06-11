type ProseContentProps = {
  content: string;
  className?: string;
};

export function ProseContent({ content, className }: ProseContentProps) {
  const paragraphs = content.split(/\n\n+/).filter((p) => p.trim().length > 0);

  if (paragraphs.length === 0) {
    return <p className={className ?? "text-foreground/90 text-base leading-8"}>{content}</p>;
  }

  return (
    <div className="space-y-6">
      {paragraphs.map((paragraph, index) => (
        <p key={index} className={className ?? "text-foreground/90 text-base leading-8"}>
          {paragraph.trim()}
        </p>
      ))}
    </div>
  );
}
