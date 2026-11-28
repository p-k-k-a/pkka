import { ImageManipulator, SaveFormat } from "expo-image-manipulator";
import * as ImagePicker from "expo-image-picker";

// Plenty for the largest avatar the apps show, and keeps a phone photo far below the upload limit.
const AVATAR_SIDE = 512;

export type PickedAvatar = {
  /** Local file, for showing the picture before the profile is saved. */
  uri: string;
  blob: Blob;
  contentType: "image/jpeg";
};

/**
 * Lets the user pick a photo from their library and crop it to a square, then scales it down
 * to a small JPEG. Whatever the phone stores (HEIC included) comes out in a format the backend
 * accepts. Resolves with `null` when the user backs out of the picker.
 */
export async function pickAvatar(): Promise<PickedAvatar | null> {
  // The system photo picker needs no permission prompt.
  const result = await ImagePicker.launchImageLibraryAsync({
    mediaTypes: "images",
    allowsEditing: true,
    aspect: [1, 1],
    quality: 1,
  });
  const asset = result.assets?.[0];
  if (result.canceled || !asset) return null;

  const rendered = await ImageManipulator.manipulate(asset.uri)
    .resize({ width: AVATAR_SIDE })
    .renderAsync();
  const saved = await rendered.saveAsync({ format: SaveFormat.JPEG, compress: 0.85 });
  const blob = await (await fetch(saved.uri)).blob();

  return { uri: saved.uri, blob, contentType: "image/jpeg" };
}
