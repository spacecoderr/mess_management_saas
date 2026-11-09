import { Link } from 'react-router-dom'

export function Logo({ to = '/', className = '' }: { to?: string; className?: string }) {
  return (
    <Link to={to} className={`flex items-center gap-2 font-display text-xl font-bold text-heading ${className}`} aria-label="Thali home">
      <span className="grid h-8 w-8 place-items-center rounded-full bg-primary">
        <span className="h-3 w-3 rounded-full bg-accent" />
      </span>
      Thali
    </Link>
  )
}
