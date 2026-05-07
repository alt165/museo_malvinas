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
      nombre: initialValue?.nombre ?? "",
      tipoObjeto: initialValue?.tipoObjeto ?? "",
      descripcion: initialValue?.descripcion ?? ""
    }
  });

  useEffect(() => {
    const validationErrors = getValidationErrors(submitError);

    Object.entries(validationErrors).forEach(([field, message]) => {
      if (field === "numeroInventario" || field === "nombre" || field === "tipoObjeto" || field === "descripcion") {
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
          nombre: values.nombre.trim(),
          tipoObjeto: values.tipoObjeto?.trim() || null,
          descripcion: values.descripcion?.trim() || null
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
        <div className="space-y-2">
          <label className="text-sm font-medium" htmlFor="nombre">
            Nombre
          </label>
          <input
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            id="nombre"
            {...register("nombre")}
          />
          {errors.nombre ? <p className="text-sm text-destructive">{errors.nombre.message}</p> : null}
        </div>
      </div>
      <div className="space-y-2">
        <label className="text-sm font-medium" htmlFor="tipoObjeto">
          Categoria
        </label>
        <select
          className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
          disabled={isSubmitting || isCategoriasLoading || isCategoriasError || categoriasOrdenadas.length === 0}
          id="tipoObjeto"
          {...register("tipoObjeto")}
        >
          <option value="">
            {isCategoriasLoading
              ? "Cargando categorias..."
              : categoriasOrdenadas.length === 0
                ? "No hay categorias disponibles"
                : "Seleccionar categoria"}
          </option>
          {categoriasOrdenadas.map((categoria) => (
            <option key={categoria.id} value={categoria.nombre}>
              {categoria.nombre}
            </option>
          ))}
        </select>
        {isCategoriasError ? (
          <p className="text-sm text-destructive">No se pudieron cargar las categorias.</p>
        ) : null}
        {errors.tipoObjeto ? <p className="text-sm text-destructive">{errors.tipoObjeto.message}</p> : null}
      </div>
      <div className="space-y-2">
        <label className="text-sm font-medium" htmlFor="descripcion">
          Descripcion
        </label>
        <textarea
          className="min-h-32 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
          id="descripcion"
          {...register("descripcion")}
        />
        {errors.descripcion ? <p className="text-sm text-destructive">{errors.descripcion.message}</p> : null}
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
