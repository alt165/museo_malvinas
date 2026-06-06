"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { useCategoriasQuery } from "@/features/categorias/queries";
import { useBuscarDepositantePorIdentificacionMutation, useBuscarDepositantesPorNombreQuery } from "@/features/depositantes/queries";
import type { DepositanteResponseDTO } from "@/features/depositantes/types";
import { identificacionVisible, telefonoVisible } from "@/features/depositantes/utils";
import { useUbicacionesQuery } from "@/features/ubicaciones/queries";
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
const caracteresConVencimiento = new Set(["PRESTAMO", "COMODATO"]);
const caracteresRecepcion = [
  ["PRESTAMO", "Préstamo"],
  ["COMODATO", "Comodato"],
  ["DONACION", "Donación"],
  ["COMPRA", "Compra"],
  ["ESTUDIO", "Estudio"],
  ["OTRO", "Otro"]
] as const;

function tipoDepositanteLabel(depositante: DepositanteResponseDTO) {
  return depositante.tipo === "PERSONA" ? "Persona" : "Institucion";
}

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
  const ubicacionesQuery = useUbicacionesQuery();
  const categoriasOrdenadas = [...categorias].sort((a, b) => a.nombre.localeCompare(b.nombre));
  const [categoriaBusqueda, setCategoriaBusqueda] = useState("");
  const [fotos, setFotos] = useState<File[]>([]);
  const [reciboEscaneado, setReciboEscaneado] = useState<File | null>(null);
  const [fileErrors, setFileErrors] = useState<string[]>([]);
  const [identificacion, setIdentificacion] = useState("");
  const [nombreDepositante, setNombreDepositante] = useState("");
  const [nombreDepositanteDebounced, setNombreDepositanteDebounced] = useState("");
  const [depositanteSeleccionado, setDepositanteSeleccionado] = useState<DepositanteResponseDTO | null>(() => {
    if (!initialValue?.depositanteId || !initialValue.depositanteNombre) {
      return null;
    }
    return {
      id: initialValue.depositanteId,
      nombre: initialValue.depositanteNombre,
      tipo: "PERSONA",
      activo: true
    } as DepositanteResponseDTO;
  });
  const buscarDepositanteMutation = useBuscarDepositantePorIdentificacionMutation();
  const depositantesPorNombreQuery = useBuscarDepositantesPorNombreQuery(nombreDepositanteDebounced);
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
      categoriaIds: initialValue?.categorias?.map((categoria) => categoria.id) ?? [],
      ubicacionId: initialValue?.ubicacionId ?? 0,
      depositanteId: initialValue?.depositanteId ?? 0,
      caracterRecepcion: initialValue?.caracterRecepcion === "RECEPCION" ? "" : initialValue?.caracterRecepcion ?? "",
      fechaVencimiento: initialValue?.fechaVencimiento ?? ""
    }
  });
  const watchedCategoriaIds = useWatch({ control, name: "categoriaIds", defaultValue: [] });
  const caracterRecepcion = useWatch({ control, name: "caracterRecepcion", defaultValue: initialValue?.caracterRecepcion === "RECEPCION" ? "" : initialValue?.caracterRecepcion ?? "" });
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
    const timeout = window.setTimeout(() => setNombreDepositanteDebounced(nombreDepositante.trim()), 300);
    return () => window.clearTimeout(timeout);
  }, [nombreDepositante]);

  useEffect(() => {
    if (!caracteresConVencimiento.has(caracterRecepcion)) {
      setValue("fechaVencimiento", "", { shouldDirty: true, shouldValidate: true });
    }
  }, [caracterRecepcion, setValue]);

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
        field === "categoriaIds" ||
        field === "ubicacionId" ||
        field === "depositanteId" ||
        field === "caracterRecepcion" ||
        field === "fechaVencimiento"
      ) {
        setError(field, { message });
      }
    });
  }, [setError, submitError]);

  function seleccionarDepositante(depositante: DepositanteResponseDTO) {
    setDepositanteSeleccionado(depositante);
    setValue("depositanteId", depositante.id, { shouldDirty: true, shouldValidate: true });
  }

  const resultadosNombre = depositantesPorNombreQuery.data ?? [];
  const buscandoPorNombre = Boolean(nombreDepositante.trim()) && (nombreDepositante.trim() !== nombreDepositanteDebounced || depositantesPorNombreQuery.isFetching);
  const mostrarFechaVencimiento = caracteresConVencimiento.has(caracterRecepcion);
  const fechaMinimaVencimiento = initialValue?.fechaIngreso ?? new Date().toISOString().slice(0, 10);

  function limpiarDepositante() {
    setDepositanteSeleccionado(null);
    setValue("depositanteId", 0, { shouldDirty: true, shouldValidate: true });
  }

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
      className="w-full space-y-5 rounded-lg border p-5"
      onSubmit={handleSubmit((values) =>
        onSubmit({
          numeroInventario: values.numeroInventario.trim(),
          denominacionObjeto: values.denominacionObjeto.trim(),
          descripcion: values.descripcion?.trim() || null,
          descripcionTecnica: values.descripcionTecnica?.trim() || null,
          materiales: values.materiales?.trim() || null,
          dimensiones: values.dimensiones?.trim() || null,
          estadoConservacion: values.estadoConservacion || null,
          categoriaIds: values.categoriaIds ?? [],
          ubicacionId: values.ubicacionId && values.ubicacionId > 0 ? Number(values.ubicacionId) : null,
          depositanteId: values.depositanteId,
          caracterRecepcion: values.caracterRecepcion || null,
          fechaVencimiento: caracteresConVencimiento.has(values.caracterRecepcion) ? values.fechaVencimiento || null : null
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
      <input type="hidden" {...register("depositanteId", { valueAsNumber: true })} />
      <section className="space-y-3 rounded-md border bg-surface p-4">
        <div>
          <h2 className="text-base font-semibold">Depositante y recepción</h2>
        </div>
        <div className="grid gap-4 lg:grid-cols-2">
          <div className="space-y-2">
            <label className="text-sm font-medium" htmlFor="depositante-identificacion">DNI o CUIT del depositante</label>
            <div className="flex gap-2">
              <input
                className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
                id="depositante-identificacion"
                onChange={(event) => { setIdentificacion(event.target.value); limpiarDepositante(); buscarDepositanteMutation.reset(); }}
                value={identificacion}
              />
              <button
                className="h-10 rounded-md border px-4 text-sm font-medium hover:bg-muted disabled:opacity-60"
                disabled={buscarDepositanteMutation.isPending || !identificacion.trim()}
                onClick={() => buscarDepositanteMutation.mutate(identificacion.trim(), { onSuccess: seleccionarDepositante, onError: limpiarDepositante })}
                type="button"
              >
                {buscarDepositanteMutation.isPending ? "Buscando..." : "Buscar"}
              </button>
            </div>
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium" htmlFor="depositante-nombre">Buscar depositante por nombre</label>
            <input
              className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
              id="depositante-nombre"
              onChange={(event) => { setNombreDepositante(event.target.value); limpiarDepositante(); }}
              value={nombreDepositante}
            />
          </div>
        </div>
        {buscandoPorNombre ? <p className="text-sm text-muted-foreground">Buscando depositantes...</p> : null}
        {resultadosNombre.length > 0 ? (
          <div className="grid gap-2 md:grid-cols-2">
            {resultadosNombre.map((depositante) => (
              <button className="rounded-md border bg-background p-3 text-left text-sm hover:bg-muted" key={depositante.id} onClick={() => seleccionarDepositante(depositante)} type="button">
                <span className="block font-medium">{depositante.nombre}</span>
                <span className="mt-1 block text-muted-foreground">{identificacionVisible(depositante)}</span>
              </button>
            ))}
          </div>
        ) : null}
        {depositanteSeleccionado ? (
          <div className="rounded-md border border-primary/20 bg-secondary/20 p-3 text-sm">
            <p className="font-medium">{depositanteSeleccionado.nombre}</p>
            <p className="mt-1 text-muted-foreground">{tipoDepositanteLabel(depositanteSeleccionado)} - {identificacionVisible(depositanteSeleccionado)}</p>
            {[depositanteSeleccionado.contacto, telefonoVisible(depositanteSeleccionado)].filter(Boolean).length > 0 ? (
              <p className="mt-1 text-muted-foreground">{[depositanteSeleccionado.contacto, telefonoVisible(depositanteSeleccionado)].filter(Boolean).join(" - ")}</p>
            ) : null}
          </div>
        ) : null}
        {buscarDepositanteMutation.isError ? <p className="text-sm text-destructive">No se encontró un depositante con esa identificación.</p> : null}
        {errors.depositanteId ? <p className="text-sm text-destructive">{errors.depositanteId.message}</p> : null}
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-2">
            <label className="text-sm font-medium" htmlFor="caracterRecepcion">Carácter de recepción</label>
            <select className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id="caracterRecepcion" {...register("caracterRecepcion")}>
              <option value="">Seleccionar carácter</option>
              {caracteresRecepcion.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
            {errors.caracterRecepcion ? <p className="text-sm text-destructive">{errors.caracterRecepcion.message}</p> : null}
          </div>
          {mostrarFechaVencimiento ? (
            <div className="space-y-2">
              <label className="text-sm font-medium" htmlFor="fechaVencimiento">Fecha de vencimiento</label>
              <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id="fechaVencimiento" type="date" min={fechaMinimaVencimiento} {...register("fechaVencimiento")} />
              {errors.fechaVencimiento ? <p className="text-sm text-destructive">{errors.fechaVencimiento.message}</p> : null}
            </div>
          ) : null}
        </div>
      </section>
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
      {!initialValue ? (
        <div className="space-y-2">
          <label className="text-sm font-medium" htmlFor="ubicacionId">
            Ubicacion inicial
          </label>
          <select
            className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
            disabled={isSubmitting || ubicacionesQuery.isLoading}
            id="ubicacionId"
            {...register("ubicacionId", { valueAsNumber: true })}
          >
            <option value={0}>Seleccionar ubicacion</option>
            {(ubicacionesQuery.data ?? []).map((ubicacion) => (
              <option key={ubicacion.id} value={ubicacion.id}>
                {ubicacion.nombre}
              </option>
            ))}
          </select>
          {ubicacionesQuery.isError ? <p className="text-sm text-destructive">No se pudieron cargar las ubicaciones.</p> : null}
          {errors.ubicacionId ? <p className="text-sm text-destructive">{errors.ubicacionId.message}</p> : null}
        </div>
      ) : null}
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
