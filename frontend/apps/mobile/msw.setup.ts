import { faker } from "@faker-js/faker";
import type { Announcement, BlogPost, Event } from "@pkka/api";

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

const generateMockBlogPost = (): BlogPost => ({
  id: faker.string.uuid(),
  title: faker.lorem.sentence(),
  description: faker.lorem.sentence(),
  content: faker.lorem.paragraphs(3, "\n\n"),
  publishedAt: faker.date.recent().toISOString(),
  tag: faker.helpers.arrayElement(["Wydarzenie", "Warsztaty", "Konferencja", "Aktualności"]),
  imageUrl: faker.image.url(),
  slug: faker.lorem.slug(),
  author: {
    name: faker.person.fullName(),
    role: faker.person.jobTitle(),
    avatarUrl: faker.image.avatar(),
  },
});

const generateMockEvents = (): Event[] =>
  Array.from({ length: faker.number.int({ min: 2, max: 5 }) }, () => {
    const format = faker.helpers.arrayElement(["Stacjonarnie", "Online"]);
    return {
      id: faker.string.uuid(),
      title: faker.lorem.sentence(),
      date: faker.date
        .future()
        .toLocaleDateString("pl-PL", { day: "numeric", month: "long", year: "numeric" }),
      format,
      access: "Publiczne",
      location:
        format === "Online"
          ? "Link do transmisji (MS Teams)"
          : `Budynek D-${faker.number.int({ min: 1, max: 20 })}, Sala ${faker.number.int({
              min: 1,
              max: 4,
            })}.${faker.number.int({ min: 10, max: 30 })}`,
    };
  });

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

  if (url.includes("/api/blog-posts/")) {
    return new Response(JSON.stringify(generateMockBlogPost()), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  }

  if (url.includes("/api/events")) {
    return new Response(JSON.stringify(generateMockEvents()), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  }

  return _originalFetch(input as RequestInfo, init);
};
