export function queryNumber(value: unknown): number {
  return typeof value === 'string' && /^[1-9]\d*$/.test(value) ? Number(value) : NaN;
}
