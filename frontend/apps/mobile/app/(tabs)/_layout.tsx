import { useIsAlumni } from "@/components/auth/require-alumni";
import { useAuth } from "@/lib/auth-context";
import { useAppColorScheme, useThemeColors } from "@/lib/theme";
import { Tabs } from "expo-router";
import { Calendar, Home, LogIn, User, Users, type LucideIcon } from "lucide-react-native";
import { Text, View } from "react-native";

function TabIcon({
  icon: Icon,
  label,
  color,
  focused,
}: {
  icon: LucideIcon;
  label: string;
  color: string;
  focused: boolean;
}) {
  return (
    <View
      style={{
        alignSelf: "stretch",
        marginHorizontal: 4,
        alignItems: "center",
        gap: 2,
        paddingHorizontal: 4,
        paddingVertical: 5,
        borderRadius: 16,
        backgroundColor: focused ? `${color}26` : "transparent",
      }}
    >
      <Icon color={color} size={20} />
      <Text
        allowFontScaling={false}
        numberOfLines={1}
        style={{ color, fontFamily: "Montserrat", fontSize: 11, fontWeight: "600" }}
      >
        {label}
      </Text>
    </View>
  );
}

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
        tabBarShowLabel: false,
        tabBarIconStyle: { width: "100%", height: 48 },
        tabBarItemStyle: { paddingHorizontal: 0 },
        tabBarStyle: {
          backgroundColor: theme.card,
          borderTopColor: theme.border,
          height: 64,
          paddingTop: 6,
          paddingHorizontal: 12,
        },
        tabBarAllowFontScaling: false,
        animation: "shift",
      }}
      detachInactiveScreens={false}
      safeAreaInsets={{ bottom: 0 }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: "Aktualności",
          tabBarIcon: (props) => <TabIcon icon={Home} label="Aktualności" {...props} />,
        }}
      />
      <Tabs.Screen
        name="events"
        options={{
          title: "Wydarzenia",
          tabBarIcon: (props) => <TabIcon icon={Calendar} label="Wydarzenia" {...props} />,
        }}
      />
      <Tabs.Screen
        name="alumni"
        options={{
          title: "Alumni",
          // Verified-alumni only: href:null hides the tab and blocks navigation for everyone else.
          href: isAlumni ? undefined : null,
          tabBarIcon: (props) => <TabIcon icon={Users} label="Alumni" {...props} />,
        }}
      />
      <Tabs.Screen
        name="login"
        options={{
          title: user ? "Profil" : "Zaloguj",
          tabBarIcon: (props) => (
            <TabIcon icon={user ? User : LogIn} label={user ? "Profil" : "Zaloguj"} {...props} />
          ),
        }}
      />
    </Tabs>
  );
}
