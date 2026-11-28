"use client";

import { useState } from "react";
import { UserCheck, UserMinus, UserPlus } from "lucide-react";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";
import {
  getGetAlumniProfileQueryKey,
  getListContactsQueryKey,
  useAddContact,
  useRemoveContact,
} from "@pkka/api";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

type ContactToggleButtonProps = {
  alumnId: string;
  isContact: boolean;
};

export function ContactToggleButton({ alumnId, isContact }: ContactToggleButtonProps) {
  const queryClient = useQueryClient();
  const addContact = useAddContact();
  const removeContact = useRemoveContact();
  const [pending, setPending] = useState(false);

  const onClick = async () => {
    setPending(true);
    try {
      if (isContact) {
        await removeContact.mutateAsync({ alumnId });
      } else {
        await addContact.mutateAsync({ alumnId });
      }
    } catch {
      toast.error(
        isContact ? "Nie udało się usunąć z kontaktów." : "Nie udało się dodać do kontaktów.",
      );
    } finally {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: getGetAlumniProfileQueryKey(alumnId) }),
        queryClient.invalidateQueries({ queryKey: getListContactsQueryKey(), refetchType: "all" }),
      ]);
      setPending(false);
    }
  };

  return (
    <Button
      type="button"
      size="xl"
      variant={isContact ? "secondary" : "outline"}
      disabled={pending}
      onClick={() => void onClick()}
      className={cn(
        "min-w-56",
        isContact && "hover:text-destructive focus-visible:text-destructive",
      )}
    >
      {isContact ? (
        <>
          <span className="inline-flex items-center gap-2 group-hover/button:hidden group-focus-visible/button:hidden">
            <UserCheck aria-hidden="true" />W kontaktach
          </span>
          <span className="hidden items-center gap-2 group-hover/button:inline-flex group-focus-visible/button:inline-flex">
            <UserMinus aria-hidden="true" />
            Usuń z kontaktów
          </span>
        </>
      ) : (
        <>
          <UserPlus data-icon="inline-start" />
          Dodaj do kontaktów
        </>
      )}
    </Button>
  );
}
