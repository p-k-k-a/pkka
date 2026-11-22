import { CharacterCount } from "@/components/admin/event-message/character-count";
import { MESSAGE_LIMITS } from "@/components/admin/event-message/message-format";

type EmailPreviewProps = {
  subject: string;
  body: string;
  onSubjectChange: (subject: string) => void;
  onBodyChange: (body: string) => void;
};

export function EmailPreview({ subject, body, onSubjectChange, onBodyChange }: EmailPreviewProps) {
  return (
    <div className="overflow-hidden rounded-xl border border-[#E2E8F0] bg-white scheme-light">
      <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1 border-b border-[#E2E8F0] bg-white px-4 py-3 text-xs">
        <dt className="text-[#486284]">Od</dt>
        <dd className="truncate text-[#16212F]">Klub Alumnów WI AGH</dd>
        <dt className="text-[#486284]">Temat</dt>
        <dd className="truncate font-semibold text-[#16212F]">
          {subject.trim() || <span className="font-normal text-[#486284]">bez tematu</span>}
        </dd>
      </dl>

      <div className="bg-[#EDF1FA] p-3 sm:p-5">
        <div className="mx-auto max-w-[560px] overflow-hidden rounded-xl bg-white">
          <div className="bg-[#022749] px-6 py-4 text-base font-bold text-white sm:px-8">
            Klub Alumnów WI AGH
          </div>

          <div className="space-y-3 px-6 py-5 text-[15px] leading-relaxed text-[#486284] sm:px-8">
            <div>
              <label htmlFor="message-subject" className="sr-only">
                Temat maila
              </label>
              <input
                id="message-subject"
                value={subject}
                maxLength={MESSAGE_LIMITS.subject}
                placeholder="Temat, np. Zmiana sali"
                className="focus-visible:border-primary focus-visible:ring-primary/30 -mx-2 block w-[calc(100%+1rem)] rounded-md border border-dashed border-transparent bg-transparent px-2 py-1 text-xl font-bold text-[#16212F] outline-none placeholder:text-[#486284]/70 hover:border-[#486284]/40 focus-visible:ring-2"
                onChange={(changeEvent) => onSubjectChange(changeEvent.target.value)}
              />
              <CharacterCount
                value={subject}
                limit={MESSAGE_LIMITS.subject}
                className="text-[#486284]"
              />
            </div>

            <div>
              <label htmlFor="message-body" className="sr-only">
                Treść maila
              </label>
              <textarea
                id="message-body"
                rows={5}
                value={body}
                maxLength={MESSAGE_LIMITS.body}
                placeholder="Treść wiadomości. Nowe linie zostaną zachowane."
                className="focus-visible:border-primary focus-visible:ring-primary/30 -mx-2 block min-h-28 w-[calc(100%+1rem)] resize-y rounded-md border border-dashed border-transparent bg-transparent px-2 py-1 outline-none placeholder:text-[#486284]/70 hover:border-[#486284]/40 focus-visible:ring-2"
                onChange={(changeEvent) => onBodyChange(changeEvent.target.value)}
              />
              <CharacterCount value={body} limit={MESSAGE_LIMITS.body} className="text-[#486284]" />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
