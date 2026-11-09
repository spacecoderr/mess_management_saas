export function MeshBackground() {
  return (
    <div className="mesh-bg" aria-hidden>
      <div className="blob animate-float -left-40 -top-40 h-[38rem] w-[38rem]" style={{ background: 'var(--blob-1)' }} />
      <div className="blob animate-float right-[-8rem] top-1/4 h-[30rem] w-[30rem]" style={{ background: 'var(--blob-2)', animationDelay: '-6s' }} />
      <div className="blob animate-float bottom-[-14rem] left-1/3 h-[34rem] w-[34rem]" style={{ background: 'var(--blob-3)', animationDelay: '-12s' }} />
    </div>
  )
}
