import { useState } from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Alert } from './Alert'
import { Button } from './Button'
import { Input, PasswordInput } from './Input'
import { StatTile } from './StatTile'
import { StepIndicator } from './StepIndicator'
import { Tabs } from './Tabs'
import { ThemeToggle } from './ThemeToggle'
import { Users } from 'lucide-react'
import { useThemeStore } from '@/store/themeStore'

describe('Button', () => {
  it('is disabled and announces busy while loading, and does not fire clicks', async () => {
    const onClick = vi.fn()
    render(<Button loading onClick={onClick}>Save</Button>)
    const b = screen.getByRole('button', { name: 'Save' })
    expect(b).toBeDisabled()
    expect(b).toHaveAttribute('aria-busy', 'true')
    await userEvent.click(b)
    expect(onClick).not.toHaveBeenCalled()
  })

  it('defaults to type="button" so it never submits a form by accident', () => {
    render(<Button>Go</Button>)
    expect(screen.getByRole('button')).toHaveAttribute('type', 'button')
  })
})

describe('Input', () => {
  it('links the label, and exposes the error to assistive tech', () => {
    render(<Input label="Email" error="Enter a valid email" />)
    const input = screen.getByLabelText('Email')
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input).toHaveAccessibleDescription('Enter a valid email')
    expect(screen.getByRole('alert')).toHaveTextContent('Enter a valid email')
  })

  it('shows a hint when there is no error', () => {
    render(<Input label="Email" hint="Use your college email" />)
    expect(screen.getByLabelText('Email')).toHaveAccessibleDescription('Use your college email')
  })

  it('password field toggles visibility', async () => {
    render(<PasswordInput label="Password" />)
    const input = screen.getByLabelText('Password')
    expect(input).toHaveAttribute('type', 'password')
    await userEvent.click(screen.getByRole('button', { name: 'Show password' }))
    expect(input).toHaveAttribute('type', 'text')
    await userEvent.click(screen.getByRole('button', { name: 'Hide password' }))
    expect(input).toHaveAttribute('type', 'password')
  })
})

describe('Alert', () => {
  it('errors are announced as alerts, others as polite status', () => {
    const { rerender } = render(<Alert tone="error">Bad</Alert>)
    expect(screen.getByRole('alert')).toHaveTextContent('Bad')
    rerender(<Alert tone="success">Good</Alert>)
    expect(screen.getByRole('status')).toHaveTextContent('Good')
  })
})

describe('Tabs', () => {
  function Demo() {
    const [v, setV] = useState<'a' | 'b'>('a')
    return <Tabs label="Mode" value={v} onChange={setV} options={[{ value: 'a', label: 'Alpha' }, { value: 'b', label: 'Beta' }]} />
  }

  it('marks the active tab and switches on click', async () => {
    render(<Demo />)
    expect(screen.getByRole('tab', { name: 'Alpha' })).toHaveAttribute('aria-selected', 'true')
    await userEvent.click(screen.getByRole('tab', { name: 'Beta' }))
    expect(screen.getByRole('tab', { name: 'Beta' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tab', { name: 'Alpha' })).toHaveAttribute('aria-selected', 'false')
  })

  it('arrow keys cycle through the tabs', async () => {
    render(<Demo />)
    screen.getByRole('tab', { name: 'Alpha' }).focus()
    await userEvent.keyboard('{ArrowRight}')
    expect(screen.getByRole('tab', { name: 'Beta' })).toHaveAttribute('aria-selected', 'true')
    await userEvent.keyboard('{ArrowRight}')
    expect(screen.getByRole('tab', { name: 'Alpha' })).toHaveAttribute('aria-selected', 'true')
  })
})

describe('StepIndicator', () => {
  it('marks the current step and shows finished ones as done', () => {
    render(<StepIndicator steps={['One', 'Two', 'Three']} current={1} />)
    expect(screen.getByText('Two').closest('li')).toHaveAttribute('aria-current', 'step')
    expect(screen.getByLabelText('done')).toBeInTheDocument()
    expect(screen.getByText('3')).toBeInTheDocument()
  })
})

describe('StatTile', () => {
  it('shows the value, a loading skeleton, or the empty text', () => {
    const { rerender } = render(<StatTile label="Students" icon={Users} value={22} />)
    expect(screen.getByText('22')).toBeInTheDocument()
    rerender(<StatTile label="Students" icon={Users} loading />)
    expect(screen.queryByText('22')).not.toBeInTheDocument()
    rerender(<StatTile label="Rating" icon={Users} empty="No ratings yet" />)
    expect(screen.getByText('No ratings yet')).toBeInTheDocument()
  })

  it('applies a custom formatter', () => {
    render(<StatTile label="Revenue" icon={Users} value={47104} format={(n) => `₹${n}`} />)
    expect(screen.getByText('₹47104')).toBeInTheDocument()
  })
})

describe('ThemeToggle', () => {
  it('switches the stored mode and offers the opposite action', async () => {
    useThemeStore.setState({ mode: 'light' })
    render(<ThemeToggle />)
    await userEvent.click(screen.getByRole('button', { name: 'Switch to dark theme' }))
    expect(useThemeStore.getState().mode).toBe('dark')
    expect(JSON.parse(localStorage.getItem('thali-theme')!).state.mode).toBe('dark')
    expect(screen.getByRole('button', { name: 'Switch to light theme' })).toBeInTheDocument()
  })
})
