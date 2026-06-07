import { LoginScreen } from "@/components/auth/login-screen";
import { LogoutScreen } from "@/components/auth/logout-screen";
import { useAuth } from "@/lib/auth-context";

export default function LoginPage() {
  const { user } = useAuth();

  return user ? <LogoutScreen /> : <LoginScreen />;
}
