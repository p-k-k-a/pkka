"use client";

import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { Toggle } from "@/components/ui/toggle";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";
import { Markdown } from "@tiptap/markdown";
import { EditorContent, useEditor, useEditorState } from "@tiptap/react";
import { StarterKit } from "@tiptap/starter-kit";
import {
  Bold,
  Code,
  Heading1,
  Heading2,
  Heading3,
  Heading4,
  Heading5,
  Heading6,
  Italic,
  List,
  ListOrdered,
  Redo2,
  TextQuote,
  Undo2,
} from "lucide-react";

function ToolbarTip({ label, children }: { label: string; children: React.ReactElement }) {
  return (
    <Tooltip disableHoverableContent>
      <TooltipTrigger asChild>{children}</TooltipTrigger>
      <TooltipContent>{label}</TooltipContent>
    </Tooltip>
  );
}

type MarkdownEditorProps = {
  initialContent: string;
  onChange: (markdown: string) => void;
  className?: string;
  ariaLabel?: string;
};

export function MarkdownEditor({
  initialContent,
  onChange,
  className,
  ariaLabel = "Treść wpisu",
}: MarkdownEditorProps) {
  const editor = useEditor({
    extensions: [
      // those 2 are not supported by portable markdown, let's not use them
      StarterKit.configure({ underline: false, strike: false }),
      Markdown,
    ],
    content: initialContent,
    contentType: "markdown",
    immediatelyRender: false,
    onUpdate: ({ editor }) => onChange(editor.getMarkdown()),
    editorProps: {
      attributes: {
        class: "focus:outline-none min-h-64 px-4 py-3",
        "aria-label": ariaLabel,
      },
    },
  });

  const state = useEditorState({
    editor,
    selector: ({ editor }) => ({
      bold: editor?.isActive("bold") ?? false,
      italic: editor?.isActive("italic") ?? false,
      code: editor?.isActive("code") ?? false,
      h1: editor?.isActive("heading", { level: 1 }) ?? false,
      h2: editor?.isActive("heading", { level: 2 }) ?? false,
      h3: editor?.isActive("heading", { level: 3 }) ?? false,
      h4: editor?.isActive("heading", { level: 4 }) ?? false,
      h5: editor?.isActive("heading", { level: 5 }) ?? false,
      h6: editor?.isActive("heading", { level: 6 }) ?? false,
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
      icon: Code,
      label: "Kod",
      active: state?.code,
      run: () => editor.chain().focus().toggleCode().run(),
    },
  ];

  const blocks = [
    {
      icon: Heading1,
      label: "Nagłówek 1",
      active: state?.h1,
      run: () => editor.chain().focus().toggleHeading({ level: 1 }).run(),
    },
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
      icon: Heading4,
      label: "Nagłówek 4",
      active: state?.h4,
      run: () => editor.chain().focus().toggleHeading({ level: 4 }).run(),
    },
    {
      icon: Heading5,
      label: "Nagłówek 5",
      active: state?.h5,
      run: () => editor.chain().focus().toggleHeading({ level: 5 }).run(),
    },
    {
      icon: Heading6,
      label: "Nagłówek 6",
      active: state?.h6,
      run: () => editor.chain().focus().toggleHeading({ level: 6 }).run(),
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
          <ToolbarTip key={label} label={label}>
            <Toggle size="sm" aria-label={label} pressed={active} onPressedChange={run}>
              <Icon />
            </Toggle>
          </ToolbarTip>
        ))}
        <Separator orientation="vertical" className="mx-1" />
        {blocks.map(({ icon: Icon, label, active, run }) => (
          <ToolbarTip key={label} label={label}>
            <Toggle size="sm" aria-label={label} pressed={active} onPressedChange={run}>
              <Icon />
            </Toggle>
          </ToolbarTip>
        ))}
        <Separator orientation="vertical" className="mx-1" />
        <ToolbarTip label="Cofnij">
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            aria-label="Cofnij"
            disabled={!state?.canUndo}
            onClick={() => editor.chain().focus().undo().run()}
          >
            <Undo2 />
          </Button>
        </ToolbarTip>
        <ToolbarTip label="Ponów">
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            aria-label="Ponów"
            disabled={!state?.canRedo}
            onClick={() => editor.chain().focus().redo().run()}
          >
            <Redo2 />
          </Button>
        </ToolbarTip>
      </div>
      <EditorContent editor={editor} className="rich-text space-y-5 leading-8" />
    </div>
  );
}
