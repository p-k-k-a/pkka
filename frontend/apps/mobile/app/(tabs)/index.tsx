import { Text, View } from "react-native";

export default function HomeScreen() {
  return (
    <View style={{ flex: 1, justifyContent: "center", alignItems: "center" }}>
      <Text style={{ fontSize: 24, fontWeight: "bold" }}>Home</Text>
      <Text style={{ marginTop: 8, color: "#6b7280" }}>Welcome to the app!</Text>
    </View>
  );
}
