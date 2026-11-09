import { useEffect, useId, useRef, useState } from 'react'
import { Html5Qrcode } from 'html5-qrcode'
import { Camera, CameraOff } from 'lucide-react'
import { Button } from '@/components/ui/Button'

/** Opens the rear camera and reports each distinct QR it reads. Cameras need HTTPS or localhost. */
export default function CameraScanner({ onScan }: { onScan: (text: string) => void }) {
  const id = useId().replace(/:/g, '')
  const [on, setOn] = useState(false)
  const [error, setError] = useState<string>()
  const last = useRef({ text: '', at: 0 })
  const cb = useRef(onScan)
  cb.current = onScan

  useEffect(() => {
    if (!on) return
    const scanner = new Html5Qrcode(id)
    scanner
      .start({ facingMode: 'environment' }, { fps: 10, qrbox: 220 }, (text) => {
        const now = Date.now()
        if (text === last.current.text && now - last.current.at < 4000) return   // same pass held in view
        last.current = { text, at: now }
        cb.current(text)
      }, () => undefined)
      .catch(() => { setError("Couldn't open the camera. Allow camera access, or use the box below."); setOn(false) })
    return () => { scanner.stop().then(() => scanner.clear()).catch(() => undefined) }
  }, [on, id])

  return (
    <div className="space-y-3">
      <div id={id} className={on ? 'overflow-hidden rounded-2xl' : 'hidden'} />
      {error && <p role="alert" className="text-sm text-danger">{error}</p>}
      <Button variant={on ? 'ghost' : 'primary'} onClick={() => { setError(undefined); setOn(!on) }}>{on ? <><CameraOff size={16} /> Stop camera</> : <><Camera size={16} /> Scan with camera</>}</Button>
    </div>
  )
}
