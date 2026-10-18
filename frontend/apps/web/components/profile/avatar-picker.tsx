"use client";

import { ImagePlus } from "lucide-react";
import { toast } from "sonner";
import { Avatar } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";

type AvatarPickerProps = {
  /** Name used for the initials shown in place of a picture. */
  fallback: string;
};

// Mock until the backend can store profile pictures: the button only announces
// that the feature is coming, so no picture is ever kept on the device.
export function AvatarPicker({ fallback }: AvatarPickerProps) {
  return (
    <div className="flex flex-col items-center gap-6 sm:flex-row sm:items-center">
      <Avatar
        fallback={fallback}
        alt="Zdjęcie profilowe"
        className="size-24 text-2xl font-semibold"
      />

      <div className="flex flex-col items-center gap-2 sm:items-start">
        <Button
          type="button"
          variant="outline"
          onClick={() => toast.info("Dodawanie zdjęcia profilowego będzie dostępne wkrótce.")}
        >
          <ImagePlus data-icon="inline-start" aria-hidden="true" />
          Zmień zdjęcie
        </Button>
        <p className="text-muted-foreground text-center text-[13px] sm:text-left">
          Dodawanie zdjęć jeszcze nie działa — na razie wyświetlamy Twoje inicjały.
        </p>
      </div>
    </div>
  );
}
