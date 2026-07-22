/**
 * Client-only stand-ins for profile fields the backend does not expose yet
 * (bio, visibility, education, discord). Persisted in localStorage per Keycloak
 * subject so view ↔ edit round-trips work until real endpoints land.
 */

export type ProfileVisibility = {
  name: boolean;
  email: boolean;
  discord: boolean;
};

export type ProfileMockFields = {
  bio?: string;
  discordId?: string;
  graduationYear?: number;
  fieldOfStudy?: string;
  alumnSince?: number;
  visibility: ProfileVisibility;
};

const DEFAULT_MOCK: ProfileMockFields = {
  bio: undefined,
  discordId: undefined,
  graduationYear: undefined,
  fieldOfStudy: undefined,
  alumnSince: undefined,
  visibility: {
    name: true,
    email: true,
    discord: false,
  },
};

function storageKey(userSub: string) {
  return `pkka.profile-mock.${userSub}`;
}

export function loadProfileMock(userSub: string): ProfileMockFields {
  if (typeof window === "undefined") return DEFAULT_MOCK;

  try {
    const raw = window.localStorage.getItem(storageKey(userSub));
    if (!raw) return { ...DEFAULT_MOCK, visibility: { ...DEFAULT_MOCK.visibility } };

    const parsed = JSON.parse(raw) as Partial<ProfileMockFields>;
    return {
      bio: typeof parsed.bio === "string" && parsed.bio.trim() ? parsed.bio.trim() : undefined,
      discordId:
        typeof parsed.discordId === "string" && parsed.discordId.trim()
          ? parsed.discordId.trim()
          : undefined,
      graduationYear:
        typeof parsed.graduationYear === "number" ? parsed.graduationYear : undefined,
      fieldOfStudy:
        typeof parsed.fieldOfStudy === "string" && parsed.fieldOfStudy.trim()
          ? parsed.fieldOfStudy.trim()
          : undefined,
      alumnSince: typeof parsed.alumnSince === "number" ? parsed.alumnSince : undefined,
      visibility: {
        name: parsed.visibility?.name ?? true,
        email: parsed.visibility?.email ?? true,
        discord: parsed.visibility?.discord ?? false,
      },
    };
  } catch {
    return { ...DEFAULT_MOCK, visibility: { ...DEFAULT_MOCK.visibility } };
  }
}

export function saveProfileMock(userSub: string, fields: ProfileMockFields) {
  if (typeof window === "undefined") return;

  const normalized: ProfileMockFields = {
    bio: fields.bio?.trim() || undefined,
    discordId: fields.discordId?.trim() || undefined,
    graduationYear: fields.graduationYear,
    fieldOfStudy: fields.fieldOfStudy?.trim() || undefined,
    alumnSince: fields.alumnSince,
    visibility: { ...fields.visibility },
  };

  window.localStorage.setItem(storageKey(userSub), JSON.stringify(normalized));
}

/** Profile URLs are user-supplied — only allow https before rendering as links. */
export function isHttpsUrl(url: string) {
  return /^https:\/\//i.test(url);
}
