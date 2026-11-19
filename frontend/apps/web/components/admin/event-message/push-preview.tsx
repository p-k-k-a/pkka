import { CharacterCount } from "@/components/admin/event-message/character-count";
import { MESSAGE_LIMITS } from "@/components/admin/event-message/message-format";

type PushPreviewProps = {
  eventTitle: string;
  text: string;
  onTextChange: (text: string) => void;
};

export function PushPreview({ eventTitle, text, onTextChange }: PushPreviewProps) {
  return (
    <div className="space-y-2">
      <div className="from-band to-accent rounded-[28px] bg-gradient-to-b p-3 pt-8 pb-10 shadow-inner">
        <div className="bg-background/95 flex gap-3 rounded-2xl p-3 shadow-lg backdrop-blur">
          <span
            aria-hidden
            className="bg-primary text-primary-foreground font-heading flex size-9 shrink-0 items-center justify-center rounded-lg text-sm font-bold"
          >
            KA
          </span>
          <div className="min-w-0 flex-1 space-y-0.5">
            <div className="text-muted-foreground flex items-center justify-between gap-2 text-[11px]">
              <span className="truncate font-medium tracking-wide uppercase">Klub Alumnów</span>
              <span className="shrink-0">teraz</span>
            </div>
            <p className="text-foreground truncate text-sm font-semibold">{eventTitle}</p>
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
