"use client";

import { ApplicationResponseDtoStatus, useGetMine, type ApplicationResponseDto } from "@pkka/api";
import { useAuth } from "@/lib/auth-context";
import { isAdmin, isVerifiedAlumn } from "@/lib/roles";

export function useVerificationStatus() {
  const { user, isAuthenticated, isLoading: isAuthLoading } = useAuth();
  const admin = isAdmin(user?.roles);
  const verifiedByRole = isVerifiedAlumn(user?.roles);

  const query = useGetMine({
    query: {
      enabled: isAuthenticated && !admin && !verifiedByRole,
      retry: false,
    },
  });

  const application = query.data?.data as ApplicationResponseDto | undefined;
  const verifiedByApplication = application?.status === ApplicationResponseDtoStatus.APPROVED;

  return {
    ...query,
    isAuthLoading,
    isAuthenticated,
    user,
    admin,
    application,
    isVerified: verifiedByRole || verifiedByApplication,
    isRejected: application?.status === ApplicationResponseDtoStatus.REJECTED,
  };
}
