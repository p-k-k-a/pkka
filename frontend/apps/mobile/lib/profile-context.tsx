import { MOCK_ALUMN_PROFILE, type AlumnProfile } from "@/lib/alumni-mock";
import { createContext, useContext, useState } from "react";

type ProfileContextType = {
  profile: AlumnProfile;
  updateProfile: (patch: Partial<AlumnProfile>) => void;
};

const ProfileContext = createContext<ProfileContextType | null>(null);

// In-memory only: seeded from the mock and reset on app reload. Stands in for a
// real profile endpoint so edits (fields + visibility) reflect live in the UI.
export function ProfileProvider({ children }: { children: React.ReactNode }) {
  const [profile, setProfile] = useState<AlumnProfile>(MOCK_ALUMN_PROFILE);
  const updateProfile = (patch: Partial<AlumnProfile>) =>
    setProfile((prev) => ({ ...prev, ...patch }));

  return <ProfileContext value={{ profile, updateProfile }}>{children}</ProfileContext>;
}

export function useProfile() {
  const ctx = useContext(ProfileContext);
  if (!ctx) throw new Error("useProfile must be used within ProfileProvider");
  return ctx;
}
