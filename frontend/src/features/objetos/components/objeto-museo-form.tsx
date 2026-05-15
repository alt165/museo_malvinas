"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { useCategoriasQuery } from "@/features/categorias/queries";
import type { ObjetoMuseoRequestDTO, ObjetoMuseoResponseDTO } from "../types";
import { objetoMuseoSchema, type ObjetoMuseoFormValues } from "../schemas";
import { getValidationErrors } from "../utils";

type ObjetoMuseoFormProps = {
  allowFileUploads?: boolean;
  initialValue?: ObjetoMuseoResponseDTO;
  isSubmitting?: boolean;
  submitError?: unknown;
  submitLabel: string;
  onSubmit: (payload: ObjetoMuseoRequestDTO, archivos: ObjetoMuseoFormFiles) => void;
};

export type ObjetoMuseoFormFiles = {
  fotos: File[];
  reciboEscaneado: File | null;
};

const fotoContentTypesPermitidos = new Set(["image/jpeg", "image/png", "image/webp"]);
const reciboContentTypesPermitidos = new Set(["application/pdf", "image/jpeg", "image/png", "image/webp"]);
const maxFotoBytes = 5 * 1024 * 1024;
const maxReciboBytes = 10 * 1024 * 1024;

export function ObjetoMuseoForm({
  allowFileUploads = false,
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
  const [categoriaBusqueda, setCategoriaBusqueda] = useState("");
  const [fotos, setFotos] = useState<File[]>([]);
  const [reciboEscaneado, setReciboEscaneado] = useState<File | null>(null);
  const [fileErrors, setFileErrors] = useState<string[]>([]);
  const {
    formState: { errors },
    handleSubmit,
    register,
    setError,
    setValue,
    control
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
  const watchedCategoriaIds = useWatch({ control, name: "categoriaIds", defaultValue: [] });
  const categoriaIds = useMemo(() => watchedCategoriaIds ?? [], [watchedCategoriaIds]);
  const categoriasFiltradas = useMemo(() => {
    const busqueda = categoriaBusqueda.trim().toLowerCase();
    if (!busqueda) {
      return categoriasOrdenadas;
    }
    return categoriasOrdenadas.filter((categoria) => categoria.nombre.toLowerCase().includes(busqueda));
  }, [categoriaBusqueda, categoriasOrdenadas]);
  const categoriasSeleccionadas = useMemo(
    () => categoriasOrdenadas.filter((categoria) => categoriaIds.includes(categoria.id)),
    [categoriasOrdenadas, categoriaIds]
  );
  const fotoPreviews = useMemo(
    () => fotos.map((foto) => ({ file: foto, url: URL.createObjectURL(foto) })),
    [fotos]
  );

  useEffect(() => {
    return () => {
      fotoPreviews.forEach((preview) => URL.revokeObjectURL(preview.url));
    };
  }, [fotoPreviews]);

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

  function toggleCategoria(categoriaId: number) {
    const seleccionada = categoriaIds.includes(categoriaId);
    setValue(
      "categoriaIds",
      seleccionada ? categoriaIds.filter((id) => id !== categoriaId) : [...categoriaIds, categoriaId],
      { shouldDirty: true, shouldValidate: true }
    );
  }

  function agregarFotos(files: FileList | null) {
    if (!files) {
      return;
    }
    const errores: string[] = [];
    const validas = Array.from(files).filter((file) => {
      if (!fotoContentTypesPermitidos.has(file.type)) {
        errores.push(`${file.name}: tipo no permitido`);
        return false;
      }
      if (file.size > maxFotoBytes) {
        errores.push(`${file.name}: supera 5 MB`);
        return false;
      }
      return true;
    });
    setFileErrors(errores);
    setFotos((current) => [...current, ...validas]);
  }

  function seleccionarRecibo(file: File | undefined) {
    if (!file) {
      return;
    }
    if (!reciboContentTypesPermitidos.has(file.type)) {
      setFileErrors([`${file.name}: tipo no permitido para recibo escaneado`]);
      return;
    }
    if (file.size > maxReciboBytes) {
      setFileErrors([`${file.name}: supera 10 MB`]);
      return;
    }
    setFileErrors([]);
    setReciboEscaneado(file);
  }

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
        }, { fotos, reciboEscaneado })
      )}
    >
      {initialValue?.origenCarga === "RAPIDA" && initialValue.datosCompletos === false ? (
        <div className="rounded-md border border-amber-300 bg-amber-50 p-4 text-sm text-amber-950">
          <p className="font-medium">Ficha pendiente de completar</p>
          <p className="mt-1">
            Para cerrar la carga se requiere descripcion tecnica, materiales, dimensiones, estado de conservacion y al menos una categoria.
          </p>
        </div>
      ) : null}
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
      <section className="space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <label className="text-sm font-medium" htmlFor="categoria-busqueda-form">
            Categorias
          </label>
          <span className="text-xs text-muted-foreground">{categoriaIds.length} seleccionada(s)</span>
        </div>
        <input
          className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
          disabled={isSubmitting || isCategoriasLoading || isCategoriasError}
          id="categoria-busqueda-form"
          onChange={(event) => setCategoriaBusqueda(event.target.value)}
          placeholder="Buscar categoria"
          type="text"
          value={categoriaBusqueda}
        />
        <div className="max-h-44 overflow-y-auto rounded-md border bg-background p-2">
          {isCategoriasLoading ? <p className="px-2 py-2 text-sm text-muted-foreground">Cargando categorias...</p> : null}
          {!isCategoriasLoading && categoriasFiltradas.length === 0 ? (
            <p className="px-2 py-2 text-sm text-muted-foreground">Sin categorias disponibles.</p>
          ) : null}
          <div className="grid gap-1 sm:grid-cols-2 lg:grid-cols-3">
            {categoriasFiltradas.map((categoria) => (
              <label className="flex min-h-9 items-center gap-2 rounded-md px-2 py-1.5 text-sm hover:bg-muted" key={categoria.id}>
                <input
                  checked={categoriaIds.includes(categoria.id)}
                  className="h-4 w-4 accent-primary"
                  disabled={isSubmitting}
                  onChange={() => toggleCategoria(categoria.id)}
                  type="checkbox"
                />
                <span className="min-w-0 flex-1 truncate">{categoria.nombre}</span>
              </label>
            ))}
          </div>
        </div>
        {categoriasSeleccionadas.length > 0 ? (
          <div className="flex flex-wrap gap-2">
            {categoriasSeleccionadas.map((categoria) => (
              <button
                className="rounded-full border border-primary/20 bg-secondary/20 px-3 py-1 text-xs font-medium text-primary hover:bg-secondary/30"
                disabled={isSubmitting}
                key={categoria.id}
                onClick={() => toggleCategoria(categoria.id)}
                type="button"
              >
                {categoria.nombre} ×
              </button>
            ))}
          </div>
        ) : null}
        {isCategoriasError ? <p className="text-sm text-destructive">No se pudieron cargar las categorias.</p> : null}
        {errors.categoriaIds ? <p className="text-sm text-destructive">{errors.categoriaIds.message}</p> : null}
      </section>
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
      {allowFileUploads ? (
        <>
          <section className="space-y-3 rounded-md border bg-surface p-4">
            <div>
              <h2 className="text-base font-semibold">Fotos del objeto</h2>
              <p className="mt-1 text-sm text-muted-foreground">JPEG, PNG o WebP. Maximo 5 MB por foto.</p>
            </div>
            <input
              accept="image/jpeg,image/png,image/webp"
              capture="environment"
              className="block w-full rounded-md border bg-background px-3 py-2 text-sm"
              disabled={isSubmitting}
              multiple
              onChange={(event) => {
                agregarFotos(event.target.files);
                event.currentTarget.value = "";
              }}
              type="file"
            />
            {fotoPreviews.length > 0 ? (
              <div className="grid gap-3 sm:grid-cols-2">
                {fotoPreviews.map((preview, index) => (
                  <div className="overflow-hidden rounded-md border bg-background" key={`${preview.file.name}-${index}`}>
                    {/* eslint-disable-next-line @next/next/no-img-element */}
                    <img alt={preview.file.name} className="h-36 w-full object-cover" src={preview.url} />
                    <div className="flex items-center justify-between gap-2 p-2 text-sm">
                      <span className="min-w-0 truncate">{preview.file.name}</span>
                      <button
                        className="rounded-md border px-2 py-1 text-xs text-destructive hover:bg-destructive/10"
                        disabled={isSubmitting}
                        onClick={() => setFotos((current) => current.filter((_, itemIndex) => itemIndex !== index))}
                        type="button"
                      >
                        Quitar
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            ) : null}
          </section>
          <section className="space-y-3 rounded-md border bg-surface p-4">
            <div>
              <h2 className="text-base font-semibold">Recibo escaneado</h2>
              <p className="mt-1 text-sm text-muted-foreground">Opcional. PDF, JPEG, PNG o WebP. Maximo 10 MB.</p>
            </div>
            <input
              accept="application/pdf,image/jpeg,image/png,image/webp"
              className="block w-full rounded-md border bg-background px-3 py-2 text-sm"
              disabled={isSubmitting}
              onChange={(event) => {
                seleccionarRecibo(event.target.files?.[0]);
                event.currentTarget.value = "";
              }}
              type="file"
            />
            {reciboEscaneado ? (
              <div className="flex flex-wrap items-center justify-between gap-2 rounded-md border bg-background p-3 text-sm">
                <span className="min-w-0 truncate">{reciboEscaneado.name}</span>
                <button
                  className="rounded-md border px-2 py-1 text-xs text-destructive hover:bg-destructive/10"
                  disabled={isSubmitting}
                  onClick={() => setReciboEscaneado(null)}
                  type="button"
                >
                  Quitar
                </button>
              </div>
            ) : null}
          </section>
          {fileErrors.length > 0 ? (
            <div className="rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive">
              {fileErrors.map((error) => <p key={error}>{error}</p>)}
            </div>
          ) : null}
        </>
      ) : null}
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
