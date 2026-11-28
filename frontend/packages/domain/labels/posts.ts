import { PostAudience } from "@pkka/api";

const POST_AUDIENCE_LABELS: Record<PostAudience, string> = {
  PUBLIC: "Publiczne",
  ALUMNI: "Alumni",
};

export const POST_AUDIENCE_OPTIONS = Object.values(PostAudience).map((value) => ({
  value,
  label: POST_AUDIENCE_LABELS[value],
}));
