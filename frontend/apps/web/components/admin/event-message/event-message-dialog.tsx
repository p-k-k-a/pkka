"use client";

import { useId, useState, type ReactNode } from "react";
import { Mail, Smartphone, Users } from "lucide-react";
import { toast } from "sonner";
import { useCountEventMessageRecipients, useSendEventMessage } from "@pkka/api";
import { pluralPl } from "@pkka/domain";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { EmailPreview } from "@/components/admin/event-message/email-preview";
import { PushPreview } from "@/components/admin/event-message/push-preview";
import { formatMailStartsAt } from "@/components/admin/event-message/message-format";

export type MessagedEvent = {
  id: string;
  title: string;
  slug: string;
  startsAt: string;
};

type EventMessageDialogProps = {
  event: MessagedEvent;
  children: ReactNode;
};

const people = (count: number) => `${count} ${pluralPl(count, "osoby", "osób", "osób")}`;

export function EventMessageDialog({ event, children }: EventMessageDialogProps) {
  const [open, setOpen] = useState(false);
  const [notificationTitle, setNotificationTitle] = useState(event.title);
  const [notification, setNotification] = useState("");
  const [subject, setSubject] = useState("");
  const [body, setBody] = useState("");
  const descriptionId = useId();

  const recipientsQuery = useCountEventMessageRecipients(event.id, {
    query: { enabled: open, staleTime: 0 },
  });
  const recipients =
    recipientsQuery.data?.status === 200 ? recipientsQuery.data.data.recipients : undefined;

  const reset = () => {
    setNotificationTitle(event.title);
    setNotification("");
    setSubject("");
    setBody("");
  };

  const sendMessage = useSendEventMessage({
    mutation: {
      onSuccess: (response) => {
        const sentTo = response.status === 202 ? response.data.recipients : 0;
        toast.success(
          sentTo > 0
            ? `Wiadomość wysłana do ${people(sentTo)}`
            : "Nikt nie był zapisany, nic nie wysłano",
        );
        reset();
        setOpen(false);
      },
    },
  });

  const isDirty =
    notificationTitle !== event.title || notification !== "" || subject !== "" || body !== "";
  const isComplete = notification.trim() !== "" && subject.trim() !== "" && body.trim() !== "";
  const canSend =
    isComplete && recipients !== undefined && recipients > 0 && !sendMessage.isPending;

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (sendMessage.isPending) return;
        setOpen(next);
        if (!next) sendMessage.reset();
      }}
    >
      <DialogTrigger asChild>{children}</DialogTrigger>
      <DialogContent
        aria-describedby={descriptionId}
        className="max-w-[calc(100%-2rem)] gap-6 lg:max-w-5xl"
        onInteractOutside={(interaction) => {
          if (isDirty) interaction.preventDefault();
        }}
      >
        <DialogHeader>
          <DialogTitle>Wiadomość do zapisanych</DialogTitle>
          <DialogDescription id={descriptionId}>
            {event.title}, {formatMailStartsAt(event.startsAt)}
          </DialogDescription>
        </DialogHeader>

        <RecipientsNote
          isLoading={recipientsQuery.isLoading}
          isError={recipientsQuery.isError}
          recipients={recipients}
          onRetry={() => recipientsQuery.refetch()}
        />

        <div className="grid grid-cols-1 items-start gap-8 lg:grid-cols-[minmax(0,340px)_minmax(0,1fr)]">
          <section aria-labelledby="push-heading" className="space-y-3">
            <ChannelHeading id="push-heading" icon={<Smartphone />} title="Powiadomienie push" />
            <PushPreview
              eventTitle={event.title}
              title={notificationTitle}
              text={notification}
              onTitleChange={setNotificationTitle}
              onTextChange={setNotification}
            />
          </section>

          <section aria-labelledby="email-heading" className="space-y-3">
            <ChannelHeading id="email-heading" icon={<Mail />} title="E-mail" />
            <EmailPreview
              subject={subject}
              body={body}
              onSubjectChange={setSubject}
              onBodyChange={setBody}
            />
          </section>
        </div>

        {sendMessage.isError ? (
          <p role="alert" className="text-destructive text-sm font-medium">
            Nie udało się wysłać wiadomości. Spróbuj ponownie.
          </p>
        ) : null}

        <DialogFooter className="bg-popover border-border sticky -bottom-4 -mx-4 -mb-4 border-t px-4 py-3 sm:-bottom-6 sm:-mx-6 sm:-mb-6 sm:px-6">
          <Button
            type="button"
            variant="outline"
            disabled={sendMessage.isPending}
            onClick={() => {
              setOpen(false);
              sendMessage.reset();
            }}
          >
            Anuluj
          </Button>
          <Button
            type="button"
            disabled={!canSend}
            onClick={() =>
              sendMessage.mutate({
                eventId: event.id,
                data: {
                  notificationTitle: notificationTitle.trim() || undefined,
                  notification: notification.trim(),
                  subject: subject.trim(),
                  body: body.trim(),
                },
              })
            }
          >
            {sendMessage.isPending
              ? "Wysyłanie…"
              : recipients
                ? `Wyślij do ${people(recipients)}`
                : "Wyślij"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function RecipientsNote({
  isLoading,
  isError,
  recipients,
  onRetry,
}: {
  isLoading: boolean;
  isError: boolean;
  recipients: number | undefined;
  onRetry: () => void;
}) {
  if (isLoading) return <Skeleton className="h-11 w-full rounded-lg" />;
  if (isError || recipients === undefined) {
    return (
      <div role="alert" className="text-destructive flex items-center gap-3 text-sm font-medium">
        Nie udało się sprawdzić, do ilu osób trafi wiadomość.
        <Button type="button" variant="outline" size="sm" onClick={onRetry}>
          Spróbuj ponownie
        </Button>
      </div>
    );
  }
  return (
    <p className="bg-accent/10 text-foreground flex items-center gap-2 rounded-lg px-3 py-2.5 text-sm">
      <Users className="text-accent size-4 shrink-0" aria-hidden />
      {recipients > 0 ? (
        <span>
          Wiadomość trafi do <strong className="tabular-nums">{people(recipients)}</strong>{" "}
          zapisanych na wydarzenie, włącznie z listą rezerwową.
        </span>
      ) : (
        <span>Nikt nie jest zapisany na to wydarzenie, więc nie ma do kogo wysłać wiadomości.</span>
      )}
    </p>
  );
}

function ChannelHeading({ id, icon, title }: { id: string; icon: ReactNode; title: string }) {
  return (
    <h3
      id={id}
      className="text-foreground [&_svg]:text-muted-foreground flex items-center gap-2 text-sm font-semibold [&_svg]:size-4"
    >
      {icon}
      {title}
    </h3>
  );
}
