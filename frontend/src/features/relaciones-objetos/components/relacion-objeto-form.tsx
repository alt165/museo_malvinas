"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import { useForm, useWatch } from "react-hook-form";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import { ObjetoSearchSelector } from "@/features/objetos/components/objeto-search-selector";
import { useObjetoQuery } from "@/features/objetos/queries";
import type { ObjetoMuseoResponseDTO } from "@/features/objetos/types";
import { ApiClientError } from "@/lib/errors/api-error";
import type { RelacionObjetoRequestDTO, RelacionObjetoResponseDTO } from "../types";
import { relacionObjetoSchema, type RelacionObjetoFormValues } from "../schemas";
import { getApiErrorMessage, getValidationErrors } from "../utils";

type RelacionObjetoFormProps = {
  initialValue?: RelacionObjetoResponseDTO;
  isSubmitting?: boolean;
  submitError?: unknown;
  submitLabel: string;
  defaultObjetoOrigenId?: number;
  onSubmit: (payload: RelacionObjetoRequestDTO) => void;
};

export function RelacionObjetoForm({
  initialValue,
  isSubmitting = false,
  onSubmit,
  submitError,
  submitLabel,
  defaultObjetoOrigenId
}: RelacionObjetoFormProps) {
  const isEditMode = Boolean(initialValue);
  const tieneOrigenPrecargado = !initialValue && Boolean(defaultObjetoOrigenId);
  const origenPrecargadoQuery = useObjetoQuery(tieneOrigenPrecargado && defaultObjetoOrigenId ? defaultObjetoOrigenId : NaN);
  const [objetoOrigen, setObjetoOrigen] = useState<ObjetoMuseoResponseDTO | null>(null);
  const [objetoDestino, setObjetoDestino] = useState<ObjetoMuseoResponseDTO | null>(null);
  const [modalDestinoAbierto, setModalDestinoAbierto] = useState(false);
  const {
    formState: { errors },
    handleSubmit,
    register,
    setError,
    setValue,
    control
  } = useForm<RelacionObjetoFormValues>({
    resolver: zodResolver(relacionObjetoSchema),
    defaultValues: {
      objetoOrigenId: initialValue?.objetoOrigenId ?? defaultObjetoOrigenId ?? 0,
      objetoDestinoId: initialValue?.objetoDestinoId ?? 0,
      tipoRelacion: initialValue?.tipoRelacion ?? "",
      descripcion: initialValue?.descripcion ?? ""
    }
  });
  const objetoOrigenId = useWatch({ control, name: "objetoOrigenId" });
  const objetoDestinoId = useWatch({ control, name: "objetoDestinoId" });

  useEffect(() => {
    const validationErrors = getValidationErrors(submitError);

    Object.entries(validationErrors).forEach(([field, message]) => {
      if (
        field === "objetoOrigenId" ||
        field === "objetoDestinoId" ||
        field === "tipoRelacion" ||
        field === "descripcion"
      ) {
        setError(field, { message });
      }
    });
  }, [setError, submitError]);

  if (origenPrecargadoQuery.isLoading) {
    return <LoadingState label="Cargando objeto origen..." />;
  }

  if (origenPrecargadoQuery.isError) {
    return (
      <ErrorState
        message={getApiErrorMessage(origenPrecargadoQuery.error)}
        requestId={origenPrecargadoQuery.error instanceof ApiClientError ? origenPrecargadoQuery.error.requestId : undefined}
      />
    );
  }

  const objetoOrigenSeleccionado = objetoOrigen ?? origenPrecargadoQuery.data ?? null;

  function handleSeleccionarOrigen(objeto: ObjetoMuseoResponseDTO) {
    setObjetoOrigen(objeto);
    setValue("objetoOrigenId", objeto.id, { shouldDirty: true, shouldValidate: true });

    if (objetoDestinoId === objeto.id) {
      setObjetoDestino(null);
      setModalDestinoAbierto(false);
      setValue("objetoDestinoId", 0, { shouldDirty: true, shouldValidate: true });
    }
  }

  function handleAbrirModalDestino(objeto: ObjetoMuseoResponseDTO) {
    if (objeto.id === objetoOrigenId) {
      setError("objetoDestinoId", { message: "El objeto origen y destino no pueden ser el mismo" });
      return;
    }

    setObjetoDestino(objeto);
    setValue("objetoDestinoId", objeto.id, { shouldDirty: true, shouldValidate: true });
    setModalDestinoAbierto(true);
  }

  const handleGuardar = handleSubmit((values) =>
    onSubmit({
      objetoOrigenId: values.objetoOrigenId,
      objetoDestinoId: values.objetoDestinoId,
      tipoRelacion: values.tipoRelacion.trim(),
      descripcion: values.descripcion?.trim() || null
    })
  );

  return (
    <div className="w-full space-y-5">
      <input type="hidden" {...register("objetoOrigenId", { valueAsNumber: true })} />
      <input type="hidden" {...register("objetoDestinoId", { valueAsNumber: true })} />

      {isEditMode && initialValue ? (
        <div className="space-y-5 rounded-lg border p-5">
          <div className="grid gap-4 sm:grid-cols-2">
            <ObjetoResumen
              label="Objeto origen"
              nombre={initialValue.objetoOrigenNombre}
              numeroInventario={initialValue.objetoOrigenNumeroInventario}
            />
            <ObjetoResumen
              label="Objeto destino"
              nombre={initialValue.objetoDestinoNombre}
              numeroInventario={initialValue.objetoDestinoNumeroInventario}
            />
          </div>
          <RelacionCampos
            descripcionField={register("descripcion")}
            descripcionError={errors.descripcion?.message}
            tipoRelacionField={register("tipoRelacion")}
            tipoRelacionError={errors.tipoRelacion?.message}
          />
          <FormActions isSubmitting={isSubmitting} onCancelHref="/relaciones-objetos" onSubmit={() => void handleGuardar()} submitLabel={submitLabel} />
        </div>
      ) : (
        <div className="space-y-5">
          {objetoOrigenSeleccionado ? (
            <ObjetoResumen
              label="Objeto origen"
              nombre={objetoOrigenSeleccionado.denominacionObjeto}
              numeroInventario={objetoOrigenSeleccionado.numeroInventario}
            />
          ) : (
            <div className="space-y-2">
              <ObjetoSearchSelector
                description="Busque y seleccione el objeto que inicia la relacion."
                onSelect={handleSeleccionarOrigen}
                selectLabel="Seleccionar origen"
                selectedObjeto={objetoOrigenSeleccionado}
                title="Paso 1: buscar objeto origen"
              />
              {errors.objetoOrigenId?.message ? <p className="text-sm font-medium text-destructive">{errors.objetoOrigenId.message}</p> : null}
            </div>
          )}

          <div className="space-y-2">
            {objetoOrigenSeleccionado ? (
              <ObjetoSearchSelector
                description="Busque el objeto destino. El objeto origen queda excluido de los resultados."
                emptyLabel="No hay objetos destino que coincidan con los filtros aplicados."
                excludeObjetoId={objetoOrigenSeleccionado.id}
                renderActions={(objeto) => (
                  <button
                    className="rounded-md border px-3 py-1.5 text-xs font-medium text-[#163A61] hover:bg-muted"
                    onClick={() => handleAbrirModalDestino(objeto)}
                    type="button"
                  >
                    Seleccionar destino
                  </button>
                )}
                selectedObjeto={objetoDestino}
                title="Buscar objeto destino"
              />
            ) : (
              <div className="rounded-lg border border-dashed p-5 text-sm text-muted-foreground">
                Seleccione primero un objeto origen para buscar el objeto destino.
              </div>
            )}
            {errors.objetoDestinoId?.message ? <p className="text-sm font-medium text-destructive">{errors.objetoDestinoId.message}</p> : null}
          </div>

          <div className="flex items-center gap-3">
            <Link className="inline-flex h-10 items-center justify-center rounded-md border px-4 text-sm hover:bg-muted" href="/relaciones-objetos">
              Cancelar
            </Link>
          </div>
        </div>
      )}

      {!isEditMode && modalDestinoAbierto && objetoOrigenSeleccionado && objetoDestino ? (
        <RelacionDestinoModal
          destino={objetoDestino}
          descripcionField={register("descripcion")}
          descripcionError={errors.descripcion?.message}
          isSubmitting={isSubmitting}
          onClose={() => setModalDestinoAbierto(false)}
          onSubmit={() => void handleGuardar()}
          origen={objetoOrigenSeleccionado}
          submitLabel={submitLabel}
          tipoRelacionField={register("tipoRelacion")}
          tipoRelacionError={errors.tipoRelacion?.message}
        />
      ) : null}
    </div>
  );
}

function RelacionDestinoModal({
  destino,
  descripcionError,
  descripcionField,
  isSubmitting,
  onClose,
  onSubmit,
  origen,
  submitLabel,
  tipoRelacionError,
  tipoRelacionField
}: {
  destino: ObjetoMuseoResponseDTO;
  descripcionError?: string;
  descripcionField: ReturnType<typeof useForm<RelacionObjetoFormValues>>["register"] extends (...args: never[]) => infer R ? R : never;
  isSubmitting: boolean;
  onClose: () => void;
  onSubmit: () => void;
  origen: ObjetoMuseoResponseDTO;
  submitLabel: string;
  tipoRelacionError?: string;
  tipoRelacionField: ReturnType<typeof useForm<RelacionObjetoFormValues>>["register"] extends (...args: never[]) => infer R ? R : never;
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-2xl rounded-lg border bg-white p-5 shadow-xl">
        <div className="flex items-start justify-between gap-4 border-b pb-4">
          <div>
            <h2 className="text-lg font-semibold text-[#163A61]">Nueva relacion</h2>
            <p className="mt-1 text-sm text-muted-foreground">Complete los datos para vincular el objeto origen con el destino seleccionado.</p>
          </div>
          <button className="rounded-md border px-3 py-1.5 text-sm hover:bg-muted" onClick={onClose} type="button">
            Cerrar
          </button>
        </div>

        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          <ObjetoResumen label="Origen" nombre={origen.denominacionObjeto} numeroInventario={origen.numeroInventario} />
          <ObjetoResumen label="Destino" nombre={destino.denominacionObjeto} numeroInventario={destino.numeroInventario} />
        </div>

        <div className="mt-5 space-y-5">
          <RelacionCampos
            descripcionField={descripcionField}
            descripcionError={descripcionError}
            tipoRelacionField={tipoRelacionField}
            tipoRelacionError={tipoRelacionError}
          />
          <FormActions isSubmitting={isSubmitting} onCancel={onClose} onSubmit={onSubmit} submitLabel={submitLabel} />
        </div>
      </div>
    </div>
  );
}

function RelacionCampos({
  descripcionError,
  descripcionField,
  tipoRelacionError,
  tipoRelacionField
}: {
  descripcionError?: string;
  descripcionField: ReturnType<typeof useForm<RelacionObjetoFormValues>>["register"] extends (...args: never[]) => infer R ? R : never;
  tipoRelacionError?: string;
  tipoRelacionField: ReturnType<typeof useForm<RelacionObjetoFormValues>>["register"] extends (...args: never[]) => infer R ? R : never;
}) {
  return (
    <>
      <Field label="Tipo de relacion" error={tipoRelacionError}>
        <input
          className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
          {...tipoRelacionField}
        />
      </Field>
      <Field label="Descripcion" error={descripcionError}>
        <textarea
          className="min-h-32 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
          {...descripcionField}
        />
      </Field>
    </>
  );
}

function FormActions({
  isSubmitting,
  onCancel,
  onCancelHref,
  onSubmit,
  submitLabel
}: {
  isSubmitting: boolean;
  onCancel?: () => void;
  onCancelHref?: string;
  onSubmit: () => void;
  submitLabel: string;
}) {
  return (
    <div className="flex items-center gap-3">
      <button
        className="inline-flex h-10 items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-primary-foreground hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
        disabled={isSubmitting}
        onClick={onSubmit}
        type="button"
      >
        {isSubmitting ? "Guardando..." : submitLabel}
      </button>
      {onCancelHref ? (
        <Link className="inline-flex h-10 items-center justify-center rounded-md border px-4 text-sm hover:bg-muted" href={onCancelHref}>
          Cancelar
        </Link>
      ) : (
        <button className="inline-flex h-10 items-center justify-center rounded-md border px-4 text-sm hover:bg-muted" onClick={onCancel} type="button">
          Cancelar
        </button>
      )}
    </div>
  );
}

function ObjetoResumen({
  label,
  nombre,
  numeroInventario
}: {
  label: string;
  nombre?: string | null;
  numeroInventario?: string | null;
}) {
  return (
    <div className="rounded-md border bg-surface p-3 text-sm">
      <p className="font-medium text-primary">{label}</p>
      <p className="mt-1">{nombre || "Objeto sin denominacion"}</p>
      <p className="text-muted-foreground">Numero de inventario: {numeroInventario || "Sin numero"}</p>
    </div>
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
