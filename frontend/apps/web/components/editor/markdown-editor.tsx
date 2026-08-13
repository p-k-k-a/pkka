"use client";

import { EditorContent, useEditor, useEditorState } from "@tiptap/react";
import { StarterKit } from "@tiptap/starter-kit";
import { Markdown } from "@tiptap/markdown";
import {
  Bold,
  Code,
  Heading2,
  Heading3,
  Italic,
  List,
  ListOrdered,
  Redo2,
  Strikethrough,
  TextQuote,
  Undo2,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { Toggle } from "@/components/ui/toggle";
import { cn } from "@/lib/utils";

const ACTIVE_TOGGLE = "data-[state=on]:bg-accent data-[state=on]:text-accent-foreground";

type MarkdownEditorProps = {
  initialContent: string;
  onChange: (markdown: string) => void;
  className?: string;
};

export function MarkdownEditor({ initialContent, onChange, className }: MarkdownEditorProps) {
  const editor = useEditor({
    extensions: [StarterKit, Markdown],
    content: initialContent,
    contentType: "markdown",
    immediatelyRender: false,
    onUpdate: ({ editor }) => onChange(editor.getMarkdown()),
    editorProps: {
      attributes: {
        class: "focus:outline-none min-h-64 px-4 py-3",
        "aria-label": "Treść wpisu",
      },
    },
  });

  const state = useEditorState({
    editor,
    selector: ({ editor }) => ({
      bold: editor?.isActive("bold") ?? false,
      italic: editor?.isActive("italic") ?? false,
      strike: editor?.isActive("strike") ?? false,
      code: editor?.isActive("code") ?? false,
      h2: editor?.isActive("heading", { level: 2 }) ?? false,
      h3: editor?.isActive("heading", { level: 3 }) ?? false,
      bulletList: editor?.isActive("bulletList") ?? false,
      orderedList: editor?.isActive("orderedList") ?? false,
      blockquote: editor?.isActive("blockquote") ?? false,
      canUndo: editor?.can().undo() ?? false,
      canRedo: editor?.can().redo() ?? false,
    }),
  });

  if (!editor) {
    return <div className={cn("bg-muted/40 min-h-80 animate-pulse rounded-lg", className)} />;
  }

  const marks = [
    {
      icon: Bold,
      label: "Pogrubienie",
      active: state?.bold,
      run: () => editor.chain().focus().toggleBold().run(),
    },
    {
      icon: Italic,
      label: "Kursywa",
      active: state?.italic,
      run: () => editor.chain().focus().toggleItalic().run(),
    },
    {
      icon: Strikethrough,
      label: "Przekreślenie",
      active: state?.strike,
      run: () => editor.chain().focus().toggleStrike().run(),
    },
    {
      icon: Code,
      label: "Kod",
      active: state?.code,
      run: () => editor.chain().focus().toggleCode().run(),
    },
  ];

  const blocks = [
    {
      icon: Heading2,
      label: "Nagłówek 2",
      active: state?.h2,
      run: () => editor.chain().focus().toggleHeading({ level: 2 }).run(),
    },
    {
      icon: Heading3,
      label: "Nagłówek 3",
      active: state?.h3,
      run: () => editor.chain().focus().toggleHeading({ level: 3 }).run(),
    },
    {
      icon: List,
      label: "Lista punktowana",
      active: state?.bulletList,
      run: () => editor.chain().focus().toggleBulletList().run(),
    },
    {
      icon: ListOrdered,
      label: "Lista numerowana",
      active: state?.orderedList,
      run: () => editor.chain().focus().toggleOrderedList().run(),
    },
    {
      icon: TextQuote,
      label: "Cytat",
      active: state?.blockquote,
      run: () => editor.chain().focus().toggleBlockquote().run(),
    },
  ];

  return (
    <div className={cn("border-border bg-background rounded-lg border", className)}>
      <div className="border-border flex flex-wrap items-center gap-1 border-b p-1.5">
        {marks.map(({ icon: Icon, label, active, run }) => (
          <Toggle
            key={label}
            size="sm"
            aria-label={label}
            title={label}
            pressed={active}
            onPressedChange={run}
            className={ACTIVE_TOGGLE}
          >
            <Icon />
          </Toggle>
        ))}
        <Separator orientation="vertical" className="mx-1" />
        {blocks.map(({ icon: Icon, label, active, run }) => (
          <Toggle
            key={label}
            size="sm"
            aria-label={label}
            title={label}
            pressed={active}
            onPressedChange={run}
            className={ACTIVE_TOGGLE}
          >
            <Icon />
          </Toggle>
        ))}
        <Separator orientation="vertical" className="mx-1" />
        <Button
          type="button"
          variant="ghost"
          size="icon-sm"
          aria-label="Cofnij"
          title="Cofnij"
          disabled={!state?.canUndo}
          onClick={() => editor.chain().focus().undo().run()}
        >
          <Undo2 />
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="icon-sm"
          aria-label="Ponów"
          title="Ponów"
          disabled={!state?.canRedo}
          onClick={() => editor.chain().focus().redo().run()}
        >
          <Redo2 />
        </Button>
      </div>
      <EditorContent
        editor={editor}
        className={cn(
          "text-foreground/90 text-base leading-7",
          "[&_h2]:font-heading [&_h2]:text-foreground [&_h2]:mt-4 [&_h2]:mb-2 [&_h2]:text-xl [&_h2]:font-semibold",
          "[&_h3]:font-heading [&_h3]:text-foreground [&_h3]:mt-3 [&_h3]:mb-1.5 [&_h3]:text-lg [&_h3]:font-semibold",
          "[&_p]:my-2",
          "[&_ul]:my-2 [&_ul]:list-disc [&_ul]:pl-6",
          "[&_ol]:my-2 [&_ol]:list-decimal [&_ol]:pl-6",
          "[&_blockquote]:border-primary [&_blockquote]:text-muted-foreground [&_blockquote]:my-2 [&_blockquote]:border-l-4 [&_blockquote]:pl-4 [&_blockquote]:italic",
          "[&_code]:bg-muted [&_code]:rounded-sm [&_code]:px-1.5 [&_code]:py-0.5 [&_code]:font-mono [&_code]:text-sm",
          "[&_pre]:bg-muted [&_pre]:my-2 [&_pre]:overflow-x-auto [&_pre]:rounded-lg [&_pre]:p-4 [&_pre_code]:bg-transparent [&_pre_code]:p-0",
        )}
      />
    </div>
  );
}
