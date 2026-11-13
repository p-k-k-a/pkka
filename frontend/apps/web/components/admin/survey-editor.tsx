"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useGetSurvey } from "@pkka/api";
import { isAdmin } from "@pkka/domain";
import { SurveyForm } from "@/components/admin/survey-form";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/lib/auth-context";
import { adminSurveyHref } from "@/lib/survey-paths";

type SurveyEditorProps = {
  id?: string;
};

export function SurveyEditor({ id }: SurveyEditorProps) {
  const router = useRouter();
  const { isLoading, user } = useAuth();
  const admin = isAdmin(user?.roles);

  useEffect(() => {
    if (!isLoading && !admin) {
      router.replace("/dashboard");
    }
  }, [admin, isLoading, router]);

  // The form copies the survey into its own state once, so it must start from a fresh
  // response rather than a cached one, and nothing may refetch under it afterwards.
  const {
    data: response,
    isError,
    isFetchedAfterMount,
  } = useGetSurvey(id ?? "", {
    query: { enabled: admin && id !== undefined, refetchOnMount: "always", staleTime: Infinity },
  });

  if (isLoading || !admin || (id !== undefined && !isFetchedAfterMount)) {
    return (
      <div className="px-4 py-10 md:px-10">
        <div className="mx-auto max-w-[1280px] space-y-6">
          <Skeleton className="h-10 w-64 rounded-lg" />
          <Skeleton className="h-96 w-full rounded-2xl" />
        </div>
      </div>
    );
  }

  if (id === undefined) {
    return <SurveyForm />;
  }

  const survey = response?.status === 200 ? response.data : undefined;
  if (isError || !survey) {
    return (
      <div className="px-4 py-16 text-center">
        <p className="text-destructive font-medium">Nie udało się załadować ankiety.</p>
      </div>
    );
  }

  if (survey.submissionCount > 0) {
    return (
      <div className="space-y-6 px-4 py-16 text-center">
        <p className="text-foreground font-medium">
          Tę ankietę ktoś już wypełnił, więc jej pytań nie można zmieniać.
        </p>
        <Button asChild variant="outline">
          <Link href={adminSurveyHref(survey.id)}>Przejdź do wyników</Link>
        </Button>
      </div>
    );
  }

  return <SurveyForm survey={survey} />;
}
