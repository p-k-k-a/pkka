"use client";

import { useState } from "react";
import { Trash2 } from "lucide-react";
import { ApiError, useDeleteMyAccount } from "@pkka/api";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export function DeleteAccount() {
  const [open, setOpen] = useState(false);
  const [email, setEmail] = useState("");
  const deleteAccount = useDeleteMyAccount({
    mutation: { onSuccess: () => window.location.assign("/") },
  });
  const busy = deleteAccount.isPending || deleteAccount.isSuccess;

  const close = () => {
    if (busy) return;
    setOpen(false);
    setEmail("");
    deleteAccount.reset();
  };

  return (
    <>
      <Button
        type="button"
        variant="ghost"
        className="text-destructive hover:text-destructive -ml-3"
        onClick={() => setOpen(true)}
      >
        <Trash2 data-icon="inline-start" />
        Usuń konto
      </Button>

      <AlertDialog open={open} onOpenChange={(next) => !next && close()}>
        <AlertDialogContent onOverlayClick={close}>
          <AlertDialogHeader>
            <AlertDialogTitle>Usunąć konto?</AlertDialogTitle>
            <AlertDialogDescription>
              Trwale usuniemy Twoje konto i wszystkie dane: profil, wnioski, zgłoszenia, zapisy na
              wydarzenia i odpowiedzi w ankietach. Tej operacji nie można cofnąć.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <div className="flex flex-col gap-2">
            <Label htmlFor="delete-account-email">Wpisz swój adres e-mail, aby potwierdzić</Label>
            <Input
              id="delete-account-email"
              type="email"
              autoComplete="off"
              value={email}
              onChange={(event) => {
                setEmail(event.target.value);
                deleteAccount.reset();
              }}
              placeholder="adres@email.com"
              disabled={busy}
            />
            {deleteAccount.isError ? (
              <p className="text-destructive text-sm font-medium">
                {deleteAccount.error instanceof ApiError && deleteAccount.error.status === 400
                  ? "Ten adres e-mail nie pasuje do Twojego konta."
                  : "Nie udało się usunąć konta. Spróbuj ponownie."}
              </p>
            ) : null}
          </div>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={busy}>Anuluj</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              disabled={!email.trim() || busy}
              onClick={(event) => {
                event.preventDefault();
                deleteAccount.mutate({ data: { email: email.trim() } });
              }}
            >
              {busy ? "Usuwanie…" : "Usuń konto na zawsze"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
