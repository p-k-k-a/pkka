import { ReactNode } from "react";

type TemporaryPageProps = {
  title: string;
  description: string;
  children?: ReactNode;
};

export function TemporaryPage({ title, description, children }: TemporaryPageProps) {
  return (
    <section className="mx-auto flex min-h-[60vh] w-full max-w-3xl flex-col items-center justify-center gap-4 px-6 py-16 text-center">
      <h1 className="text-3xl font-extrabold tracking-tight text-foreground md:text-4xl">{title}</h1>
      <p className="max-w-xl text-base leading-relaxed text-muted-foreground md:text-lg">{description}</p>
      {children}
    </section>
  );
}
