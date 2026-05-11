type ErrorStateProps = {
  title?: string;
  message: string;
  requestId?: string;
};

export function ErrorState({ message, requestId, title = "No se pudo cargar la informacion" }: ErrorStateProps) {
  return (
    <div className="rounded-lg border border-destructive/30 bg-surface p-5 shadow-sm">
      <h2 className="text-base font-semibold text-destructive">{title}</h2>
      <p className="mt-2 text-sm text-muted-foreground">{message}</p>
      {requestId ? <p className="mt-3 text-xs text-muted-foreground">Request ID: {requestId}</p> : null}
    </div>
  );
}
