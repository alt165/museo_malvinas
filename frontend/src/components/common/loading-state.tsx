type LoadingStateProps = {
  label?: string;
};

export function LoadingState({ label = "Cargando..." }: LoadingStateProps) {
  return (
    <div className="flex min-h-40 items-center justify-center rounded-lg border border-secondary/35 bg-secondary/10 text-sm font-medium text-primary">
      {label}
    </div>
  );
}
