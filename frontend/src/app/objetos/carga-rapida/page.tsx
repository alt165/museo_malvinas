"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { ErrorState } from "@/components/common/error-state";
import { PageHeader } from "@/components/common/page-header";
import { AppShell } from "@/components/layout/app-shell";
import { useBuscarDepositantePorIdentificacionMutation, useBuscarDepositantesPorNombreQuery } from "@/features/depositantes/queries";
import type { DepositanteResponseDTO } from "@/features/depositantes/types";
import { identificacionVisible, telefonoVisible } from "@/features/depositantes/utils";
import { descargarReciboIngresoPdf } from "@/features/objetos/recibos";
import { cargaRapidaObjetoSchema, type CargaRapidaObjetoFormValues } from "@/features/objetos/schemas";
import type { CargaRapidaObjetoResponseDTO } from "@/features/objetos/types";
import { getApiErrorMessage, getValidationErrors } from "@/features/objetos/utils";
import { ApiClientError } from "@/lib/errors/api-error";
import { routePermissions } from "@/lib/routes";
import { useCargaRapidaObjetoMutation } from "@/features/objetos/queries";
import { useEffect, useState } from "react";

function tipoDepositanteLabel(depositante: DepositanteResponseDTO) {
  return depositante.tipo === "PERSONA" ? "Persona" : "Institucion";
}

export default function CargaRapidaObjetoPage() {
  const [resultado, setResultado] = useState<CargaRapidaObjetoResponseDTO | null>(null);
  const [downloadError, setDownloadError] = useState<string | null>(null);
  const [identificacion, setIdentificacion] = useState("");
  const [nombreDepositante, setNombreDepositante] = useState("");
  const [nombreDepositanteDebounced, setNombreDepositanteDebounced] = useState("");
  const [depositanteSeleccionado, setDepositanteSeleccionado] = useState<DepositanteResponseDTO | null>(null);
  const buscarDepositanteMutation = useBuscarDepositantePorIdentificacionMutation();
  const depositantesPorNombreQuery = useBuscarDepositantesPorNombreQuery(nombreDepositanteDebounced);
  const mutation = useCargaRapidaObjetoMutation();
  const {
    formState: { errors },
    handleSubmit,
    register,
    reset,
    setError,
    setValue
  } = useForm<CargaRapidaObjetoFormValues>({
    resolver: zodResolver(cargaRapidaObjetoSchema),
    defaultValues: {
      depositanteId: 0,
      denominacionObjeto: "",
      numeroInventario: "",
      descripcionBreve: ""
    }
  });

  useEffect(() => {
    const validationErrors = getValidationErrors(mutation.error);
    Object.entries(validationErrors).forEach(([field, message]) => {
      if (field === "depositanteId" || field === "denominacionObjeto" || field === "numeroInventario" || field === "descripcionBreve") {
        setError(field, { message });
      }
    });
  }, [mutation.error, setError]);

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      setNombreDepositanteDebounced(nombreDepositante.trim());
    }, 350);

    return () => window.clearTimeout(timeout);
  }, [nombreDepositante]);

  function seleccionarDepositante(depositante: DepositanteResponseDTO) {
    setDepositanteSeleccionado(depositante);
    setValue("depositanteId", depositante.id, { shouldDirty: true, shouldValidate: true });
  }

  const resultadosNombre = depositantesPorNombreQuery.data ?? [];
  const buscandoPorNombre = Boolean(nombreDepositante.trim()) && (nombreDepositante.trim() !== nombreDepositanteDebounced || depositantesPorNombreQuery.isFetching);
  const mostrarSinResultadosNombre = Boolean(nombreDepositanteDebounced) && !depositantesPorNombreQuery.isFetching && !depositantesPorNombreQuery.isError && resultadosNombre.length === 0;

  return (
    <AppShell requiredRoles={[...routePermissions.write]}>
      <div className="space-y-6">
        <PageHeader
          actions={
            <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href="/objetos">
              Volver
            </Link>
          }
          description="Ingreso minimo de un objeto y emision de recibo para el depositante."
          title="Carga rapida de objeto"
        />
        {mutation.isError ? (
          <ErrorState
            message={getApiErrorMessage(mutation.error)}
            requestId={mutation.error instanceof ApiClientError ? mutation.error.requestId : undefined}
          />
        ) : null}
        {resultado ? (
          <div className="rounded-lg border p-5 text-sm">
            <p className="font-medium">Objeto creado: {resultado.objeto.numeroInventario}</p>
            <p className="mt-1 text-muted-foreground">Recibo emitido: {resultado.recibo.numeroRecibo}</p>
            <div className="mt-4 flex flex-wrap gap-2">
              <Link className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" href={`/objetos/${resultado.objeto.id}`}>
                Ver objeto
              </Link>
              <button
                className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted"
                onClick={async () => {
                  setDownloadError(null);
                  try {
                    await descargarReciboIngresoPdf(resultado.recibo);
                  } catch {
                    setDownloadError("No se pudo descargar el recibo. Intentalo nuevamente.");
                  }
                }}
                type="button"
              >
                Descargar recibo
              </button>
            </div>
            {downloadError ? <p className="mt-3 text-sm text-destructive">{downloadError}</p> : null}
          </div>
        ) : null}
        <form
          className="w-full space-y-5 rounded-lg border p-5"
          onSubmit={handleSubmit((values) =>
            mutation.mutate(
              {
                depositanteId: values.depositanteId,
                denominacionObjeto: values.denominacionObjeto.trim(),
                numeroInventario: values.numeroInventario.trim(),
                descripcionBreve: values.descripcionBreve.trim()
              },
              { onSuccess: (data) => {
                setDownloadError(null);
                setResultado(data);
                reset({
                  depositanteId: 0,
                  denominacionObjeto: "",
                  numeroInventario: "",
                  descripcionBreve: ""
                });
                setIdentificacion("");
                setNombreDepositante("");
                setNombreDepositanteDebounced("");
                setDepositanteSeleccionado(null);
                buscarDepositanteMutation.reset();
              } }
            )
          )}
        >
          <input type="hidden" {...register("depositanteId", { valueAsNumber: true })} />
          <section className="space-y-3">
            <div>
              <h2 className="text-base font-semibold">Buscar depositante</h2>
              <p className="mt-1 text-sm text-muted-foreground">Selecciona un depositante existente por DNI/CUIT o por nombre.</p>
            </div>
            <div className="grid gap-3 sm:grid-cols-[1fr_auto]">
              <label className="space-y-2 text-sm font-medium">
                <span>DNI o CUIT del depositante</span>
                <input
                  className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
                  onChange={(event) => {
                    setIdentificacion(event.target.value);
                    setDepositanteSeleccionado(null);
                    setValue("depositanteId", 0, { shouldDirty: true, shouldValidate: true });
                    buscarDepositanteMutation.reset();
                  }}
                  value={identificacion}
                />
              </label>
              <button
                className="mt-7 inline-flex h-10 items-center justify-center rounded-md border px-4 text-sm font-medium hover:bg-muted disabled:opacity-60"
                disabled={buscarDepositanteMutation.isPending || !identificacion.trim()}
                onClick={() => {
                  buscarDepositanteMutation.mutate(identificacion.trim(), {
                    onSuccess: (depositante) => {
                      seleccionarDepositante(depositante);
                    },
                    onError: () => {
                      setDepositanteSeleccionado(null);
                      setValue("depositanteId", 0, { shouldDirty: true, shouldValidate: true });
                    }
                  });
                }}
                type="button"
              >
                {buscarDepositanteMutation.isPending ? "Buscando..." : "Buscar"}
              </button>
            </div>
            <div className="space-y-3">
              <label className="space-y-2 text-sm font-medium">
                <span>Buscar depositante por nombre</span>
                <input
                  className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
                  onChange={(event) => {
                    setNombreDepositante(event.target.value);
                    setDepositanteSeleccionado(null);
                    setValue("depositanteId", 0, { shouldDirty: true, shouldValidate: true });
                  }}
                  placeholder="Nombre, apellido u organizacion"
                  value={nombreDepositante}
                />
              </label>
              {buscandoPorNombre ? <p className="text-sm text-muted-foreground">Buscando depositantes...</p> : null}
              {mostrarSinResultadosNombre ? <p className="text-sm text-muted-foreground">No se encontraron depositantes con ese nombre.</p> : null}
              {depositantesPorNombreQuery.isError ? (
                <ErrorState
                  message={getApiErrorMessage(depositantesPorNombreQuery.error)}
                  requestId={depositantesPorNombreQuery.error instanceof ApiClientError ? depositantesPorNombreQuery.error.requestId : undefined}
                />
              ) : null}
              {resultadosNombre.length > 0 ? (
                <div className="overflow-hidden rounded-md border">
                  {resultadosNombre.map((depositante) => (
                    <button
                      className="flex w-full items-start justify-between gap-4 border-b px-4 py-3 text-left text-sm hover:bg-muted/60 last:border-b-0"
                      key={depositante.id}
                      onClick={() => seleccionarDepositante(depositante)}
                      type="button"
                    >
                      <span>
                        <span className="block font-medium">{depositante.nombre}</span>
                        <span className="mt-1 block text-muted-foreground">{identificacionVisible(depositante)}</span>
                      </span>
                      <span className="shrink-0 rounded-md border px-2 py-1 text-xs text-muted-foreground">{tipoDepositanteLabel(depositante)}</span>
                    </button>
                  ))}
                </div>
              ) : null}
            </div>
            {depositanteSeleccionado ? (
              <div className="rounded-md border bg-muted/30 p-4 text-sm">
                <p className="font-medium">{depositanteSeleccionado.nombre}</p>
                <p className="mt-1 text-muted-foreground">{tipoDepositanteLabel(depositanteSeleccionado)} - {identificacionVisible(depositanteSeleccionado)}</p>
                {[depositanteSeleccionado.contacto, telefonoVisible(depositanteSeleccionado)]
                  .filter((item) => item && item !== "Sin telefono")
                  .length > 0 ? (
                  <p className="mt-1 text-muted-foreground">
                    {[depositanteSeleccionado.contacto, telefonoVisible(depositanteSeleccionado)]
                      .filter((item) => item && item !== "Sin telefono")
                      .join(" / ")}
                  </p>
                ) : null}
              </div>
            ) : null}
            {buscarDepositanteMutation.error instanceof ApiClientError && buscarDepositanteMutation.error.status === 404 ? (
              <div className="rounded-md border border-destructive/30 bg-destructive/5 p-4 text-sm">
                <p className="text-destructive">No se encontro un depositante con ese DNI/CUIT.</p>
                <Link
                  className="mt-3 inline-flex h-10 items-center justify-center rounded-md border bg-background px-4 text-sm font-medium hover:bg-muted"
                  href={`/depositantes/nuevo?identificacion=${encodeURIComponent(identificacion.trim())}`}
                >
                  Dar de alta depositante
                </Link>
              </div>
            ) : null}
            {buscarDepositanteMutation.isError && !(buscarDepositanteMutation.error instanceof ApiClientError && buscarDepositanteMutation.error.status === 404) ? (
              <ErrorState
                message={getApiErrorMessage(buscarDepositanteMutation.error)}
                requestId={buscarDepositanteMutation.error instanceof ApiClientError ? buscarDepositanteMutation.error.requestId : undefined}
              />
            ) : null}
            {errors.depositanteId ? <p className="text-sm text-destructive">{errors.depositanteId.message}</p> : null}
          </section>
          <div className="grid gap-5 sm:grid-cols-2">
            <div className="space-y-2">
              <label className="text-sm font-medium" htmlFor="denominacionObjeto">Denominacion</label>
              <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id="denominacionObjeto" {...register("denominacionObjeto")} />
              {errors.denominacionObjeto ? <p className="text-sm text-destructive">{errors.denominacionObjeto.message}</p> : null}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium" htmlFor="numeroInventario">Numero de inventario</label>
              <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id="numeroInventario" {...register("numeroInventario")} />
              {errors.numeroInventario ? <p className="text-sm text-destructive">{errors.numeroInventario.message}</p> : null}
            </div>
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium" htmlFor="descripcionBreve">Descripcion breve</label>
            <textarea className="min-h-28 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" id="descripcionBreve" {...register("descripcionBreve")} />
            {errors.descripcionBreve ? <p className="text-sm text-destructive">{errors.descripcionBreve.message}</p> : null}
          </div>
          <button className="inline-flex h-10 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:opacity-60" disabled={mutation.isPending} type="submit">
            {mutation.isPending ? "Generando..." : "Crear y generar recibo"}
          </button>
        </form>
      </div>
    </AppShell>
  );
}
