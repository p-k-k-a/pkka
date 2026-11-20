export const ADMIN_SURVEYS_PATH = "/dashboard/admin/surveys";

export function adminSurveyHref(id: string) {
  return `${ADMIN_SURVEYS_PATH}/${id}`;
}

export function adminSurveyEditHref(id: string) {
  return `${ADMIN_SURVEYS_PATH}/${id}/edit`;
}
