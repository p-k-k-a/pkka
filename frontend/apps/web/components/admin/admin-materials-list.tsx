"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, ArrowRight, ArrowUpRight, Pencil, Plus, Trash2 } from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import {
  getListAdminMaterialsQueryKey,
  getListMaterialsQueryKey,
  useDeleteAdminMaterial,
  useListAdminMaterials,
  type AdminMaterialResponse,
  type MaterialType,
} from "@pkka/api";
import { MATERIALS_PAGE_SIZE, formatMaterialCount, isAdmin } from "@pkka/domain";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { ADMIN_MATERIALS_PATH } from "@/components/admin/material-form";
import { MaterialCard } from "@/components/materials/material-card";
import { MaterialTypeTabs } from "@/components/materials/material-type-tabs";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuth } from "@/lib/auth-context";

export function AdminMaterialsList() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { isLoading, user } = useAuth();
  const admin = isAdmin(user?.roles);
  const [page, setPage] = useState(0);
  const [type, setType] = useState<MaterialType | null>(null);
  const [materialToDelete, setMaterialToDelete] = useState<AdminMaterialResponse | null>(null);

  useEffect(() => {
    if (!isLoading && !admin) {
      router.replace("/dashboard");
    }
  }, [admin, isLoading, router]);

  const {
    data: response,
    isLoading: isListLoading,
    isError,
  } = useListAdminMaterials(
    { page, size: MATERIALS_PAGE_SIZE, ...(type ? { type } : {}) },
    { query: { enabled: admin } },
  );

  const deleteMaterial = useDeleteAdminMaterial({
    mutation: {
      onSuccess: () => {
        // Removing the last card on a page would leave an empty page with no pager.
        if (response?.status === 200 && response.data.content?.length === 1) {
          setPage((prev) => Math.max(0, prev - 1));
        }
        queryClient.invalidateQueries({ queryKey: getListAdminMaterialsQueryKey() });
        queryClient.invalidateQueries({ queryKey: getListMaterialsQueryKey() });
        setMaterialToDelete(null);
      },
    },
  });

  if (isLoading || !admin) {
    return (
      <div className="px-4 py-10 md:px-10 md:py-20">
        <Skeleton className="mx-auto h-40 w-full max-w-3xl rounded-lg" />
      </div>
    );
  }

  const pageData = response?.status === 200 ? response.data : undefined;
  const materials = pageData?.content ?? [];
  const totalElements = pageData?.totalElements ?? 0;
  const totalPages = pageData?.totalPages ?? 0;
  const currentPage = pageData?.number ?? page;
  const firstShown = currentPage * (pageData?.size ?? MATERIALS_PAGE_SIZE) + 1;
  const lastShown = firstShown + materials.length - 1;

  return (
    <div>
      <section className="bg-background px-4 py-10 md:px-10 md:py-16">
        <div className="mx-auto max-w-[1280px] space-y-6">
          <p className="bg-accent/10 text-accent inline-flex rounded-lg px-3 py-1 text-xs font-semibold tracking-widest uppercase">
            Materiały klubu
          </p>
          <h1 className="font-heading text-foreground text-[28px] font-semibold tracking-tight md:text-[33px]">
            Nagrania i prezentacje
          </h1>
          <p className="text-muted-foreground max-w-2xl text-base leading-relaxed">
            Dodawaj linki do nagrań, slajdów i innych materiałów, a potem przypisuj je do wydarzeń.
            Zweryfikowani alumni widzą te same karty w zakładce „Materiały”.
          </p>
          <div className="flex flex-wrap items-center justify-between gap-4">
            <MaterialTypeTabs
              value={type}
              onChange={(next) => {
                setType(next);
                setPage(0);
              }}
            />
            <Button asChild>
              <Link href={`${ADMIN_MATERIALS_PATH}/new`}>
                <Plus data-icon="inline-start" />
                Nowy materiał
              </Link>
            </Button>
          </div>
        </div>
      </section>

      <section className="bg-background px-4 py-10 md:px-10 md:py-16">
        <div className="mx-auto max-w-[1280px]">
          {isListLoading ? (
            <div className="grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-3">
              {Array.from({ length: 3 }).map((_, i) => (
                <Skeleton key={i} className="bg-muted h-80 w-full rounded-none" />
              ))}
            </div>
          ) : isError ? (
            <p className="text-destructive font-medium">Nie udało się załadować materiałów.</p>
          ) : materials.length === 0 ? (
            <p className="text-muted-foreground">
              {type === null ? "Brak materiałów — dodaj pierwszy." : "Brak materiałów tego typu."}
            </p>
          ) : (
            <div className="space-y-8">
              <p className="text-muted-foreground text-sm">
                {totalPages > 1
                  ? `Wyświetlono ${firstShown}–${lastShown} z ${totalElements} materiałów, strona ${currentPage + 1} z ${totalPages}.`
                  : `Wyświetlono ${formatMaterialCount(totalElements)}.`}
              </p>

              <div className="grid grid-cols-1 gap-8 md:grid-cols-2 lg:grid-cols-3">
                {materials.map((item) => (
                  <MaterialCard
                    key={item.id}
                    material={item}
                    footer={
                      <div className="mt-auto flex flex-wrap items-center gap-2 pt-2">
                        <Button asChild variant="ghost" size="sm" className="mr-auto">
                          <a href={item.url} target="_blank" rel="noopener noreferrer">
                            Otwórz
                            <ArrowUpRight data-icon="inline-end" />
                          </a>
                        </Button>
                        <Button asChild variant="outline" size="sm">
                          <Link href={`${ADMIN_MATERIALS_PATH}/${item.id}`}>
                            <Pencil data-icon="inline-start" />
                            Edytuj
                          </Link>
                        </Button>
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          className="text-destructive hover:text-destructive"
                          onClick={() => setMaterialToDelete(item)}
                        >
                          <Trash2 data-icon="inline-start" />
                          Usuń
                        </Button>
                      </div>
                    }
                  />
                ))}
              </div>

              {totalPages > 1 ? (
                <div className="flex items-center justify-between gap-4 pt-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={currentPage <= 0}
                    onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                  >
                    <ArrowLeft data-icon="inline-start" />
                    Poprzednia
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={currentPage >= totalPages - 1}
                    onClick={() => setPage((prev) => prev + 1)}
                  >
                    Następna
                    <ArrowRight data-icon="inline-end" />
                  </Button>
                </div>
              ) : null}
            </div>
          )}
        </div>
      </section>

      <AlertDialog
        open={materialToDelete !== null}
        onOpenChange={(open) => {
          if (!open) {
            setMaterialToDelete(null);
            // Otherwise a failed delete would greet the next material's dialog.
            deleteMaterial.reset();
          }
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Usunąć materiał?</AlertDialogTitle>
            <AlertDialogDescription>
              „{materialToDelete?.title}” zniknie z listy alumnów. Sam plik lub nagranie pod linkiem
              pozostanie nietknięte.
            </AlertDialogDescription>
          </AlertDialogHeader>
          {deleteMaterial.isError ? (
            <p className="text-destructive text-sm font-medium">Nie udało się usunąć materiału.</p>
          ) : null}
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleteMaterial.isPending}>Anuluj</AlertDialogCancel>
            <AlertDialogAction
              variant="destructive"
              disabled={deleteMaterial.isPending}
              onClick={(clickEvent) => {
                clickEvent.preventDefault();
                if (materialToDelete) {
                  deleteMaterial.mutate({ id: materialToDelete.id });
                }
              }}
            >
              {deleteMaterial.isPending ? "Usuwanie…" : "Usuń materiał"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
