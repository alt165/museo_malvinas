type LoadingStateProps = {
  label?: string;
};

export function LoadingState({ label = "Cargando..." }: LoadingStateProps) {
  return (
    <div className="flex min-h-40 items-center justify-center rounded-lg border text-sm text-muted-foreground">
      {label}
    </div>
  );
}
