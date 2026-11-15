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
      {
        // Files uploaded to object storage (Cloudflare R2), served through signed URLs.
        // Keep in step with isOptimizableImageUrl in lib/content-images.ts.
        protocol: "https",
        hostname: "**.eu.r2.cloudflarestorage.com",
      },
    ],
  },
};

export default nextConfig;
