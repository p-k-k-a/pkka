import { MaterialEditor } from "@/components/admin/material-editor";

type DashboardEditMaterialPageProps = {
  params: Promise<{ id: string }>;
};

export default async function DashboardEditMaterialPage({
  params,
}: DashboardEditMaterialPageProps) {
  const { id } = await params;
  return <MaterialEditor id={id} />;
}
