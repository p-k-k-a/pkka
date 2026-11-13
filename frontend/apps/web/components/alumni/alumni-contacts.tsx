"use client";

import { useState } from "react";
import Link from "next/link";
import { UserMinus } from "lucide-react";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";
import {
  type ContactResponse,
  getGetAlumniProfileQueryKey,
  getListContactsQueryKey,
  useAddContact,
  useListContacts,
  useRemoveContact,
} from "@pkka/api";
import { getProfileContacts } from "@pkka/domain";
import { DiscordIcon } from "@pkka/icons/web";
import { AlumniSectionTabs } from "@/components/alumni/alumni-section-tabs";
import { Avatar } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { ALUMNI_DIRECTORY_HREF } from "@/lib/alumni-search-params";

function contactName(contact: ContactResponse) {
  return [contact.firstName, contact.lastName].filter(Boolean).join(" ") || "Alumn";
}

export function AlumniContacts() {
  const queryClient = useQueryClient();
  const { data, isPending, isError, refetch } = useListContacts();
  const addContact = useAddContact();
  const removeContact = useRemoveContact();
  const [removingId, setRemovingId] = useState<string | null>(null);
  const contacts = data?.status === 200 ? data.data : [];

  const refresh = (alumnId: string) =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: getListContactsQueryKey() }),
      queryClient.invalidateQueries({
        queryKey: getGetAlumniProfileQueryKey(alumnId),
        refetchType: "all",
      }),
    ]);

  const restore = async (alumnId: string) => {
    try {
      await addContact.mutateAsync({ alumnId });
    } catch {
      toast.error("Nie udało się przywrócić kontaktu.");
    } finally {
      await refresh(alumnId);
    }
  };

  const remove = async (contact: ContactResponse) => {
    setRemovingId(contact.id);
    try {
      await removeContact.mutateAsync({ alumnId: contact.id });
      toast.success(`Usunięto z kontaktów: ${contactName(contact)}`, {
        action: { label: "Cofnij", onClick: () => void restore(contact.id) },
      });
    } catch {
      toast.error("Nie udało się usunąć kontaktu.");
    } finally {
      await refresh(contact.id);
      setRemovingId(null);
    }
  };

  return (
    <div className="flex flex-col">
      <section className="bg-background px-4 pt-10 pb-6 md:px-10 md:pt-16 md:pb-8">
        <div className="mx-auto flex max-w-[1280px] flex-col gap-6">
          <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
            Alumni
          </h1>
          <AlumniSectionTabs />
          <p className="text-muted-foreground max-w-2xl text-base leading-relaxed">
            Widzisz je tylko Ty. Dodajesz je przyciskiem na profilu alumna.
          </p>
        </div>
      </section>

      <section className="bg-background px-4 pb-10 md:px-10 md:pb-20">
        <div className="mx-auto max-w-[1280px]">
          {isPending ? (
            <div className="flex max-w-3xl flex-col gap-4">
              {Array.from({ length: 3 }).map((_, i) => (
                <Skeleton key={i} className="h-16 w-full rounded-lg" />
              ))}
            </div>
          ) : isError ? (
            <div className="flex flex-col items-start gap-3">
              <p className="text-destructive font-medium">Nie udało się załadować kontaktów.</p>
              <Button type="button" variant="outline" onClick={() => void refetch()}>
                Spróbuj ponownie
              </Button>
            </div>
          ) : contacts.length === 0 ? (
            <div className="flex max-w-xl flex-col items-start gap-4">
              <h2 className="font-heading text-foreground text-[23px] font-semibold">
                Nie masz jeszcze kontaktów
              </h2>
              <p className="text-muted-foreground">
                Dodawaj alumnów przyciskiem „Dodaj do kontaktów” na ich profilu.
              </p>
              <Button asChild variant="outline">
                <Link href={ALUMNI_DIRECTORY_HREF}>Przejdź do katalogu</Link>
              </Button>
            </div>
          ) : (
            <ul className="border-muted-foreground/25 dark:border-input max-w-3xl border-t">
              {contacts.map((contact) => (
                <ContactRow
                  key={contact.id}
                  contact={contact}
                  removing={removingId === contact.id}
                  onRemove={() => void remove(contact)}
                />
              ))}
            </ul>
          )}
        </div>
      </section>
    </div>
  );
}

type ContactRowProps = {
  contact: ContactResponse;
  removing: boolean;
  onRemove: () => void;
};

function ContactRow({ contact, removing, onRemove }: ContactRowProps) {
  const name = contactName(contact);
  const role = [contact.currentPosition, contact.company].filter(Boolean).join(" @ ");
  const details = [role, contact.graduationYear ? `Rocznik ${contact.graduationYear}` : null]
    .filter(Boolean)
    .join(" · ");
  const { discordUrl } = getProfileContacts(contact);

  return (
    <li className="border-muted-foreground/25 dark:border-input flex flex-wrap items-center gap-4 border-b py-4">
      <Avatar fallback={name} alt="" />
      <div className="min-w-48 flex-1">
        <Link
          href={`${ALUMNI_DIRECTORY_HREF}/${contact.id}`}
          className="font-heading text-foreground hover:text-accent focus-visible:ring-ring rounded-sm text-base font-semibold underline-offset-4 hover:underline focus-visible:ring-2 focus-visible:outline-none"
        >
          {name}
        </Link>
        {details ? <p className="text-muted-foreground mt-0.5 text-sm">{details}</p> : null}
      </div>
      <div className="ml-auto flex items-center gap-2">
        {discordUrl ? (
          <Button asChild variant="outline" size="sm">
            <a href={discordUrl} target="_blank" rel="noopener noreferrer">
              <DiscordIcon className="size-4" />
              Napisz na Discordzie
            </a>
          </Button>
        ) : null}
        <Button
          type="button"
          variant="ghost"
          size="icon"
          aria-label={`Usuń z kontaktów: ${name}`}
          title="Usuń z kontaktów"
          disabled={removing}
          onClick={onRemove}
          className="text-muted-foreground hover:text-destructive"
        >
          <UserMinus />
        </Button>
      </div>
    </li>
  );
}
