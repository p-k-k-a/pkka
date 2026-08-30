import type { EventDetailsResponse } from "@pkka/api";
import * as Calendar from "expo-calendar";

export async function addEventToCalendar(event: EventDetailsResponse): Promise<boolean> {
  const { status } = await Calendar.requestCalendarPermissionsAsync();
  if (!status) return false;

  const { action } = await Calendar.createEventInCalendarAsync({
    title: event.title,
    startDate: new Date(event.startsAt),
    endDate: new Date(event.endsAt),
    location: event.location ?? undefined,
    notes: event.fullDescription ?? undefined,
  });

  return action !== Calendar.CalendarDialogResultActions.canceled;
}
