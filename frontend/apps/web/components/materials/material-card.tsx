import type { ReactNode } from "react";
import Image from "next/image";
import { ArrowUpRight, CalendarDays, FileText, Link2, PlayCircle } from "lucide-react";
import { MaterialType } from "@pkka/api";
import { materialLinkHost, materialTypeLabelUpper, youtubeThumbnailUrl } from "@pkka/domain";
import { Card } from "@/components/ui/card";
import { cn } from "@/lib/utils";

export type MaterialCardData = {
  title: string;
  description?: string;
  type: MaterialType;
  url: string;
  eventTitle?: string;
};

const TYPE_ICONS: Record<MaterialType, React.ComponentType<{ className?: string }>> = {
  RECORDING: PlayCircle,
  PRESENTATION: FileText,
  OTHER: Link2,
};

function MaterialThumbnail({ material }: { material: MaterialCardData }) {
  const thumbnail = youtubeThumbnailUrl(material.url);
  const Icon = TYPE_ICONS[material.type];

  return (
    <div className="bg-accent/10 relative flex aspect-video w-full items-center justify-center overflow-hidden">
      {thumbnail ? (
        <>
          <Image
            src={thumbnail}
            alt=""
            fill
            sizes="(max-width: 768px) 100vw, (max-width: 1280px) 50vw, 400px"
            className="object-cover"
          />
          <PlayCircle className="relative size-12 text-white drop-shadow-md" aria-hidden="true" />
        </>
      ) : (
        <Icon className="text-accent size-12" aria-hidden="true" />
      )}
    </div>
  );
}

type MaterialCardProps = {
  material: MaterialCardData;
  /** Replaces the outbound link; the card itself then stops being a link. */
  footer?: ReactNode;
  className?: string;
};

export function MaterialCard({ material, footer, className }: MaterialCardProps) {
  const host = materialLinkHost(material.url);

  const inner = (
    <Card
      className={cn(
        "bg-muted flex h-full flex-col gap-0 overflow-hidden rounded-none p-0 shadow-none ring-0",
        className,
      )}
    >
      <MaterialThumbnail material={material} />
      <div className="flex flex-1 flex-col gap-4 p-6">
        <span className="bg-band text-band-foreground inline-flex w-fit rounded-lg px-3 py-1 text-[11px] font-semibold tracking-widest uppercase">
          {materialTypeLabelUpper(material.type)}
        </span>
        <h3 className="font-heading text-foreground text-lg leading-snug font-semibold">
          {material.title.trim() || "Tytuł materiału"}
        </h3>
        {material.description ? (
          <p className="text-muted-foreground line-clamp-3 text-sm leading-relaxed">
            {material.description}
          </p>
        ) : null}
        {material.eventTitle ? (
          <p className="text-muted-foreground flex items-start gap-3 text-sm">
            <CalendarDays className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
            <span>{material.eventTitle}</span>
          </p>
        ) : null}
        {footer ?? (
          <div className="mt-auto flex justify-end pt-2">
            <span className="bg-background text-accent inline-flex h-[34px] items-center gap-1.5 rounded-none px-2.5 text-sm font-medium">
              {host ? `Otwórz w ${host}` : "Otwórz"}
              <ArrowUpRight className="size-3.5" aria-hidden="true" />
            </span>
          </div>
        )}
      </div>
    </Card>
  );

  if (footer) {
    return inner;
  }

  return (
    <a
      href={material.url}
      target="_blank"
      rel="noopener noreferrer"
      className="focus-visible:ring-ring block h-full rounded-none transition-opacity hover:opacity-90 focus-visible:ring-2 focus-visible:outline-none"
    >
      {inner}
    </a>
  );
}
