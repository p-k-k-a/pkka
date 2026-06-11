import { LoginScreen } from "@/components/auth/login-screen";
import { UserPanel } from "@/components/profile/user-panel";
import { useAuth } from "@/lib/auth-context";

// TODO: think of a better name for this component
export default function LoginPage() {
  const { user } = useAuth();

  return user ? <UserPanel /> : <LoginScreen />;
}
