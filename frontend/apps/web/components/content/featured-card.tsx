import Image from "next/image";
import Link from "next/link";
import { Card, CardTitle } from "@/components/ui/card";
import { CardLinkFooter } from "@/components/content/card-link-footer";

type FeaturedCardProps = {
  href: string;
  imageSrc: string;
  imageAlt: string;
  meta: React.ReactNode;
  title: string;
  cta: React.ReactNode;
  ctaAlign?: "start" | "end";
  imageOverlay?: React.ReactNode;
  children?: React.ReactNode;
};

export function FeaturedCard({
  href,
  imageSrc,
  imageAlt,
  meta,
  title,
  cta,
  ctaAlign = "end",
  imageOverlay,
  children,
}: FeaturedCardProps) {
  return (
    <Link
      href={href}
      className="group focus-visible:ring-ring flex rounded-[24px] focus-visible:ring-2 focus-visible:outline-none md:col-span-2"
    >
      <Card className="border-border/70 bg-card text-card-foreground hover:bg-muted/30 flex w-full flex-col overflow-hidden rounded-[24px] border p-0 shadow-sm transition-all duration-300 hover:shadow-md md:flex-row">
        <div className="bg-muted relative aspect-[4/3] w-full shrink-0 overflow-hidden md:aspect-auto md:w-[50%]">
          <Image
            src={imageSrc}
            alt={imageAlt}
            fill
            sizes="(max-width: 768px) 100vw, 50vw"
            className="object-cover transition-transform duration-500 group-hover:scale-[1.02]"
            priority
          />
          {imageOverlay}
        </div>

        <div className="flex flex-1 flex-col justify-between p-8 md:p-10">
          <div className="space-y-4">
            {meta}
            <CardTitle className="text-foreground group-hover:text-primary text-2xl leading-tight font-extrabold transition-colors md:text-3xl">
              {title}
            </CardTitle>
            {children}
          </div>

          <CardLinkFooter align={ctaAlign}>{cta}</CardLinkFooter>
        </div>
      </Card>
    </Link>
  );
}
