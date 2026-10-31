import { hashString } from "../format/hash";

const PLACEHOLDER_COVERS = [
  "photo-1492684223066-81342ee5ff30",
  "photo-1497366811353-6870744d04b2",
  "photo-1498050108023-c5249f4df085",
  "photo-1504384308090-c894fdcc538d",
  "photo-1505373877841-8d25f7d46678",
  "photo-1511795409834-ef04bbd61622",
  "photo-1516321318423-f06f85e504b3",
  "photo-1517048676732-d65bc937f952",
  "photo-1517694712202-14dd9538aa97",
  "photo-1540575467063-178a50c2df87",
  "photo-1543269865-cbf427effbad",
  "photo-1556761175-5973dc0f32e7",
  "photo-1558494949-ef010cbdcc31",
  "photo-1610890716171-6b1bb98ffd09",
];

export function placeholderCoverUrl(id: string) {
  const photo = PLACEHOLDER_COVERS[hashString(id) % PLACEHOLDER_COVERS.length];
  return `https://images.unsplash.com/${photo}?w=800&h=450&fit=crop`;
}
