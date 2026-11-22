import { CharacterCount } from "@/components/admin/event-message/character-count";
import { MESSAGE_LIMITS } from "@/components/admin/event-message/message-format";

type PushPreviewProps = {
  eventTitle: string;
  title: string;
  text: string;
  onTitleChange: (title: string) => void;
  onTextChange: (text: string) => void;
};

export function PushPreview({
  eventTitle,
  title,
  text,
  onTitleChange,
  onTextChange,
}: PushPreviewProps) {
  return (
    <div className="space-y-2">
      <div className="bg-muted rounded-2xl p-3">
        <div className="bg-background rounded-2xl p-3 shadow-sm">
          <div className="min-w-0 space-y-0.5">
            <div className="text-muted-foreground flex items-center justify-between gap-2 text-[11px]">
              <span className="truncate font-medium tracking-wide uppercase">Klub Alumnów</span>
              <span className="shrink-0">teraz</span>
            </div>
            <label htmlFor="message-notification-title" className="sr-only">
              Tytuł powiadomienia
            </label>
            <input
              id="message-notification-title"
              value={title}
              maxLength={MESSAGE_LIMITS.notificationTitle}
              placeholder={eventTitle}
              className="text-foreground placeholder:text-muted-foreground/70 hover:border-muted-foreground/40 focus-visible:border-primary focus-visible:ring-primary/30 -mx-1 block w-[calc(100%+0.5rem)] truncate rounded-md border border-dashed border-transparent bg-transparent px-1 py-0.5 text-sm leading-snug font-semibold outline-none focus-visible:ring-2"
              onChange={(changeEvent) => onTitleChange(changeEvent.target.value)}
            />
            <label htmlFor="message-notification" className="sr-only">
              Treść powiadomienia
            </label>
            <textarea
              id="message-notification"
              rows={2}
              value={text}
              maxLength={MESSAGE_LIMITS.notification}
              placeholder="np. Spotkanie przeniesione do sali 2.41"
              className="text-foreground placeholder:text-muted-foreground/70 hover:border-muted-foreground/40 focus-visible:border-primary focus-visible:ring-primary/30 -mx-1 block w-[calc(100%+0.5rem)] resize-none rounded-md border border-dashed border-transparent bg-transparent px-1 py-0.5 text-sm leading-snug outline-none focus-visible:ring-2"
              onChange={(changeEvent) =>
                onTextChange(changeEvent.target.value.replace(/[\r\n]+/g, " "))
              }
            />
          </div>
        </div>
      </div>
      <CharacterCount value={text} limit={MESSAGE_LIMITS.notification} />
    </div>
  );
}
