"use client";

import { useEffect } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { SIGN_UP_REF_PARAM, signUpSourceFromRef } from "@pkka/domain";
import { rememberSignUpSource } from "@/lib/signup-source";

export function useCaptureSignUpSource(slug: string) {
  const searchParams = useSearchParams();
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    const ref = searchParams.get(SIGN_UP_REF_PARAM);
    if (ref === null) return;
    if (signUpSourceFromRef(ref)) rememberSignUpSource(slug, ref);

    const rest = new URLSearchParams(searchParams);
    rest.delete(SIGN_UP_REF_PARAM);
    const query = rest.toString();
    router.replace(`${pathname}${query ? `?${query}` : ""}${window.location.hash}`, {
      scroll: false,
    });
  }, [searchParams, pathname, router, slug]);
}
