"use client";

import { Link2, Plus } from "lucide-react";
import Link from "next/link";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { ObjetoSearchSelector } from "@/features/objetos/components/objeto-search-selector";
import { canWrite, useAuth } from "@/lib/auth";

export default function RelacionesObjetosPage() {
  const { roles } = useAuth();
  const puedeEscribir = canWrite(roles);

  return (
    <AppShell>
      <div className="space-y-6">
        <PageHeader
          description="Busque un objeto para consultar su grafo de relaciones o crear un nuevo vinculo desde ese objeto."
          title="Relaciones entre objetos"
        />

        <ObjetoSearchSelector
          description="Filtre por nombre, numero de inventario o categorias. Las acciones se realizan desde cada objeto del listado."
          renderActions={(objeto) => (
            <div className="flex flex-wrap justify-end gap-2">
              <Link
                className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs font-medium hover:bg-muted"
                href={`/objetos/${objeto.id}/relaciones?view=graph`}
              >
                <Link2 className="h-3.5 w-3.5" />
                Ver relaciones
              </Link>
              {puedeEscribir ? (
                <Link
                  className="inline-flex h-8 items-center gap-1 rounded-md border px-2 text-xs font-medium text-[#163A61] hover:bg-muted"
                  href={`/relaciones-objetos/nueva?origenId=${objeto.id}`}
                >
                  <Plus className="h-3.5 w-3.5" />
                  Nueva relacion
                </Link>
              ) : null}
            </div>
          )}
          title="Buscar objeto"
        />
      </div>
    </AppShell>
  );
}
