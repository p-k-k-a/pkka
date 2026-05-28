import { ReactNode } from "react";

type TemporaryPageProps = {
  title: string;
  description: string;
  children?: ReactNode;
};

export function TemporaryPage({ title, description, children }: TemporaryPageProps) {
  return (
    <section className="mx-auto flex min-h-[60vh] w-full max-w-3xl flex-col items-center justify-center gap-4 px-6 py-16 text-center">
      <h1 className="text-foreground text-3xl font-extrabold tracking-tight md:text-4xl">
        {title}
      </h1>
      <p className="text-muted-foreground max-w-xl text-base leading-relaxed md:text-lg">
        {description}
      </p>
      {children}
    </section>
  );
}
