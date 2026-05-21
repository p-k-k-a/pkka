import { faker } from "@faker-js/faker";
import type { Announcement } from "@pkka/api";

const generateMockAnnouncements = (): Announcement[] =>
  Array.from({ length: faker.number.int({ min: 2, max: 5 }) }, () => ({
    id: faker.string.uuid(),
    title: faker.lorem.sentence(),
    description: faker.lorem.sentence(),
    content: faker.lorem.paragraphs(),
    date: faker.date.recent().toLocaleDateString("pl-PL"),
    time: faker.date.recent().toLocaleTimeString("pl-PL", { hour: "2-digit", minute: "2-digit" }),
    tag: faker.helpers.arrayElement(["Konferencja", "Wydarzenie", "Warsztaty"]),
    imageUrl: faker.image.url(),
    slug: faker.lorem.slug(),
    author: {
      name: faker.person.fullName(),
      role: faker.person.jobTitle(),
      avatarUrl: faker.image.avatar(),
    },
  }));

const _originalFetch = global.fetch;

global.fetch = async function (input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  const url =
    typeof input === "string" ? input : input instanceof URL ? input.href : (input as Request).url;

  if (url.includes("/api/announcements")) {
    return new Response(JSON.stringify(generateMockAnnouncements()), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  }

  return _originalFetch(input as RequestInfo, init);
};
