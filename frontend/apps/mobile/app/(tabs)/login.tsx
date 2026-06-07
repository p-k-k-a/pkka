import { Text, View } from "react-native";

export default function LoginScreen() {
  return (
    <View style={{ flex: 1, justifyContent: "center", alignItems: "center" }}>
      <Text style={{ fontSize: 24, fontWeight: "bold" }}>Login</Text>
      <Text style={{ marginTop: 8, color: "#6b7280" }}>Sign in to continue.</Text>
    </View>
  );
}
