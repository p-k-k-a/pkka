export const SURVEYS_PATH = "/dashboard/surveys";

export function surveyHref(id: string) {
  return `${SURVEYS_PATH}/${id}`;
}

export const ADMIN_SURVEYS_PATH = "/dashboard/admin/surveys";

export function adminSurveyHref(id: string) {
  return `${ADMIN_SURVEYS_PATH}/${id}`;
}

export function adminSurveyEditHref(id: string) {
  return `${ADMIN_SURVEYS_PATH}/${id}/edit`;
}
