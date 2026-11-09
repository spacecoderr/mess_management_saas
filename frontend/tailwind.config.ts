import type { Config } from 'tailwindcss'

// Semantic colours read CSS variables (see styles/globals.css) so every component themes itself: light "Ivory & Forest",
// dark "Midnight Forest". The raw palettes (forest/sage/terracotta/ivory) stay available for tinted surfaces.
const token = (name: string) => `rgb(var(--${name}) / <alpha-value>)`

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  darkMode: ['selector', '[data-theme="dark"]'],
  theme: {
    extend: {
      colors: {
        ink: token('ink'),
        'ink-soft': token('ink-soft'),
        heading: token('heading'),
        primary: token('primary'),
        'on-primary': token('on-primary'),
        accent: token('accent'),
        'on-accent': token('on-accent'),
        danger: token('danger'),
        success: token('success'),
        ivory: { 50: '#FFFDF8', 100: '#FBF7EE', 200: '#F3EBDA', 300: '#E8DCC2' },
        forest: { 50: '#EEF4F0', 100: '#D5E5DB', 300: '#6FA085', 500: '#2F6B4F', 600: '#265A42', 700: '#1F4D3A', 800: '#173B2C', 900: '#0F2A1F' },
        sage: { 100: '#EDF1E8', 200: '#D9E2D0', 300: '#C2CFB6', 400: '#A8B9A0', 500: '#8FA58A', 600: '#6F8A6B' },
        terracotta: { 100: '#FAE3D8', 300: '#E8A084', 400: '#D9805A', 500: '#C8643B', 600: '#AD512D', 700: '#8A3F22' },
      },
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'system-ui', 'sans-serif'],
        display: ['Fraunces', 'Georgia', 'serif'],
      },
      boxShadow: {
        glass: 'var(--shadow-glass)',
        'glass-lg': 'var(--shadow-glass-lg)',
      },
      keyframes: {
        float: { '0%,100%': { transform: 'translate3d(0,0,0) scale(1)' }, '50%': { transform: 'translate3d(30px,-40px,0) scale(1.08)' } },
        shimmer: { '100%': { transform: 'translateX(100%)' } },
      },
      animation: { float: 'float 18s ease-in-out infinite', shimmer: 'shimmer 1.6s infinite' },
    },
  },
  plugins: [],
} satisfies Config
