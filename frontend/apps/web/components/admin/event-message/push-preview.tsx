import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
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
          <div className="min-w-0 space-y-1.5">
            <div className="text-muted-foreground flex items-center justify-between gap-2 text-[11px]">
              <span className="truncate font-medium tracking-wide uppercase">Klub Alumnów</span>
              <span className="shrink-0">teraz</span>
            </div>
            <label htmlFor="message-notification-title" className="sr-only">
              Tytuł powiadomienia
            </label>
            <Input
              id="message-notification-title"
              value={title}
              maxLength={MESSAGE_LIMITS.notificationTitle}
              placeholder={eventTitle}
              className="h-auto px-2 py-1 font-semibold"
              onChange={(changeEvent) => onTitleChange(changeEvent.target.value)}
            />
            <label htmlFor="message-notification" className="sr-only">
              Treść powiadomienia
            </label>
            <Textarea
              id="message-notification"
              rows={2}
              value={text}
              maxLength={MESSAGE_LIMITS.notification}
              placeholder="np. Spotkanie przeniesione do sali 2.41"
              className="min-h-0 resize-none px-2 py-1"
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
