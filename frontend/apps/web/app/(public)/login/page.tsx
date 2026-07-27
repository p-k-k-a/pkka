import { LoginOptions } from "@/components/auth/login-options";

export default function LoginPage() {
  return (
    <section className="bg-muted flex flex-1 items-center justify-center px-4 py-10 md:px-10 md:py-20">
      <LoginOptions />
    </section>
  );
}
