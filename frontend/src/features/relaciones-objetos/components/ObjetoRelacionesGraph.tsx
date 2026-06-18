"use client";

import { Archive, ImageIcon, Maximize2, Network, Table2 } from "lucide-react";
import { useRouter } from "next/navigation";
import { memo, useMemo, useState } from "react";
import ReactFlow, {
  Background,
  Controls,
  Handle,
  MarkerType,
  MiniMap,
  Position,
  type Edge,
  type Node,
  type NodeProps,
  type ReactFlowInstance
} from "reactflow";
import { EmptyState } from "@/components/common/empty-state";
import { ErrorState } from "@/components/common/error-state";
import { LoadingState } from "@/components/common/loading-state";
import type { ObjetoMuseoResponseDTO } from "@/features/objetos/types";
import { ApiClientError } from "@/lib/errors/api-error";
import { useGrafoRelacionesObjetoQuery } from "../queries";
import type { AristaGrafoObjetoDTO, NodoGrafoObjetoDTO, ObjetoGrafoResponseDTO } from "../types";
import { getApiErrorMessage } from "../utils";

type ObjetoRelacionesGraphProps = {
  objetoId: number;
  objeto?: ObjetoMuseoResponseDTO;
  profundidad: number;
  onBackToTable?: () => void;
  onProfundidadChange: (value: number) => void;
};

type ObjectNodeData = {
  isCentral: boolean;
  label: string;
  numeroInventario: string;
};

const relationPalette = ["#163A61", "#2F7FA2", "#DBB060", "#5E8C61", "#8A5F32", "#7E6AAE"];

export function ObjetoRelacionesGraph({
  objeto,
  objetoId,
  onBackToTable,
  onProfundidadChange,
  profundidad
}: ObjetoRelacionesGraphProps) {
  const router = useRouter();
  const graphQuery = useGrafoRelacionesObjetoQuery(objetoId, profundidad);
  const [flowInstance, setFlowInstance] = useState<ReactFlowInstance | null>(null);

  const relationColors = useMemo(() => getRelationColors(graphQuery.data?.edges ?? []), [graphQuery.data?.edges]);
  const flowNodes = useMemo(() => (graphQuery.data ? toFlowNodes(graphQuery.data, objetoId) : []), [graphQuery.data, objetoId]);
  const flowEdges = useMemo(() => (graphQuery.data ? toFlowEdges(graphQuery.data, relationColors) : []), [graphQuery.data, relationColors]);

  const centralNode = graphQuery.data?.nodes.find((node) => node.id === objetoId);
  const objetoPrincipal = {
    nombre: objeto?.denominacionObjeto ?? centralNode?.label ?? "Objeto consultado",
    numeroInventario: objeto?.numeroInventario ?? centralNode?.numeroInventario ?? String(objetoId),
    ubicacion: objeto?.ubicacionNombre,
    coleccion: objeto?.coleccionNombre
  };

  return (
    <section className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-lg border bg-surface p-4 shadow-sm">
        <div>
          <h2 className="text-base font-semibold text-[#163A61]">Grafo de relaciones</h2>
          <p className="text-sm text-muted-foreground">Lectura radial con profundidad maxima 3.</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <div className="inline-flex rounded-md border bg-white p-1" aria-label="Seleccionar profundidad">
            {[1, 2, 3].map((value) => (
              <button
                className={
                  value === profundidad
                    ? "rounded bg-[#163A61] px-3 py-1.5 text-sm font-medium text-white"
                    : "rounded px-3 py-1.5 text-sm font-medium hover:bg-muted"
                }
                key={value}
                onClick={() => onProfundidadChange(value)}
                type="button"
              >
                {value}
              </button>
            ))}
          </div>
          <button
            className="inline-flex items-center gap-2 rounded-md border bg-white px-3 py-2 text-sm font-medium text-[#163A61] hover:bg-muted"
            onClick={() => flowInstance?.fitView({ padding: 0.2, duration: 400 })}
            type="button"
          >
            <Maximize2 className="h-4 w-4" />
            Ajustar vista
          </button>
          {onBackToTable ? (
            <button
              className="inline-flex items-center gap-2 rounded-md border bg-white px-3 py-2 text-sm font-medium hover:bg-muted"
              onClick={onBackToTable}
              type="button"
            >
              <Table2 className="h-4 w-4" />
              Vista tabla
            </button>
          ) : null}
        </div>
      </div>

      {graphQuery.isLoading ? <LoadingState label="Cargando grafo..." /> : null}
      {graphQuery.isError ? (
        <ErrorState
          message={getApiErrorMessage(graphQuery.error)}
          requestId={graphQuery.error instanceof ApiClientError ? graphQuery.error.requestId : undefined}
        />
      ) : null}
      {graphQuery.data && graphQuery.data.edges.length === 0 ? (
        <EmptyState description="El objeto no tiene relaciones para graficar en esta profundidad." title="Sin relaciones" />
      ) : null}
      {graphQuery.data && graphQuery.data.edges.length > 0 ? (
        <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_280px]">
          <div className="h-[680px] min-h-[520px] overflow-hidden rounded-lg border bg-[#F7FAFC] shadow-sm sm:h-[740px]">
            <ReactFlow
              edges={flowEdges}
              edgeTypes={{}}
              fitView
              fitViewOptions={{ padding: 0.18 }}
              maxZoom={1.7}
              minZoom={0.18}
              nodeTypes={nodeTypes}
              nodes={flowNodes}
              nodesConnectable={false}
              nodesDraggable={false}
              onInit={setFlowInstance}
              onNodeClick={(_, node) => {
                if (node.id !== String(objetoId)) {
                  router.push(`/objetos/${node.id}`);
                }
              }}
              panOnScroll
              proOptions={{ hideAttribution: true }}
            >
              <MiniMap
                nodeBorderRadius={8}
                nodeColor={(node) => (node.data?.isCentral ? "#163A61" : "#DBB060")}
                pannable
                zoomable
              />
              <Controls showInteractive={false} />
              <Background color="#D8E3EC" gap={28} />
            </ReactFlow>
          </div>

          <aside className="space-y-4 rounded-lg border bg-white p-4 shadow-sm">
            <div>
              <p className="text-xs font-semibold uppercase text-muted-foreground">Objeto principal</p>
              <div className="mt-3 rounded-md border border-[#163A61]/20 bg-[#163A61] p-3 text-white">
                <p className="text-xs text-white/75">{objetoPrincipal.numeroInventario}</p>
                <p className="mt-1 text-sm font-semibold leading-snug">{objetoPrincipal.nombre}</p>
              </div>
              {objetoPrincipal.ubicacion || objetoPrincipal.coleccion ? (
                <dl className="mt-3 space-y-2 text-sm">
                  {objetoPrincipal.ubicacion ? (
                    <div>
                      <dt className="text-xs text-muted-foreground">Ubicacion</dt>
                      <dd className="font-medium">{objetoPrincipal.ubicacion}</dd>
                    </div>
                  ) : null}
                  {objetoPrincipal.coleccion ? (
                    <div>
                      <dt className="text-xs text-muted-foreground">Coleccion</dt>
                      <dd className="font-medium">{objetoPrincipal.coleccion}</dd>
                    </div>
                  ) : null}
                </dl>
              ) : null}
            </div>

            <div>
              <p className="text-xs font-semibold uppercase text-muted-foreground">Leyenda</p>
              <div className="mt-3 space-y-2">
                {Object.entries(relationColors).map(([tipo, color]) => (
                  <div className="flex items-center gap-2 text-sm" key={tipo}>
                    <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: color }} />
                    <span className="truncate">{tipo}</span>
                  </div>
                ))}
              </div>
            </div>
          </aside>
        </div>
      ) : null}
    </section>
  );
}

const ObjectNode = memo(function ObjectNode({ data }: NodeProps<ObjectNodeData>) {
  const isCentral = data.isCentral;

  return (
    <div
      className={
        isCentral
          ? "w-[240px] rounded-lg border-2 border-[#81D2F7] bg-[#163A61] p-3 text-white shadow-lg"
          : "w-[220px] rounded-lg border border-[#D6DEE7] bg-white p-3 text-foreground shadow-md"
      }
    >
      <Handle className="opacity-0" position={Position.Top} type="target" />
      <Handle className="opacity-0" position={Position.Right} type="source" />
      <Handle className="opacity-0" position={Position.Bottom} type="source" />
      <Handle className="opacity-0" position={Position.Left} type="target" />
      <div className="flex gap-3">
        <div
          className={
            isCentral
              ? "flex h-14 w-14 shrink-0 items-center justify-center rounded-md bg-white/15 text-white"
              : "flex h-14 w-14 shrink-0 items-center justify-center rounded-md bg-[#EAF7FD] text-[#163A61]"
          }
          aria-hidden="true"
        >
          {isCentral ? <Network className="h-6 w-6" /> : <ImageIcon className="h-6 w-6" />}
        </div>
        <div className="min-w-0">
          <p className={isCentral ? "truncate text-xs text-white/75" : "truncate text-xs text-muted-foreground"}>
            {data.numeroInventario}
          </p>
          <p className="mt-1 line-clamp-2 text-sm font-semibold leading-snug">{data.label}</p>
        </div>
      </div>
      <div className={isCentral ? "mt-3 flex items-center gap-1 text-xs text-white/75" : "mt-3 flex items-center gap-1 text-xs text-muted-foreground"}>
        <Archive className="h-3.5 w-3.5" />
        {isCentral ? "Objeto consultado" : "Objeto relacionado"}
      </div>
    </div>
  );
});

const nodeTypes = { objectNode: ObjectNode };

function toFlowNodes(graph: ObjetoGrafoResponseDTO, objetoId: number): Node<ObjectNodeData>[] {
  const positions = calculateRadialPositions(graph, objetoId);

  return graph.nodes.map((node) => ({
    id: String(node.id),
    data: {
      isCentral: node.id === objetoId,
      label: node.label,
      numeroInventario: node.numeroInventario
    },
    position: positions.get(node.id) ?? { x: 0, y: 0 },
    type: "objectNode"
  }));
}

function calculateRadialPositions(graph: ObjetoGrafoResponseDTO, objetoId: number) {
  const positions = new Map<number, { x: number; y: number }>();
  const metadata = getLayoutMetadata(graph, objetoId);
  const groups = new Map<string, NodoGrafoObjetoDTO[]>();

  positions.set(objetoId, { x: 0, y: 0 });

  graph.nodes
    .filter((node) => node.id !== objetoId)
    .forEach((node) => {
      const item = metadata.get(node.id) ?? { distance: 1, side: "outgoing" as LayoutSide };
      const key = `${item.side}:${Math.min(item.distance, 3)}`;
      groups.set(key, [...(groups.get(key) ?? []), node]);
    });

  Array.from(groups.entries()).forEach(([key, nodes]) => {
    const [side, levelValue] = key.split(":") as [LayoutSide, string];
    const level = Number(levelValue);
    const sign = side === "incoming" ? -1 : 1;
    const horizontalGap = 430;
    const levelGap = 360;
    const verticalGap = 230;
    const x = sign * (horizontalGap + (level - 1) * levelGap);
    const sideOffset = side === "incoming" ? -36 : 36;
    const levelOffset = level % 2 === 0 ? 70 : 0;

    nodes
      .sort((a, b) => a.numeroInventario.localeCompare(b.numeroInventario))
      .forEach((node, index) => {
        const centeredIndex = index - (nodes.length - 1) / 2;
        const alternatingOffset = index % 2 === 0 ? 0 : 34;
        positions.set(node.id, {
          x,
          y: centeredIndex * verticalGap + sideOffset + levelOffset + alternatingOffset
        });
      });
  });

  return positions;
}

type LayoutSide = "incoming" | "outgoing";

type LayoutMetadata = {
  distance: number;
  side: LayoutSide;
};

function getLayoutMetadata(graph: ObjetoGrafoResponseDTO, objetoId: number) {
  const adjacency = new Map<number, number[]>();
  graph.nodes.forEach((node) => adjacency.set(node.id, []));
  graph.edges.forEach((edge) => {
    adjacency.get(edge.source)?.push(edge.target);
    adjacency.get(edge.target)?.push(edge.source);
  });

  const metadata = new Map<number, LayoutMetadata>([[objetoId, { distance: 0, side: "outgoing" }]]);
  const queue: number[] = [];

  graph.edges
    .filter((edge) => edge.source === objetoId || edge.target === objetoId)
    .sort((a, b) => `${a.tipoRelacion}-${a.id}`.localeCompare(`${b.tipoRelacion}-${b.id}`))
    .forEach((edge) => {
      const neighbor = edge.source === objetoId ? edge.target : edge.source;
      const side: LayoutSide = edge.source === objetoId ? "outgoing" : "incoming";
      if (!metadata.has(neighbor)) {
        metadata.set(neighbor, { distance: 1, side });
        queue.push(neighbor);
      }
    });

  while (queue.length > 0) {
    const current = queue.shift();
    if (current === undefined) {
      continue;
    }
    const currentMetadata = metadata.get(current);
    if (!currentMetadata || currentMetadata.distance >= 3) {
      continue;
    }

    adjacency.get(current)?.forEach((neighbor) => {
      if (!metadata.has(neighbor)) {
        metadata.set(neighbor, { distance: currentMetadata.distance + 1, side: currentMetadata.side });
        queue.push(neighbor);
      }
    });
  }

  return metadata;
}

function toFlowEdges(graph: ObjetoGrafoResponseDTO, relationColors: Record<string, string>): Edge[] {
  const parallelIndexes = getParallelEdgeIndexes(graph.edges);

  return graph.edges.map((edge) => {
    const color = relationColors[edge.tipoRelacion] ?? "#163A61";
    const parallel = parallelIndexes.get(edge.id) ?? { index: 0, total: 1 };
    const parallelOffset = (parallel.index - (parallel.total - 1) / 2) * 28;

    return {
      id: String(edge.id),
      source: String(edge.source),
      target: String(edge.target),
      label: edge.tipoRelacion,
      markerEnd: { color, type: MarkerType.ArrowClosed },
      pathOptions: { borderRadius: 34, offset: 48 + Math.abs(parallelOffset) },
      style: { stroke: color, strokeWidth: 2 },
      type: "smoothstep",
      labelStyle: { fill: "#163A61", fontSize: 12, fontWeight: 700 },
      labelBgBorderRadius: 6,
      labelBgPadding: [8, 5],
      labelBgStyle: { fill: "#FFFFFF", fillOpacity: 0.98, stroke: color, strokeWidth: 0.7 }
    };
  });
}

function getParallelEdgeIndexes(edges: AristaGrafoObjetoDTO[]) {
  const groups = new Map<string, AristaGrafoObjetoDTO[]>();
  edges.forEach((edge) => {
    const key = `${edge.source}:${edge.target}`;
    groups.set(key, [...(groups.get(key) ?? []), edge]);
  });

  const indexes = new Map<number, { index: number; total: number }>();
  groups.forEach((group) => {
    group
      .sort((a, b) => a.id - b.id)
      .forEach((edge, index) => indexes.set(edge.id, { index, total: group.length }));
  });

  return indexes;
}

function getRelationColors(edges: AristaGrafoObjetoDTO[]) {
  return Array.from(new Set(edges.map((edge) => edge.tipoRelacion))).reduce<Record<string, string>>((acc, tipo, index) => {
    acc[tipo] = relationPalette[index % relationPalette.length];
    return acc;
  }, {});
}
