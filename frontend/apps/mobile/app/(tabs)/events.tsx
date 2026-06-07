import { Text, View } from "react-native";

export default function EventsScreen() {
  return (
    <View style={{ flex: 1, justifyContent: "center", alignItems: "center" }}>
      <Text style={{ fontSize: 24, fontWeight: "bold" }}>Events</Text>
      <Text style={{ marginTop: 8, color: "#6b7280" }}>Browse upcoming events.</Text>
    </View>
  );
}
