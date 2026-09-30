const ARGENTINA_TIME_ZONE = "America/Argentina/Buenos_Aires";

/**
 * Backend LocalDateTime strings have no offset and represent stored civil time.
 * Keep those clock fields unchanged; offset-bearing values are real instants.
 */
export function formatDateTimeArgentina(value?: string | null, fallback = "Sin dato") {
  if (!value) return fallback;

  const tieneOffset = /(?:Z|[+-]\d{2}:\d{2})$/i.test(value);
  const date = new Date(tieneOffset ? value : `${value}Z`);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat("es-AR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
    timeZone: tieneOffset ? ARGENTINA_TIME_ZONE : "UTC"
  }).format(date).replace(", ", " ");
}

export function todayInArgentina() {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: ARGENTINA_TIME_ZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit"
  }).formatToParts(new Date());
  const values = Object.fromEntries(parts.map(({ type, value }) => [type, value]));
  return `${values.year}-${values.month}-${values.day}`;
}

export function currentTimestampInArgentinaForFilename() {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: ARGENTINA_TIME_ZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23"
  }).formatToParts(new Date());
  const values = Object.fromEntries(parts.map(({ type, value }) => [type, value]));
  return `${values.year}-${values.month}-${values.day}_${values.hour}-${values.minute}`;
}
