import { useIsAlumni } from "@/components/auth/require-alumni";
import { useAuth } from "@/lib/auth-context";
import { useTheme } from "@react-navigation/native";
import { Tabs } from "expo-router";
import { Calendar, Home, LogIn, User, Users } from "lucide-react-native";

export default function TabsLayout() {
  const { colors } = useTheme();
  const { user } = useAuth();
  const isAlumni = useIsAlumni();

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.primary,
        tabBarStyle: {
          backgroundColor: colors.card,
          borderTopColor: colors.border,
        },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: "AKTUALNOŚCI",
          tabBarIcon: ({ color, size }) => <Home color={color} size={size} />,
        }}
      />
      <Tabs.Screen
        name="events"
        options={{
          title: "WYDARZENIA",
          tabBarIcon: ({ color, size }) => <Calendar color={color} size={size} />,
        }}
      />
      <Tabs.Screen
        name="alumni"
        options={{
          title: "ALUMNI",
          // Verified-alumni only: href:null hides the tab and blocks navigation for everyone else.
          href: isAlumni ? undefined : null,
          tabBarIcon: ({ color, size }) => <Users color={color} size={size} />,
        }}
      />
      <Tabs.Screen
        name="login"
        options={{
          title: user ? "PROFIL" : "ZALOGUJ",
          tabBarIcon: ({ color, size }) =>
            user ? <User color={color} size={size} /> : <LogIn color={color} size={size} />,
        }}
      />
    </Tabs>
  );
}
