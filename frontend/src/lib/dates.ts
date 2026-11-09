/** Local calendar date as YYYY-MM-DD (not UTC, so "today" is right late at night). */
export const isoDate = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
export const addDays = (d: Date, n: number) => { const c = new Date(d); c.setDate(c.getDate() + n); return c }
export const parseDate = (iso: string) => new Date(`${iso}T00:00:00`)
export const dayLabel = (iso: string) => parseDate(iso).toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short' })
