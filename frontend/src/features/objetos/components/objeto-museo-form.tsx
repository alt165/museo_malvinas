"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { type ReactNode, useEffect, useMemo, useRef, useState } from "react";
import { type FieldErrors, useForm, useWatch } from "react-hook-form";
import { useCategoriasQuery } from "@/features/categorias/queries";
import { useBuscarDepositantePorIdentificacionMutation, useBuscarDepositantesPorNombreQuery } from "@/features/depositantes/queries";
import type { DepositanteResponseDTO } from "@/features/depositantes/types";
import { identificacionVisible, telefonoVisible } from "@/features/depositantes/utils";
import { useDetallesConservacionQuery } from "@/features/tablas-auxiliares/queries";
import { useUbicacionesQuery } from "@/features/ubicaciones/queries";
import { ApiClientError } from "@/lib/errors/api-error";
import type { ObjetoMuseoRequestDTO, ObjetoMuseoResponseDTO, VisibilidadCampo } from "../types";
import { objetoMuseoSchema, type ObjetoMuseoFormValues } from "../schemas";
import { getValidationErrors } from "../utils";

type ObjetoMuseoFormProps = {
  allowFileUploads?: boolean;
  initialValue?: ObjetoMuseoResponseDTO;
  isSubmitting?: boolean;
  submitError?: unknown;
  submitLabel: string;
  footerContent?: ReactNode;
  onSubmit: (payload: ObjetoMuseoRequestDTO, archivos: ObjetoMuseoFormFiles) => void;
  resetSignal?: number;
};

export type ObjetoMuseoFormFiles = {
  fotos: File[];
  fotoVisibilidades: VisibilidadCampo[];
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

const camposVisibilidad = [
  "numeroInventario", "denominacionObjeto", "descripcion", "descripcionTecnica", "materiales",
  "alto", "ancho", "diametro", "espesor", "peso", "inscripciones", "regimenPropiedad",
  "condicionLegalBien", "estadoConservacion", "detallesEstadoConservacion", "intervencionesInadecuadas",
  "estadoIntegridad", "humedadConservacion", "temperaturaConservacion", "luzConservacion",
  "conservacionExtintores", "conservacionMontaje", "conservacionSistemaElectrico", "conservacionAlarmas",
  "conservacionCamaras", "ubicacion", "depositante", "caracterRecepcion", "fechaVencimiento", "categorias"
] as const;

function visibilidadesDefault(initialValue?: ObjetoMuseoResponseDTO) {
  return Object.fromEntries(camposVisibilidad.map((campo) => [campo, initialValue?.visibilidades?.[campo] ?? "PUBLICO"])) as Record<string, VisibilidadCampo>;
}

function booleanFormValue(value?: boolean | null) {
  if (value === true) return "true";
  if (value === false) return "false";
  return "";
}

function booleanPayload(value?: string) {
  if (value === "true") return true;
  if (value === "false") return false;
  return null;
}

const objetoMuseoFieldLabels: Partial<Record<keyof ObjetoMuseoFormValues, string>> = {
  numeroInventario: "Número de inventario",
  denominacionObjeto: "Denominación",
  descripcion: "Descripción breve",
  descripcionTecnica: "Descripción técnica",
  materiales: "Materiales",
  alto: "Alto",
  ancho: "Ancho",
  diametro: "Diámetro",
  espesor: "Espesor",
  peso: "Peso",
  inscripciones: "Inscripciones",
  regimenPropiedad: "Régimen de propiedad",
  condicionLegalBien: "Condición legal del bien",
  estadoConservacion: "Estado de conservación",
  detallesEstadoConservacion: "Detalles del estado de conservación",
  intervencionesInadecuadas: "Intervenciones inadecuadas",
  estadoIntegridad: "Estado de integridad",
  humedadConservacion: "Humedad",
  temperaturaConservacion: "Temperatura",
  luzConservacion: "Luz",
  conservacionExtintores: "Extintores",
  conservacionMontaje: "Montaje",
  conservacionSistemaElectrico: "Sistema eléctrico",
  conservacionAlarmas: "Alarmas",
  conservacionCamaras: "Cámaras",
  categoriaIds: "Categorías",
  ubicacionId: "Ubicación inicial",
  depositanteId: "Depositante",
  caracterRecepcion: "Carácter de recepción",
  fechaVencimiento: "Fecha de vencimiento"
};

function validationMessages(errors: FieldErrors<ObjetoMuseoFormValues>) {
  return Object.entries(errors).flatMap(([field, error]) => {
    const label = objetoMuseoFieldLabels[field as keyof ObjetoMuseoFormValues] ?? field;
    const message = typeof error?.message === "string" ? error.message : "Debe completar este campo";
    return [`${label}: ${message}`];
  });
}

function tipoDepositanteLabel(depositante: DepositanteResponseDTO) {
  return depositante.tipo === "PERSONA" ? "Persona" : "Institucion";
}

export function ObjetoMuseoForm({
  allowFileUploads = false,
  initialValue,
  footerContent,
  isSubmitting = false,
  onSubmit,
  resetSignal = 0,
  submitError,
  submitLabel
}: ObjetoMuseoFormProps) {
  const {
    data: categorias = [],
    isError: isCategoriasError,
    isLoading: isCategoriasLoading
  } = useCategoriasQuery();
  const detallesConservacionQuery = useDetallesConservacionQuery();
  const ubicacionesQuery = useUbicacionesQuery();
  const categoriasOrdenadas = [...categorias].sort((a, b) => a.nombre.localeCompare(b.nombre));
  const detallesConservacion = detallesConservacionQuery.data ?? [];
  const [categoriaBusqueda, setCategoriaBusqueda] = useState("");
  const [fotos, setFotos] = useState<File[]>([]);
  const [fotoVisibilidades, setFotoVisibilidades] = useState<VisibilidadCampo[]>([]);
  const [reciboEscaneado, setReciboEscaneado] = useState<File | null>(null);
  const [fileErrors, setFileErrors] = useState<string[]>([]);
  const [identificacion, setIdentificacion] = useState("");
  const [nombreDepositante, setNombreDepositante] = useState("");
  const [nombreDepositanteDebounced, setNombreDepositanteDebounced] = useState("");
  const [validationSummary, setValidationSummary] = useState<string[] | null>(null);
  const lastResetSignalRef = useRef(0);
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
    reset,
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
      alto: initialValue?.alto ?? "",
      ancho: initialValue?.ancho ?? "",
      diametro: initialValue?.diametro ?? "",
      espesor: initialValue?.espesor ?? "",
      peso: initialValue?.peso ?? "",
      inscripciones: initialValue?.inscripciones ?? "",
      regimenPropiedad: initialValue?.regimenPropiedad ?? "",
      condicionLegalBien: initialValue?.condicionLegalBien ?? "",
      estadoConservacion: initialValue?.estadoConservacion ?? "",
      detallesEstadoConservacion: initialValue?.detallesEstadoConservacion ?? [],
      intervencionesInadecuadas: initialValue?.intervencionesInadecuadas ?? "",
      estadoIntegridad: initialValue?.estadoIntegridad ?? "",
      humedadConservacion: initialValue?.humedadConservacion ?? "",
      temperaturaConservacion: initialValue?.temperaturaConservacion ?? "",
      luzConservacion: initialValue?.luzConservacion ?? "",
      conservacionExtintores: booleanFormValue(initialValue?.conservacionExtintores),
      conservacionMontaje: booleanFormValue(initialValue?.conservacionMontaje),
      conservacionSistemaElectrico: booleanFormValue(initialValue?.conservacionSistemaElectrico),
      conservacionAlarmas: booleanFormValue(initialValue?.conservacionAlarmas),
      conservacionCamaras: booleanFormValue(initialValue?.conservacionCamaras),
      visibilidades: visibilidadesDefault(initialValue),
      categoriaIds: initialValue?.categorias?.map((categoria) => categoria.id) ?? [],
      ubicacionId: initialValue?.ubicacionId ?? 0,
      depositanteId: initialValue?.depositanteId ?? 0,
      caracterRecepcion: initialValue?.caracterRecepcion === "RECEPCION" ? "" : initialValue?.caracterRecepcion ?? "",
      fechaVencimiento: initialValue?.fechaVencimiento ?? ""
    }
  });
  const watchedCategoriaIds = useWatch({ control, name: "categoriaIds", defaultValue: [] });
  const watchedDetallesConservacion = useWatch({ control, name: "detallesEstadoConservacion", defaultValue: [] });
  const caracterRecepcion = useWatch({ control, name: "caracterRecepcion", defaultValue: initialValue?.caracterRecepcion === "RECEPCION" ? "" : initialValue?.caracterRecepcion ?? "" });
  const categoriaIds = useMemo(() => watchedCategoriaIds ?? [], [watchedCategoriaIds]);
  const detallesSeleccionados = useMemo(() => watchedDetallesConservacion ?? [], [watchedDetallesConservacion]);
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
    if (resetSignal === 0 || initialValue || lastResetSignalRef.current === resetSignal) {
      return;
    }

    lastResetSignalRef.current = resetSignal;
    reset({
      numeroInventario: "",
      denominacionObjeto: "",
      descripcion: "",
      descripcionTecnica: "",
      materiales: "",
      alto: "",
      ancho: "",
      diametro: "",
      espesor: "",
      peso: "",
      inscripciones: "",
      regimenPropiedad: "",
      condicionLegalBien: "",
      estadoConservacion: "",
      detallesEstadoConservacion: [],
      intervencionesInadecuadas: "",
      estadoIntegridad: "",
      humedadConservacion: "",
      temperaturaConservacion: "",
      luzConservacion: "",
      conservacionExtintores: "",
      conservacionMontaje: "",
      conservacionSistemaElectrico: "",
      conservacionAlarmas: "",
      conservacionCamaras: "",
      visibilidades: visibilidadesDefault(),
      categoriaIds: [],
      ubicacionId: 0,
      depositanteId: 0,
      caracterRecepcion: "",
      fechaVencimiento: ""
    });
    setCategoriaBusqueda("");
    setFotos([]);
    setFotoVisibilidades([]);
    setReciboEscaneado(null);
    setFileErrors([]);
    setIdentificacion("");
    setNombreDepositante("");
    setNombreDepositanteDebounced("");
    setDepositanteSeleccionado(null);
    buscarDepositanteMutation.reset();
  }, [buscarDepositanteMutation, initialValue, reset, resetSignal]);

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
        field === "alto" ||
        field === "ancho" ||
        field === "diametro" ||
        field === "espesor" ||
        field === "peso" ||
        field === "inscripciones" ||
        field === "regimenPropiedad" ||
        field === "condicionLegalBien" ||
        field === "estadoConservacion" ||
        field === "detallesEstadoConservacion" ||
        field === "intervencionesInadecuadas" ||
        field === "estadoIntegridad" ||
        field === "humedadConservacion" ||
        field === "temperaturaConservacion" ||
        field === "luzConservacion" ||
        field === "conservacionExtintores" ||
        field === "conservacionMontaje" ||
        field === "conservacionSistemaElectrico" ||
        field === "conservacionAlarmas" ||
        field === "conservacionCamaras" ||
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
  const depositanteNoEncontrado = buscarDepositanteMutation.error instanceof ApiClientError && buscarDepositanteMutation.error.status === 404;
  const fechaMinimaVencimiento = initialValue?.fechaIngreso ?? new Date().toISOString().slice(0, 10);

  function limpiarDepositante() {
    setDepositanteSeleccionado(null);
    setValue("depositanteId", 0, { shouldDirty: true, shouldValidate: true });
  }

  function visibilidadControl(campo: string) {
    return (
      <select
        aria-label={`Visibilidad de ${campo}`}
        className="h-8 rounded-md border bg-background px-2 text-xs outline-none focus:ring-2 focus:ring-ring"
        {...register(`visibilidades.${campo}`)}
      >
        <option value="PUBLICO">Público</option>
        <option value="PRIVADO">Privado</option>
      </select>
    );
  }

  function toggleDetalleConservacion(detalle: string) {
    const seleccionado = detallesSeleccionados.includes(detalle);
    setValue(
      "detallesEstadoConservacion",
      seleccionado ? detallesSeleccionados.filter((item) => item !== detalle) : [...detallesSeleccionados, detalle],
      { shouldDirty: true, shouldValidate: true }
    );
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
    setFotoVisibilidades((current) => [...current, ...validas.map(() => "PUBLICO" as VisibilidadCampo)]);
  }

  function cambiarVisibilidadFoto(index: number, visibilidad: VisibilidadCampo) {
    setFotoVisibilidades((current) => current.map((item, itemIndex) => itemIndex === index ? visibilidad : item));
  }

  function quitarFoto(index: number) {
    setFotos((current) => current.filter((_, itemIndex) => itemIndex !== index));
    setFotoVisibilidades((current) => current.filter((_, itemIndex) => itemIndex !== index));
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
      className="w-full space-y-5"
      onSubmit={handleSubmit(
        (values) => {
          setValidationSummary(null);
          onSubmit({
            numeroInventario: values.numeroInventario.trim(),
            denominacionObjeto: values.denominacionObjeto.trim(),
            descripcion: values.descripcion?.trim() || null,
            descripcionTecnica: values.descripcionTecnica?.trim() || null,
            materiales: values.materiales?.trim() || null,
            alto: values.alto?.trim() || null,
            ancho: values.ancho?.trim() || null,
            diametro: values.diametro?.trim() || null,
            espesor: values.espesor?.trim() || null,
            peso: values.peso?.trim() || null,
            inscripciones: values.inscripciones?.trim() || null,
            regimenPropiedad: values.regimenPropiedad || null,
            condicionLegalBien: values.condicionLegalBien?.trim() || null,
            estadoConservacion: values.estadoConservacion || null,
            detallesEstadoConservacion: values.detallesEstadoConservacion ?? [],
            intervencionesInadecuadas: values.intervencionesInadecuadas || null,
            estadoIntegridad: values.estadoIntegridad || null,
            humedadConservacion: values.humedadConservacion || null,
            temperaturaConservacion: values.temperaturaConservacion?.trim() || null,
            luzConservacion: values.luzConservacion?.trim() || null,
            conservacionExtintores: booleanPayload(values.conservacionExtintores),
            conservacionMontaje: booleanPayload(values.conservacionMontaje),
            conservacionSistemaElectrico: booleanPayload(values.conservacionSistemaElectrico),
            conservacionAlarmas: booleanPayload(values.conservacionAlarmas),
            conservacionCamaras: booleanPayload(values.conservacionCamaras),
            visibilidades: values.visibilidades,
            categoriaIds: values.categoriaIds ?? [],
            ubicacionId: values.ubicacionId && values.ubicacionId > 0 ? Number(values.ubicacionId) : null,
            depositanteId: values.depositanteId,
            caracterRecepcion: values.caracterRecepcion || null,
            fechaVencimiento: caracteresConVencimiento.has(values.caracterRecepcion) ? values.fechaVencimiento || null : null
          }, { fotos, fotoVisibilidades, reciboEscaneado });
        },
        (invalidErrors) => setValidationSummary(validationMessages(invalidErrors))
      )}
    >
      {validationSummary ? (
        <div aria-labelledby="objeto-validation-title" aria-modal="true" className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" role="dialog">
          <div className="w-full max-w-lg rounded-lg border bg-background p-5 shadow-xl">
            <h2 className="text-lg font-semibold text-destructive" id="objeto-validation-title">Faltan datos para completar la carga</h2>
            <p className="mt-2 text-sm text-muted-foreground">Revisá los siguientes campos antes de guardar:</p>
            <ul className="mt-4 list-disc space-y-2 pl-5 text-sm text-destructive">
              {validationSummary.map((message) => <li key={message}>{message}</li>)}
            </ul>
            <div className="mt-5 flex justify-end">
              <button className="rounded-md border px-4 py-2 text-sm font-medium hover:bg-muted" onClick={() => setValidationSummary(null)} type="button">
                Entendido
              </button>
            </div>
          </div>
        </div>
      ) : null}
      {initialValue?.origenCarga === "RAPIDA" && initialValue.datosCompletos === false ? (
        <div className="rounded-md border border-amber-300 bg-amber-50 p-4 text-sm text-amber-950">
          <p className="font-medium">Ficha pendiente de completar</p>
          <p className="mt-1">
            Para cerrar la carga se requiere descripcion tecnica, materiales, al menos una dimension, estado de conservacion y al menos una categoria.
          </p>
        </div>
      ) : null}
      <input type="hidden" {...register("depositanteId", { valueAsNumber: true })} />

      <section className="space-y-4 rounded-md border bg-white p-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-base font-semibold">Depositante</h2>
          {visibilidadControl("depositante")}
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
        {depositanteNoEncontrado ? (
          <div className="rounded-md border border-destructive/30 bg-destructive/5 p-4 text-sm">
            <p className="text-destructive">No se encontró un depositante con ese DNI/CUIT.</p>
            <Link
              className="mt-3 inline-flex h-10 items-center justify-center rounded-md border bg-background px-4 text-sm font-medium hover:bg-muted"
              href={`/depositantes/nuevo?identificacion=${encodeURIComponent(identificacion.trim())}`}
            >
              Dar de alta depositante
            </Link>
          </div>
        ) : null}
        {buscarDepositanteMutation.isError && !depositanteNoEncontrado ? <p className="text-sm text-destructive">No se pudo buscar el depositante.</p> : null}
        {errors.depositanteId ? <p className="text-sm text-destructive">{errors.depositanteId.message}</p> : null}
        <div className="grid gap-4 sm:grid-cols-2">
          {!initialValue ? (
            <div className="space-y-2">
              <div className="flex items-center justify-between gap-2">
                <label className="text-sm font-medium" htmlFor="ubicacionId">Ubicacion inicial</label>
                {visibilidadControl("ubicacion")}
              </div>
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
            <div className="flex items-center justify-between gap-2">
              <label className="text-sm font-medium" htmlFor="caracterRecepcion">Carácter de recepción</label>
              {visibilidadControl("caracterRecepcion")}
            </div>
            <select className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id="caracterRecepcion" {...register("caracterRecepcion")}>
              <option value="">Seleccionar carácter</option>
              {caracteresRecepcion.map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
            {errors.caracterRecepcion ? <p className="text-sm text-destructive">{errors.caracterRecepcion.message}</p> : null}
          </div>
          {mostrarFechaVencimiento ? (
            <div className="space-y-2">
              <div className="flex items-center justify-between gap-2">
                <label className="text-sm font-medium" htmlFor="fechaVencimiento">Fecha de vencimiento</label>
                {visibilidadControl("fechaVencimiento")}
              </div>
              <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id="fechaVencimiento" type="date" min={fechaMinimaVencimiento} {...register("fechaVencimiento")} />
              {errors.fechaVencimiento ? <p className="text-sm text-destructive">{errors.fechaVencimiento.message}</p> : null}
            </div>
          ) : null}
        </div>
      </section>

      <section className="space-y-3 rounded-md border bg-white p-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <h2 className="text-base font-semibold">Categorías</h2>
            {visibilidadControl("categorias")}
          </div>
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

      <section className="space-y-4 rounded-md border bg-white p-4">
        <h2 className="text-base font-semibold">Identificación del objeto</h2>
        <div className="grid gap-5 sm:grid-cols-2">
          <div className="space-y-2">
            <div className="flex items-center justify-between gap-2">
              <label className="text-sm font-medium" htmlFor="numeroInventario">Numero de inventario</label>
              {visibilidadControl("numeroInventario")}
            </div>
            <input
              className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
              id="numeroInventario"
              {...register("numeroInventario")}
            />
            {errors.numeroInventario ? <p className="text-sm text-destructive">{errors.numeroInventario.message}</p> : null}
          </div>
          <div className="space-y-2">
            <div className="flex items-center justify-between gap-2">
              <label className="text-sm font-medium" htmlFor="denominacionObjeto">Denominacion</label>
              {visibilidadControl("denominacionObjeto")}
            </div>
            <input
              className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring"
              id="denominacionObjeto"
              {...register("denominacionObjeto")}
            />
            {errors.denominacionObjeto ? <p className="text-sm text-destructive">{errors.denominacionObjeto.message}</p> : null}
          </div>
        </div>
        <div className="space-y-2">
          <div className="flex items-center justify-between gap-2">
            <label className="text-sm font-medium" htmlFor="descripcion">Descripcion breve</label>
            {visibilidadControl("descripcion")}
          </div>
          <textarea
            className="min-h-32 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
            id="descripcion"
            {...register("descripcion")}
          />
          {errors.descripcion ? <p className="text-sm text-destructive">{errors.descripcion.message}</p> : null}
        </div>
        <div className="space-y-2">
          <div className="flex items-center justify-between gap-2">
            <label className="text-sm font-medium" htmlFor="descripcionTecnica">Descripcion tecnica</label>
            {visibilidadControl("descripcionTecnica")}
          </div>
          <textarea
            className="min-h-32 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
            id="descripcionTecnica"
            {...register("descripcionTecnica")}
          />
        </div>
      </section>

      <section className="space-y-4 rounded-md border bg-white p-4">
        <h2 className="text-base font-semibold">Información técnica</h2>
        <div className="grid gap-5 sm:grid-cols-2">
          <div className="space-y-2 sm:col-span-2">
            <div className="flex items-center justify-between gap-2">
              <label className="text-sm font-medium" htmlFor="materiales">Materiales</label>
              {visibilidadControl("materiales")}
            </div>
            <textarea className="min-h-24 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" id="materiales" {...register("materiales")} />
          </div>
          {[
            ["alto", "Alto"],
            ["ancho", "Ancho"],
            ["diametro", "Diámetro"],
            ["espesor", "Espesor"],
            ["peso", "Peso"]
          ].map(([field, label]) => (
            <div className="space-y-2" key={field}>
              <div className="flex items-center justify-between gap-2">
                <label className="text-sm font-medium" htmlFor={field}>{label}</label>
                {visibilidadControl(field)}
              </div>
              <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id={field} {...register(field as keyof ObjetoMuseoFormValues)} />
            </div>
          ))}
          <div className="space-y-2 sm:col-span-2">
            <div className="flex items-center justify-between gap-2">
              <label className="text-sm font-medium" htmlFor="inscripciones">Inscripciones</label>
              {visibilidadControl("inscripciones")}
            </div>
            <textarea className="min-h-24 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" id="inscripciones" {...register("inscripciones")} />
            {errors.inscripciones ? <p className="text-sm text-destructive">{errors.inscripciones.message}</p> : null}
          </div>
        </div>
      </section>

      <section className="space-y-4 rounded-md border bg-white p-4">
        <h2 className="text-base font-semibold">Régimen jurídico</h2>
        <div className="grid gap-5 sm:grid-cols-2">
          <div className="space-y-2">
            <div className="flex items-center justify-between gap-2">
              <label className="text-sm font-medium" htmlFor="regimenPropiedad">Régimen de propiedad</label>
              {visibilidadControl("regimenPropiedad")}
            </div>
            <select className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id="regimenPropiedad" {...register("regimenPropiedad")}>
              <option value="">Sin especificar</option>
              <option value="PUBLICO">Público</option>
              <option value="PRIVADO">Privado</option>
            </select>
          </div>
          <div className="space-y-2 sm:col-span-2">
            <div className="flex items-center justify-between gap-2">
              <label className="text-sm font-medium" htmlFor="condicionLegalBien">Condición legal del bien</label>
              {visibilidadControl("condicionLegalBien")}
            </div>
            <textarea className="min-h-24 w-full rounded-md border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring" id="condicionLegalBien" {...register("condicionLegalBien")} />
          </div>
        </div>
      </section>

      <section className="space-y-4 rounded-md border bg-white p-4">
        <h2 className="text-base font-semibold">Estado de conservación</h2>
        <div className="grid gap-5 sm:grid-cols-2">
          <div className="space-y-2">
            <div className="flex items-center justify-between gap-2">
              <label className="text-sm font-medium" htmlFor="estadoConservacion">Estado de conservacion</label>
              {visibilidadControl("estadoConservacion")}
            </div>
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
          <div className="space-y-2">
            <div className="flex items-center justify-between gap-2">
              <label className="text-sm font-medium" htmlFor="intervencionesInadecuadas">Intervenciones inadecuadas</label>
              {visibilidadControl("intervencionesInadecuadas")}
            </div>
            <select className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id="intervencionesInadecuadas" {...register("intervencionesInadecuadas")}>
              <option value="">Sin especificar</option>
              <option value="SI">Sí</option>
              <option value="NO">No</option>
              <option value="ELEMENTOS_EXTRANOS">Elementos extraños</option>
            </select>
          </div>
          <div className="space-y-2">
            <div className="flex items-center justify-between gap-2">
              <label className="text-sm font-medium" htmlFor="estadoIntegridad">Estado de integridad</label>
              {visibilidadControl("estadoIntegridad")}
            </div>
            <select className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id="estadoIntegridad" {...register("estadoIntegridad")}>
              <option value="">Sin especificar</option>
              <option value="COMPLETO">Completo</option>
              <option value="INCOMPLETO">Incompleto</option>
              <option value="FRAGMENTADO">Fragmentado</option>
            </select>
          </div>
        </div>
        <div className="space-y-2">
          <div className="flex items-center justify-between gap-2">
            <span className="text-sm font-medium">Detalles del estado de conservación</span>
            {visibilidadControl("detallesEstadoConservacion")}
          </div>
          <div className="max-h-52 overflow-y-auto rounded-md border bg-background p-2">
            <div className="grid gap-1 sm:grid-cols-2 lg:grid-cols-3">
              {detallesConservacion.map((detalle) => (
                <label className="flex min-h-9 items-center gap-2 rounded-md px-2 py-1.5 text-sm hover:bg-muted" key={detalle.codigo}>
                  <input checked={detallesSeleccionados.includes(detalle.codigo)} className="h-4 w-4 accent-primary" disabled={isSubmitting} onChange={() => toggleDetalleConservacion(detalle.codigo)} type="checkbox" />
                  <span className="min-w-0 flex-1 truncate">{detalle.nombre}</span>
                </label>
              ))}
              {detallesConservacionQuery.isLoading ? <p className="px-2 py-2 text-sm text-muted-foreground">Cargando detalles...</p> : null}
              {!detallesConservacionQuery.isLoading && detallesConservacion.length === 0 ? <p className="px-2 py-2 text-sm text-muted-foreground">Sin detalles disponibles.</p> : null}
            </div>
          {detallesConservacionQuery.isError ? <p className="text-sm text-destructive">No se pudieron cargar los detalles de conservación.</p> : null}
          </div>
        </div>
      </section>

      <section className="space-y-4 rounded-md border bg-white p-4">
        <h2 className="text-base font-semibold">Conservación preventiva</h2>
        <div className="grid gap-5 sm:grid-cols-3">
          <div className="space-y-2">
            <div className="flex items-center justify-between gap-2">
              <label className="text-sm font-medium" htmlFor="humedadConservacion">Humedad</label>
              {visibilidadControl("humedadConservacion")}
            </div>
            <select className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id="humedadConservacion" {...register("humedadConservacion")}>
              <option value="">Sin especificar</option>
              <option value="ALTA">Alta</option>
              <option value="BAJA">Baja</option>
            </select>
          </div>
          <div className="space-y-2">
            <div className="flex items-center justify-between gap-2">
              <label className="text-sm font-medium" htmlFor="temperaturaConservacion">Temperatura</label>
              {visibilidadControl("temperaturaConservacion")}
            </div>
            <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id="temperaturaConservacion" {...register("temperaturaConservacion")} />
          </div>
          <div className="space-y-2">
            <div className="flex items-center justify-between gap-2">
              <label className="text-sm font-medium" htmlFor="luzConservacion">Luz</label>
              {visibilidadControl("luzConservacion")}
            </div>
            <input className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id="luzConservacion" {...register("luzConservacion")} />
          </div>
        </div>
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-5">
          {[
            ["conservacionExtintores", "Extintores"],
            ["conservacionMontaje", "Montaje"],
            ["conservacionSistemaElectrico", "Sistema eléctrico"],
            ["conservacionAlarmas", "Alarmas"],
            ["conservacionCamaras", "Cámaras"]
          ].map(([field, label]) => (
            <div className="space-y-2" key={field}>
              <div className="flex items-center justify-between gap-2">
                <label className="text-sm font-medium" htmlFor={field}>{label}</label>
                {visibilidadControl(field)}
              </div>
              <select className="h-10 w-full rounded-md border bg-background px-3 text-sm outline-none focus:ring-2 focus:ring-ring" id={field} {...register(field as keyof ObjetoMuseoFormValues)}>
                <option value="">Sin especificar</option>
                <option value="true">Sí</option>
                <option value="false">No</option>
              </select>
            </div>
          ))}
        </div>
      </section>

      {allowFileUploads ? (
        <>
          <section className="space-y-3 rounded-md border bg-white p-4">
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
                    <div className="space-y-2 p-2 text-sm">
                      <span className="block min-w-0 truncate">{preview.file.name}</span>
                      <div className="flex items-center justify-between gap-2">
                        <select
                          aria-label={`Visibilidad de ${preview.file.name}`}
                          className="h-8 rounded-md border bg-background px-2 text-xs outline-none focus:ring-2 focus:ring-ring"
                          disabled={isSubmitting}
                          onChange={(event) => cambiarVisibilidadFoto(index, event.target.value as VisibilidadCampo)}
                          value={fotoVisibilidades[index] ?? "PUBLICO"}
                        >
                          <option value="PUBLICO">Público</option>
                          <option value="PRIVADO">Privado</option>
                        </select>
                        <button
                          className="rounded-md border px-2 py-1 text-xs text-destructive hover:bg-destructive/10"
                          disabled={isSubmitting}
                          onClick={() => quitarFoto(index)}
                          type="button"
                        >
                          Quitar
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : null}
          </section>
          <section className="space-y-3 rounded-md border bg-white p-4">
            <div>
              <h2 className="text-base font-semibold">Recibo</h2>
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
      {footerContent}
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
