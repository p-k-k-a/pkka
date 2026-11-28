import { PostAudience } from "@pkka/api";

const POST_AUDIENCE_LABELS: Record<PostAudience, string> = {
  PUBLIC: "Wszyscy",
  ALUMNI: "Tylko zweryfikowani alumni",
};

export const ALUMNI_ONLY_POST_LABEL = "Dla alumnów";

export const POST_AUDIENCE_OPTIONS = Object.values(PostAudience).map((value) => ({
  value,
  label: POST_AUDIENCE_LABELS[value],
}));
