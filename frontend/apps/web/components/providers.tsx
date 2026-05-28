"use client";

import { configureApi } from "@pkka/api";
import { QueryClientProvider } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { getQueryClient } from "@/lib/query-client";
import { ensureDevMsw, isMswReady } from "@/lib/msw/ensure-dev-msw";

configureApi({ baseUrl: "" });

const isDev = process.env.NODE_ENV === "development";

export function Providers({ children }: { children: React.ReactNode }) {
  const [ready, setReady] = useState(isMswReady());
  const queryClient = getQueryClient();

  useEffect(() => {
    if (!isDev) return;
    if (isMswReady()) {
      setReady(true);
      return;
    }
    void ensureDevMsw().then(() => setReady(true));
  }, []);

  if (!ready) {
    return null;
  }

  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
