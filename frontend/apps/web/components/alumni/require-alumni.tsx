"use client";

import Link from "next/link";
import { ArrowRight, Lock } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useVerificationStatus } from "@/lib/use-verification-status";

type RequireAlumniProps = {
  children: React.ReactNode;
  title?: string;
  description?: string;
};

/** The alumni endpoints answer 403 to anyone without the VERIFIED_ALUMN role. */
export function RequireAlumni({
  children,
  title = "Katalog dostępny dla zweryfikowanych alumnów",
  description = "Po zatwierdzeniu wniosku zobaczysz profile innych absolwentów i będziesz mógł się z nimi skontaktować.",
}: RequireAlumniProps) {
  // isLoading, not isPending: the application query stays pending forever when
  // it is disabled for admins.
  const { isAuthLoading, isLoading, isVerified } = useVerificationStatus();

  if (isAuthLoading || isLoading) {
    return (
      <div className="mx-auto w-full max-w-[1280px] px-4 py-10 md:px-10">
        <Skeleton className="h-40 w-full rounded-xl" />
      </div>
    );
  }

  if (!isVerified) {
    return (
      <section className="bg-background px-4 py-16 md:px-10 md:py-20">
        <div className="mx-auto flex max-w-[720px] flex-col items-start gap-4">
          <Lock className="text-muted-foreground size-6" aria-hidden="true" />
          <h1 className="font-heading text-foreground text-[28px] leading-tight font-semibold">
            {title}
          </h1>
          <p className="text-muted-foreground leading-relaxed">{description}</p>
          <Button asChild size="xl" className="font-bold">
            <Link href="/dashboard/verification">
              Zweryfikuj się
              <ArrowRight data-icon="inline-end" />
            </Link>
          </Button>
        </div>
      </section>
    );
  }

  return children;
}
