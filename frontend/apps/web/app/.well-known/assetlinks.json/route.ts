const ANDROID_PACKAGE = "com.pkka.app";

export function GET() {
  const fingerprints = (process.env.ANDROID_APP_SHA256_FINGERPRINTS ?? "")
    .split(",")
    .map((fingerprint) => fingerprint.trim())
    .filter(Boolean);

  return Response.json([
    {
      relation: ["delegate_permission/common.handle_all_urls"],
      target: {
        namespace: "android_app",
        package_name: ANDROID_PACKAGE,
        sha256_cert_fingerprints: fingerprints,
      },
    },
  ]);
}
