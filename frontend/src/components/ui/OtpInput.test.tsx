import { useState } from 'react'
import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { OtpInput } from './OtpInput'

function Harness({ onComplete }: { onComplete?: (v: string) => void }) {
  const [value, setValue] = useState('')
  return (
    <>
      <OtpInput value={value} onChange={setValue} onComplete={onComplete} />
      <output data-testid="value">{value}</output>
    </>
  )
}
const box = (n: number) => screen.getByLabelText(`Digit ${n} of 6`) as HTMLInputElement
const value = () => screen.getByTestId('value').textContent

describe('OtpInput', () => {
  it('renders six labelled boxes', () => {
    render(<Harness />)
    expect(screen.getAllByRole('textbox')).toHaveLength(6)
    expect(screen.getByRole('group', { name: 'One-time code' })).toBeInTheDocument()
  })

  it('moves to the next box after each digit and calls onComplete with the full code', async () => {
    const done = vi.fn()
    render(<Harness onComplete={done} />)
    await userEvent.click(box(1))
    await userEvent.keyboard('123456')
    expect(value()).toBe('123456')
    expect([1, 2, 3, 4, 5, 6].map((n) => box(n).value).join('')).toBe('123456')
    expect(done).toHaveBeenCalledTimes(1)
    expect(done).toHaveBeenCalledWith('123456')
  })

  it('ignores letters and symbols', async () => {
    render(<Harness />)
    await userEvent.click(box(1))
    await userEvent.keyboard('a1-b2')
    expect(value()).toBe('12')
  })

  it('backspace clears a digit, and on an empty box goes back and clears the previous one', async () => {
    render(<Harness />)
    await userEvent.click(box(1))
    await userEvent.keyboard('123')
    expect(value()).toBe('123')
    await userEvent.keyboard('{Backspace}')   // box 4 is empty and focused -> clears box 3 and moves back
    expect(value()).toBe('12')
    expect(box(3)).toHaveFocus()
  })

  it('arrow keys move between boxes', async () => {
    render(<Harness />)
    await userEvent.click(box(3))
    await userEvent.keyboard('{ArrowLeft}')
    expect(box(2)).toHaveFocus()
    await userEvent.keyboard('{ArrowRight}{ArrowRight}')
    expect(box(4)).toHaveFocus()
  })

  it('accepts a pasted code', async () => {
    const done = vi.fn()
    render(<Harness onComplete={done} />)
    await userEvent.click(box(1))
    await userEvent.paste('654 321')
    expect(value()).toBe('654321')
    expect(done).toHaveBeenCalledWith('654321')
  })

  it('REGRESSION: accepts a whole code dropped into one box (SMS / email autofill)', () => {
    // Found while testing in a real browser: maxLength=1 made the browser truncate autofilled codes to one digit.
    const done = vi.fn()
    render(<Harness onComplete={done} />)
    fireEvent.change(box(1), { target: { value: '574564' } })
    expect(value()).toBe('574564')
    expect(done).toHaveBeenCalledWith('574564')
    expect(box(1)).not.toHaveAttribute('maxlength')
  })

  it('autofill into a later box spreads from that box on, and never exceeds six digits', () => {
    render(<Harness />)
    fireEvent.change(box(1), { target: { value: '12' } })
    fireEvent.change(box(3), { target: { value: '99999999' } })
    expect(value()).toBe('129999')
  })

  it('typing over a filled box replaces just that digit', async () => {
    render(<Harness />)
    await userEvent.click(box(1))
    await userEvent.keyboard('123')
    await userEvent.click(box(2))   // focusing selects its content
    await userEvent.keyboard('9')
    expect(value()).toBe('193')
  })

  it('can be disabled and marked invalid', () => {
    render(<OtpInput value="" onChange={() => {}} disabled invalid />)
    expect(box(1)).toBeDisabled()
    expect(box(1)).toHaveAttribute('aria-invalid', 'true')
  })
})
