import { LoginOptions } from "@/components/auth/login-options";

type LoginPageProps = {
  searchParams: Promise<{ returnTo?: string | string[] }>;
};

export default async function LoginPage({ searchParams }: LoginPageProps) {
  const { returnTo } = await searchParams;
  return (
    <section className="bg-muted flex flex-1 items-center justify-center px-4 py-10 md:px-10 md:py-20">
      <LoginOptions returnTo={typeof returnTo === "string" ? returnTo : undefined} />
    </section>
  );
}
