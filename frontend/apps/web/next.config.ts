import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  transpilePackages: ["@pkka/api", "@pkka/domain", "@pkka/icons"],
  async rewrites() {
    if (process.env.NODE_ENV !== "development") return [];
    return [
      {
        source: "/api/:path*",
        destination: "http://localhost:8080/api/:path*",
      },
    ];
  },
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "images.unsplash.com",
      },
      {
        protocol: "https",
        hostname: "api.dicebear.com",
      },
      {
        protocol: "https",
        hostname: "img.youtube.com",
      },
      // Deliberately no object storage (Cloudflare R2) host here. The bucket is private and its
      // files are reached through signed URLs that die after an hour; the image optimizer would
      // re-serve a copy as public and cacheable for hours. Such images use `unoptimized`.
    ],
  },
};

export default nextConfig;
