import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { ThemeProvider, useTheme } from '../ThemeContext';

const ThemeConsumer = () => {
  const { theme, isDark, toggleTheme } = useTheme();
  return (
    <>
      <span>{theme}</span>
      <span>{isDark ? 'dark-on' : 'dark-off'}</span>
      <button onClick={toggleTheme}>toggle</button>
    </>
  );
};

describe('ThemeContext', () => {
  it('usa dark por defecto y persiste en localStorage', () => {
    localStorage.removeItem('theme');

    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>
    );

    expect(screen.getByText('dark')).toBeInTheDocument();
    expect(screen.getByText('dark-on')).toBeInTheDocument();
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(localStorage.getItem('theme')).toBe('dark');
  });

  it('lee tema previo desde localStorage', () => {
    localStorage.setItem('theme', 'light');

    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>
    );

    expect(screen.getByText('light')).toBeInTheDocument();
    expect(screen.getByText('dark-off')).toBeInTheDocument();
  });

  it('toggleTheme alterna entre light y dark', async () => {
    const user = userEvent.setup();
    localStorage.setItem('theme', 'dark');

    render(
      <ThemeProvider>
        <ThemeConsumer />
      </ThemeProvider>
    );

    await user.click(screen.getByRole('button', { name: 'toggle' }));

    expect(screen.getByText('light')).toBeInTheDocument();
    expect(localStorage.getItem('theme')).toBe('light');
  });

  it('useTheme falla fuera del provider', () => {
    expect(() => render(<ThemeConsumer />)).toThrow('useTheme debe usarse dentro de ThemeProvider');
  });
});
