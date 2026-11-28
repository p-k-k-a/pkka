"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { isAdmin } from "@pkka/domain";
import { AdminTopicGroupList } from "@/components/admin/admin-topic-group-list";
import { TopicGroupRequestsList } from "@/components/admin/topic-group-requests-list";
import { SectionShell } from "@/components/content/section-shell";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { useAuth } from "@/lib/auth-context";

export function AdminTopicGroups() {
  const router = useRouter();
  const { isLoading, user } = useAuth();
  const admin = isAdmin(user?.roles);

  useEffect(() => {
    if (!isLoading && !admin) {
      router.replace("/dashboard");
    }
  }, [admin, isLoading, router]);

  return (
    <SectionShell
      title="Grupy tematyczne"
      description="Wnioski alumnów o nowe grupy i grupy, które już działają. Po zatwierdzeniu wniosku powstaje grupa z prywatnym kanałem na Discordzie, a wnioskodawca zostaje jej koordynatorem."
      as="section"
    >
      {isLoading || !admin ? (
        <Skeleton className="h-40 w-full max-w-3xl rounded-2xl" />
      ) : (
        <Tabs defaultValue="requests" className="max-w-3xl gap-6">
          <TabsList>
            <TabsTrigger value="requests">Wnioski</TabsTrigger>
            <TabsTrigger value="groups">Istniejące grupy</TabsTrigger>
          </TabsList>
          <TabsContent value="requests" className="space-y-4">
            <TopicGroupRequestsList />
          </TabsContent>
          <TabsContent value="groups" className="space-y-4">
            <AdminTopicGroupList />
          </TabsContent>
        </Tabs>
      )}
    </SectionShell>
  );
}
