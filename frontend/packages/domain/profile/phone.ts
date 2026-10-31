// Mirrors the @Pattern constraint on UpdateProfileRequest.phoneNumber.
const PHONE_NUMBER = /^\+?[0-9 -]{7,32}$/;

export function phoneNumberError(raw: string): string | undefined {
  const phone = raw.trim();
  if (!phone || PHONE_NUMBER.test(phone)) return undefined;
  return "Podaj numer telefonu, np. +48 600 700 800";
}
