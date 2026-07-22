"use client";

import { ProfileEditForm } from "@/components/profile/profile-edit-form";

export function ProfileEditContent() {
  return (
    <section className="bg-background px-4 py-10 md:px-10 md:py-20">
      <div className="mx-auto max-w-[1280px]">
        <ProfileEditForm />
      </div>
    </section>
  );
}
