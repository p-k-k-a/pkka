import { useIsAlumni } from "@/components/auth/require-alumni";
import { useAuth } from "@/lib/auth-context";
import { useAppColorScheme, useThemeColors } from "@/lib/theme";
import { Tabs } from "expo-router";
import { Calendar, Home, LogIn, User, Users } from "lucide-react-native";

export default function TabsLayout() {
  const theme = useThemeColors();
  const scheme = useAppColorScheme();
  const { user } = useAuth();
  const isAlumni = useIsAlumni();

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: scheme === "dark" ? theme.primary : theme.accent,
        tabBarInactiveTintColor: theme.mutedForeground,
        tabBarLabelStyle: {
          fontFamily: "Montserrat",
          fontSize: 10,
          fontWeight: "700",
          letterSpacing: 0.8,
        },
        tabBarStyle: {
          backgroundColor: theme.card,
          borderTopColor: theme.border,
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
