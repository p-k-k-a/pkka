import type { AlumniProfileResponse, ProfileResponse } from "@pkka/api";

/**
 * The viewer's own profile (`/api/profiles/me`) and another alumn's public profile
 * (`/api/alumni/{id}`) carry the same fields, so the profile presentation components
 * accept either. Both null out whatever the owner's visibility settings hide.
 */
export type AlumnProfile = ProfileResponse | AlumniProfileResponse;
