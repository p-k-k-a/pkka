import Image from "next/image";
import Link from "next/link";
import { Card } from "@/components/ui/card";
import { CardLinkFooter } from "@/components/content/card-link-footer";

type FeaturedCardProps = {
  href: string;
  imageSrc: string;
  imageAlt: string;
  label: string;
  badge?: React.ReactNode;
  title: string;
  meta: React.ReactNode;
  cta: string;
  className?: string;
};

export function FeaturedCard({
  href,
  imageSrc,
  imageAlt,
  label,
  badge,
  title,
  meta,
  cta,
  className,
}: FeaturedCardProps) {
  return (
    <Link
      href={href}
      className={`group focus-visible:ring-ring block rounded-none transition-opacity hover:opacity-90 focus-visible:ring-2 focus-visible:outline-none ${className ?? ""}`}
    >
      <Card className="bg-muted flex h-full flex-col gap-0 overflow-hidden rounded-none p-0 shadow-none ring-0 md:flex-row">
        <div className="relative aspect-[4/3] w-full shrink-0 md:aspect-auto md:min-h-80 md:w-1/2">
          <Image
            src={imageSrc}
            alt={imageAlt}
            fill
            sizes="(max-width: 768px) 100vw, 640px"
            className="object-cover"
            priority
          />
        </div>

        <div className="flex flex-1 flex-col gap-6 p-6 md:p-10">
          <div className="flex flex-wrap items-center gap-2">
            <span className="bg-band text-band-foreground inline-flex w-fit rounded-lg px-3 py-1 text-[11px] font-semibold tracking-widest uppercase">
              {label}
            </span>
            {badge}
          </div>
          <h3 className="font-heading text-foreground text-[23px] leading-snug font-semibold md:text-[28px]">
            {title}
          </h3>
          {meta}
          <CardLinkFooter>{cta}</CardLinkFooter>
        </div>
      </Card>
    </Link>
  );
}
