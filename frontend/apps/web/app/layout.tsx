import type { Metadata } from "next";
import { Geist_Mono, Jost, Montserrat } from "next/font/google";
import Script from "next/script";
import { Providers } from "@/components/providers";
import { authReturnRecoveryScript } from "@/lib/auth-navigation";
import "./globals.css";

const jost = Jost({
  variable: "--font-jost",
  subsets: ["latin", "latin-ext"],
});

const montserrat = Montserrat({
  variable: "--font-montserrat",
  subsets: ["latin", "latin-ext"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "PKKA - Klub Alumna WI AGH",
  description: "Platforma Klubu Alumna Wydziału Informatyki AGH",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="pl"
      suppressHydrationWarning
      className={`${jost.variable} ${montserrat.variable} ${geistMono.variable} h-full antialiased`}
    >
      <head>
        <Script
          id="theme-init"
          strategy="beforeInteractive"
          dangerouslySetInnerHTML={{
            __html: `(function(){try{var stored=localStorage.getItem("theme");var system=window.matchMedia("(prefers-color-scheme: dark)").matches?"dark":"light";var theme=stored==="dark"||stored==="light"?stored:system;var root=document.documentElement;root.classList.toggle("dark",theme==="dark");root.style.colorScheme=theme;}catch(e){}})();`,
          }}
        />
        <Script
          id="auth-return-recovery"
          strategy="beforeInteractive"
          dangerouslySetInnerHTML={{ __html: authReturnRecoveryScript }}
        />
      </head>
      <body className="flex min-h-full flex-col font-sans">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
