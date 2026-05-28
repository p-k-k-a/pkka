import type { Announcement, BlogPost } from "@pkka/api";
import type { Faker } from "@faker-js/faker";

const IMAGES = [
  "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?q=80&w=800&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1531482615713-2afd69097998?q=80&w=800&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?q=80&w=800&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1523240795612-9a054b0db644?q=80&w=800&auto=format&fit=crop",
  "https://images.unsplash.com/photo-1515187029135-18ee286d815b?q=80&w=800&auto=format&fit=crop",
] as const;

const POLISH_NAMES = [
  "Jan Kowalski",
  "Anna Nowak",
  "Mariusz Wiśniewski",
  "Katarzyna Wójcik",
  "Piotr Kamiński",
  "Agnieszka Lewandowska",
] as const;

const POLISH_ROLES = [
  "PRZEWODNICZĄCY PKKA",
  "SPECJALISTA DS. WSPÓŁPRACY Z BIZNESEM",
  "KOORDYNATOR WARSZTATÓW IT",
  "FRONTEND ENGINEER & AGH ALUMNUS",
  "DYREKTOR DS. ROZWOJU",
] as const;

const TAGS = ["Wydarzenie", "Warsztaty", "Ogłoszenie", "Kariera", "Nauka"] as const;

function formatPolishDate(date: Date) {
  const day = String(date.getDate()).padStart(2, "0");
  const months = [
    "stycznia",
    "lutego",
    "marca",
    "kwietnia",
    "maja",
    "czerwca",
    "lipca",
    "sierpnia",
    "września",
    "października",
    "listopada",
    "grudnia",
  ];

  return `${day} ${months[date.getMonth()]} ${date.getFullYear()}`;
}

function formatTime(date: Date) {
  return `${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
}

function enrichBase(
  item: Pick<Announcement, "title" | "description" | "content" | "tag" | "imageUrl" | "slug" | "author">,
  faker: Faker,
) {
  const title = faker.lorem.sentence({ min: 5, max: 10 }).replace(/\.$/, "");
  const authorName = faker.helpers.arrayElement(POLISH_NAMES);
  const publishedDate = faker.date.recent({ days: 30 });

  return {
    ...item,
    title,
    description: faker.lorem.paragraph({ min: 2, max: 3 }),
    content: Array.from({ length: 4 })
      .map(() => faker.lorem.paragraphs(1))
      .join("\n\n"),
    tag: faker.helpers.arrayElement(TAGS),
    imageUrl: faker.helpers.arrayElement(IMAGES),
    slug: faker.helpers.slugify(title.toLowerCase()),
    author: {
      ...item.author,
      name: authorName,
      role: faker.helpers.arrayElement(POLISH_ROLES),
      avatarUrl: faker.helpers.arrayElement([
        `https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(authorName)}`,
        undefined,
      ]),
    },
    date: formatPolishDate(publishedDate),
    time: formatTime(publishedDate),
  };
}

export function enrichAnnouncements(pool: Announcement[], faker: Faker): Announcement[] {
  return pool.map((item) => ({
    ...item,
    ...enrichBase(item, faker),
  }));
}

export function enrichBlogPost(post: BlogPost, faker: Faker): BlogPost {
  const title = faker.lorem.sentence({ min: 5, max: 10 }).replace(/\.$/, "");
  const authorName = faker.helpers.arrayElement(POLISH_NAMES);

  return {
    ...post,
    title,
    description: faker.lorem.paragraph({ min: 2, max: 3 }),
    content: Array.from({ length: 4 })
      .map(() => faker.lorem.paragraphs(1))
      .join("\n\n"),
    tag: faker.helpers.arrayElement(TAGS),
    imageUrl: faker.helpers.arrayElement(IMAGES),
    slug: faker.helpers.slugify(title.toLowerCase()),
    author: {
      ...post.author,
      name: authorName,
      role: faker.helpers.arrayElement(POLISH_ROLES),
      avatarUrl: faker.helpers.arrayElement([
        `https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(authorName)}`,
        undefined,
      ]),
    },
    publishedAt: faker.date.recent({ days: 30 }).toISOString(),
  };
}
