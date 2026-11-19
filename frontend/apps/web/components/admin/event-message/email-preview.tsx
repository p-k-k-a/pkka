import { cn } from "@/lib/utils";
import { CharacterCount } from "@/components/admin/event-message/character-count";
import {
  formatMailStartsAt,
  MESSAGE_LIMITS,
} from "@/components/admin/event-message/message-format";
import type { MessagedEvent } from "@/components/admin/event-message/event-message-dialog";

type EmailPreviewProps = {
  event: MessagedEvent;
  subject: string;
  body: string;
  onSubjectChange: (subject: string) => void;
  onBodyChange: (body: string) => void;
};

const editable =
  "hover:border-muted-foreground/40 focus-visible:border-primary focus-visible:ring-primary/30 placeholder:text-muted-foreground/70 -mx-2 block w-[calc(100%+1rem)] rounded-md border border-dashed border-transparent bg-transparent px-2 outline-none focus-visible:ring-2";

export function EmailPreview({
  event,
  subject,
  body,
  onSubjectChange,
  onBodyChange,
}: EmailPreviewProps) {
  return (
    <div className="border-border overflow-hidden rounded-xl border">
      <dl className="bg-background border-border grid grid-cols-[auto_1fr] gap-x-3 gap-y-1 border-b px-4 py-3 text-xs">
        <dt className="text-muted-foreground">Od</dt>
        <dd className="text-foreground truncate">Klub Alumnów WI AGH</dd>
        <dt className="text-muted-foreground">Temat</dt>
        <dd className="text-foreground truncate font-semibold">
          {subject.trim() || <span className="text-muted-foreground font-normal">bez tematu</span>}
        </dd>
      </dl>

      <div className="bg-muted p-3 sm:p-5">
        <div className="bg-background mx-auto max-w-[560px] overflow-hidden rounded-xl">
          <div className="bg-band text-band-foreground px-6 py-4 text-base font-bold sm:px-8">
            Klub Alumnów WI AGH
          </div>

          <div className="text-muted-foreground space-y-4 px-6 py-6 text-[15px] leading-relaxed sm:px-8">
            <p className="text-[13px]">
              Wiadomość do zapisanych na wydarzenie{" "}
              <strong className="text-foreground">{event.title}</strong> (
              {formatMailStartsAt(event.startsAt)})
            </p>

            <div>
              <label htmlFor="message-subject" className="sr-only">
                Temat maila
              </label>
              <input
                id="message-subject"
                value={subject}
                maxLength={MESSAGE_LIMITS.subject}
                placeholder="Temat, np. Zmiana sali"
                className={cn(editable, "text-foreground py-1 text-xl font-bold")}
                onChange={(changeEvent) => onSubjectChange(changeEvent.target.value)}
              />
              <CharacterCount value={subject} limit={MESSAGE_LIMITS.subject} />
            </div>

            <div>
              <label htmlFor="message-body" className="sr-only">
                Treść maila
              </label>
              <textarea
                id="message-body"
                rows={7}
                value={body}
                maxLength={MESSAGE_LIMITS.body}
                placeholder="Treść wiadomości. Nowe linie zostaną zachowane."
                className={cn(editable, "min-h-40 resize-y py-1")}
                onChange={(changeEvent) => onBodyChange(changeEvent.target.value)}
              />
              <CharacterCount value={body} limit={MESSAGE_LIMITS.body} />
            </div>

            <span
              aria-hidden
              className="bg-primary text-primary-foreground inline-block rounded-lg px-5 py-3 text-sm font-bold"
            >
              Szczegóły wydarzenia
            </span>
          </div>

          <div className="border-muted text-muted-foreground border-t px-6 py-4 text-xs leading-normal sm:px-8">
            Masz pytania? Odpowiedz na tę wiadomość.
            <br />
            <span className="text-accent">pkka.me</span>
          </div>
        </div>
      </div>
    </div>
  );
}
