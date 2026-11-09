import { motion, type HTMLMotionProps } from 'framer-motion'
import { cn } from '@/lib/cn'

interface Props extends HTMLMotionProps<'div'> {
  strong?: boolean
  hover?: boolean
}

export function GlassCard({ strong, hover = true, className, children, ...rest }: Props) {
  return (
    <motion.div
      whileHover={hover ? { y: -4 } : undefined}
      transition={{ type: 'spring', stiffness: 300, damping: 22 }}
      className={cn(strong ? 'glass-strong' : 'glass', className)}
      {...rest}
    >
      {children}
    </motion.div>
  )
}
