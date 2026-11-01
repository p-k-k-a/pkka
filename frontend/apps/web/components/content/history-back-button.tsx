"use client";

import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";

export function HistoryBackButton({ className }: { className?: string }) {
  const router = useRouter();
  return (
    <Button
      type="button"
      variant="ghost"
      size="sm"
      className={className}
      onClick={() => router.back()}
    >
      <ArrowLeft data-icon="inline-start" />
      Wróć
    </Button>
  );
}
