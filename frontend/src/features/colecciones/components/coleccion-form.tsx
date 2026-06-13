"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { useForm } from "react-hook-form";
import { ObjetoSearchSelector } from "@/features/objetos/components/objeto-search-selector";
import type { ObjetoMuseoResponseDTO } from "@/features/objetos/types";
import { coleccionObjetoSchema, type ColeccionObjetoFormValues } from "../schemas";
import type { ColeccionObjetoRequestDTO, ColeccionObjetoResponseDTO } from "../types";
import { getValidationErrors } from "../utils";

type ColeccionFormProps = {
  initialObjetos?: ObjetoMuseoResponseDTO[];
  initialValue?: ColeccionObjetoResponseDTO;
  isSubmitting?: boolean;
  submitError?: unknown;
  submitLabel: string;
  onSubmit: (payload: ColeccionObjetoRequestDTO) => void;
};

export function ColeccionForm({ initialObjetos = [], initialValue, isSubmitting = false, onSubmit, submitError, submitLabel }: ColeccionFormProps) {
  const [objetosIncluidos, setObjetosIncluidos] = useState<ObjetoMuseoResponseDTO[]>(initialObjetos);
  const {
    formState: { errors },
    handleSubmit,
    register,
    setError
  } = useForm<ColeccionObjetoFormValues>({
    resolver: zodResolver(coleccionObjetoSchema),
    defaultValues: {
      nombre: initialValue?.nombre ?? "",
      descripcion: initialValue?.descripcion ?? ""
    }
  });


  useEffect(() => {
    const validationErrors = getValidationErrors(submitError);

    Object.entries(validationErrors).forEach(([field, message]) => {
      if (field === "nombre" || field === "descripcion") {
        setError(field, { message });
      }
    });
  }, [setError, submitError]);

  const objetosIncluidosIds = useMemo(() => objetosIncluidos.map((objeto) => objeto.id), [objetosIncluidos]);

  function handleAgregarObjeto(objeto: ObjetoMuseoResponseDTO) {
    setObjetosIncluidos((current) => {
      if (current.some((item) => item.id === objeto.id)) {
        return current;
      }
      return [...current, objeto];
    });
  }

  function handleQuitarObjeto(objetoId: number) {
    setObjetosIncluidos((current) => current.filter((objeto) => objeto.id !== objetoId));
  }

  return (
    <form
      className="w-full space-y-5 rounded-lg border p-5"
      onSubmit={handleSubmit((values) =>
        onSubmit({
          nombre: values.nombre.trim(),
          descripcion: values.descripcion?.trim() || null,
          objetoIds: objetosIncluidosIds
        })
      )}
    >
      <Field error={errors.nombre?.message} label="Nombre">
        <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" {...register("nombre")} />
      </Field>
      <Field error={errors.descripcion?.message} label="Descripcion">
        <textarea className="min-h-32 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" {...register("descripcion")} />
      </Field>

      <section className="space-y-3 rounded-lg border bg-surface p-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-base font-semibold text-primary">Objetos incluidos en la colección</h2>
          <span className="text-xs text-muted-foreground">{objetosIncluidos.length} objeto(s)</span>
        </div>
        {objetosIncluidos.length === 0 ? (
          <div className="rounded-md border border-dashed p-5 text-sm text-muted-foreground">No hay objetos incluidos en la colección.</div>
        ) : (
          <div className="overflow-hidden rounded-md border">
            <table className="w-full border-collapse text-sm">
              <thead className="bg-muted/60">
                <tr>
                  <th className="px-4 py-3 text-left font-medium">Inventario</th>
                  <th className="px-4 py-3 text-left font-medium">Denominación</th>
                  <th className="px-4 py-3 text-right font-medium">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {objetosIncluidos.map((objeto) => (
                  <tr className="border-t" key={objeto.id}>
                    <td className="px-4 py-3 align-top font-medium">{objeto.numeroInventario}</td>
                    <td className="px-4 py-3 align-top">{objeto.denominacionObjeto}</td>
                    <td className="px-4 py-3 text-right align-top">
                      <button
                        className="rounded-md border px-3 py-1.5 text-xs text-destructive hover:bg-muted disabled:opacity-60"
                        disabled={isSubmitting}
                        onClick={() => handleQuitarObjeto(objeto.id)}
                        type="button"
                      >
                        Quitar
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <ObjetoSearchSelector
        coleccionId={initialValue?.id}
        description="Busque objetos sin colección para incluirlos. En edición también se consideran los objetos ya incluidos en esta colección."
        emptyLabel="No hay objetos disponibles que coincidan con los filtros aplicados."
        excludeObjetoIds={objetosIncluidosIds}
        onSelect={handleAgregarObjeto}
        requireFilters
        selectLabel="Agregar"
        soloDisponiblesParaColeccion
        title="Buscar objetos disponibles"
      />

      <div className="flex items-center gap-3">
        <button
          className="inline-flex h-10 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
          disabled={isSubmitting}
          type="submit"
        >
          {isSubmitting ? "Guardando..." : submitLabel}
        </button>
        <Link className="inline-flex h-10 items-center justify-center rounded-md border px-4 text-sm hover:bg-muted" href="/objetos/colecciones">
          Cancelar
        </Link>
      </div>
    </form>
  );
}

function Field({ children, error, label }: { children: ReactNode; error?: string; label: string }) {
  return (
    <label className="space-y-2 text-sm font-medium">
      <span>{label}</span>
      {children}
      {error ? <p className="font-normal text-destructive">{error}</p> : null}
    </label>
  );
}
