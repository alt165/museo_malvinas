"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { useCategoriasQuery } from "@/features/categorias/queries";
import type { ObjetoMuseoRequestDTO, ObjetoMuseoResponseDTO } from "../types";
import { objetoMuseoSchema, type ObjetoMuseoFormValues } from "../schemas";
import { getValidationErrors } from "../utils";

type ObjetoMuseoFormProps = {
  initialValue?: ObjetoMuseoResponseDTO;
  isSubmitting?: boolean;
  submitError?: unknown;
  submitLabel: string;
  onSubmit: (payload: ObjetoMuseoRequestDTO) => void;
};

export function ObjetoMuseoForm({
  initialValue,
  isSubmitting = false,
  onSubmit,
  submitError,
  submitLabel
}: ObjetoMuseoFormProps) {
  const {
    data: categorias = [],
    isError: isCategoriasError,
    isLoading: isCategoriasLoading
  } = useCategoriasQuery();
  const categoriasOrdenadas = [...categorias].sort((a, b) => a.nombre.localeCompare(b.nombre));
  const {
    formState: { errors },
    handleSubmit,
    register,
    setError
  } = useForm<ObjetoMuseoFormValues>({
    resolver: zodResolver(objetoMuseoSchema),
    defaultValues: {
      numeroInventario: initialValue?.numeroInventario ?? "",
      denominacionObjeto: initialValue?.denominacionObjeto ?? "",
      descripcion: initialValue?.descripcion ?? "",
      descripcionTecnica: initialValue?.descripcionTecnica ?? "",
      materiales: initialValue?.materiales ?? "",
      dimensiones: initialValue?.dimensiones ?? "",
      estadoConservacion: initialValue?.estadoConservacion ?? "",
      categoriaIds: initialValue?.categorias?.map((categoria) => categoria.id) ?? []
    }
  });

  useEffect(() => {
    const validationErrors = getValidationErrors(submitError);

    Object.entries(validationErrors).forEach(([field, message]) => {
      if (
        field === "numeroInventario" ||
        field === "denominacionObjeto" ||
        field === "descripcion" ||
        field === "descripcionTecnica" ||
        field === "materiales" ||
        field === "dimensiones" ||
        field === "estadoConservacion" ||
        field === "categoriaIds"
      ) {
        setError(field, { message });
      }
    });
  }, [setError, submitError]);

  return (
    <form
      className="max-w-3xl space-y-5 rounded-lg border p-5"
      onSubmit={handleSubmit((values) =>
        onSubmit({
          numeroInventario: values.numeroInventario.trim(),
          denominacionObjeto: values.denominacionObjeto.trim(),
          descripcion: values.descripcion?.trim() || null,
          descripcionTecnica: values.descripcionTecnica?.trim() || null,
          materiales: values.materiales?.trim() || null,
          dimensiones: values.dimensiones?.trim() || null,
          estadoConservacion: values.estadoConservacion || null,
          categoriaIds: values.categoriaIds ?? []
        })
      )}
    >
      <div className="grid gap-5 sm:grid-cols-2">
        <div className="space-y-2">
          <label className="text-sm font-medium" htmlFor="numeroInventario">
            Numero de inventario
          </label>
          <input
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            id="numeroInventario"
            {...register("numeroInventario")}
          />
          {errors.numeroInventario ? (
            <p className="text-sm text-destructive">{errors.numeroInventario.message}</p>
          ) : null}
        </div>
      </div>
      <div className="space-y-2">
        <label className="text-sm font-medium" htmlFor="denominacionObjeto">
          Denominacion
        </label>
        <input
          className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
          id="denominacionObjeto"
          {...register("denominacionObjeto")}
        />
        {errors.denominacionObjeto ? <p className="text-sm text-destructive">{errors.denominacionObjeto.message}</p> : null}
      </div>
      <div className="space-y-2">
        <label className="text-sm font-medium" htmlFor="categoriaIds">
          Categorias
        </label>
        <select
          className="min-h-28 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
          disabled={isSubmitting || isCategoriasLoading || isCategoriasError || categoriasOrdenadas.length === 0}
          id="categoriaIds"
          multiple
          {...register("categoriaIds", {
            setValueAs: (value) => (Array.isArray(value) ? value.map(Number) : [])
          })}
        >
          {categoriasOrdenadas.map((categoria) => (
            <option key={categoria.id} value={categoria.id}>
              {categoria.nombre}
            </option>
          ))}
        </select>
        {isCategoriasError ? (
          <p className="text-sm text-destructive">No se pudieron cargar las categorias.</p>
        ) : null}
      </div>
      <div className="grid gap-5 sm:grid-cols-2">
        <div className="space-y-2">
          <label className="text-sm font-medium" htmlFor="estadoConservacion">
            Estado de conservacion
          </label>
          <select
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            id="estadoConservacion"
            {...register("estadoConservacion")}
          >
            <option value="">Sin especificar</option>
            <option value="EXCELENTE">Excelente</option>
            <option value="BUENO">Bueno</option>
            <option value="REGULAR">Regular</option>
            <option value="MALO">Malo</option>
            <option value="CRITICO">Critico</option>
          </select>
          {errors.estadoConservacion ? <p className="text-sm text-destructive">{errors.estadoConservacion.message}</p> : null}
        </div>
      </div>
      <div className="space-y-2">
        <label className="text-sm font-medium" htmlFor="descripcion">
          Descripcion breve
        </label>
        <textarea
          className="min-h-32 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
          id="descripcion"
          {...register("descripcion")}
        />
        {errors.descripcion ? <p className="text-sm text-destructive">{errors.descripcion.message}</p> : null}
      </div>
      <div className="space-y-2">
        <label className="text-sm font-medium" htmlFor="descripcionTecnica">
          Descripcion tecnica
        </label>
        <textarea
          className="min-h-32 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
          id="descripcionTecnica"
          {...register("descripcionTecnica")}
        />
      </div>
      <div className="grid gap-5 sm:grid-cols-2">
        <div className="space-y-2">
          <label className="text-sm font-medium" htmlFor="materiales">
            Materiales
          </label>
          <textarea
            className="min-h-24 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
            id="materiales"
            {...register("materiales")}
          />
        </div>
        <div className="space-y-2">
          <label className="text-sm font-medium" htmlFor="dimensiones">
            Dimensiones
          </label>
          <textarea
            className="min-h-24 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
            id="dimensiones"
            {...register("dimensiones")}
          />
        </div>
      </div>
      <div className="flex items-center gap-3">
        <button
          className="inline-flex h-10 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
          disabled={isSubmitting}
          type="submit"
        >
          {isSubmitting ? "Guardando..." : submitLabel}
        </button>
        <Link className="inline-flex h-10 items-center justify-center rounded-md border px-4 text-sm hover:bg-muted" href="/objetos">
          Cancelar
        </Link>
      </div>
    </form>
  );
}
