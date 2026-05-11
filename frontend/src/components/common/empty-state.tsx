type EmptyStateProps = {
  title: string;
  description?: string;
  action?: React.ReactNode;
};

export function EmptyState({ action, description, title }: EmptyStateProps) {
  return (
    <div className="rounded-lg border border-dashed border-primary/25 bg-surface p-8 text-center shadow-sm">
      <h2 className="text-base font-semibold text-primary">{title}</h2>
      {description ? (
        <p className="mx-auto mt-2 max-w-xl text-sm text-muted-foreground">{description}</p>
      ) : null}
      {action ? <div className="mt-5 flex justify-center">{action}</div> : null}
    </div>
  );
}
