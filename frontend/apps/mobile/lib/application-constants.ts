import { facultyLabel, meetingPreferenceLabel, studyTypeLabel, toOptions } from "@pkka/domain";
import { Faculty, MeetingPreference, StudyType } from "@pkka/api";

export const FACULTIES = toOptions(Faculty, facultyLabel);

export const STUDY_TYPES = toOptions(StudyType, studyTypeLabel);

export const MEETING_FORMATS = toOptions(MeetingPreference, meetingPreferenceLabel);
