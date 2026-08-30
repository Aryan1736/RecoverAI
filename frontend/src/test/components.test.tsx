import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { PasswordInput } from '../components/ui/PasswordInput';
import { Card, CardHeader, CardTitle, CardContent } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Alert } from '../components/ui/Alert';
import { Avatar } from '../components/ui/Avatar';
import { EmptyState } from '../components/ui/EmptyState';
import { ErrorState } from '../components/ui/ErrorState';
import { PageHeader } from '../components/ui/PageHeader';
import { Sidebar } from '../components/layout/Sidebar';

describe('Design System UI Components', () => {
  describe('Button Component', () => {
    it('renders text and handles click events', async () => {
      const user = userEvent.setup();
      const handleClick = vi.fn();

      render(<Button onClick={handleClick}>Click Me</Button>);

      const button = screen.getByRole('button', { name: /Click Me/i });
      expect(button).toBeInTheDocument();
      await user.click(button);
      expect(handleClick).toHaveBeenCalledOnce();
    });

    it('displays loading spinner and disables button when isLoading is true', () => {
      render(<Button isLoading>Processing</Button>);

      const button = screen.getByRole('button');
      expect(button).toBeDisabled();
      expect(button.querySelector('.animate-spin')).toBeInTheDocument();
    });

    it('renders different variants with appropriate classes', () => {
      const { rerender } = render(<Button variant="danger">Delete</Button>);
      expect(screen.getByRole('button')).toHaveClass('bg-rose-600');

      rerender(<Button variant="secondary">Secondary</Button>);
      expect(screen.getByRole('button')).toHaveClass('bg-slate-800');
    });
  });

  describe('Input Component', () => {
    it('renders with label and helper text', () => {
      render(
        <Input
          label="API Key"
          helperText="Your private key"
          placeholder="key_xxx"
        />
      );

      expect(screen.getByLabelText(/API Key/i)).toBeInTheDocument();
      expect(screen.getByText(/Your private key/i)).toBeInTheDocument();
    });

    it('renders error message with role="alert" and aria-invalid', () => {
      render(<Input label="Email" error="Invalid email address" />);

      const input = screen.getByLabelText(/Email/i);
      expect(input).toHaveAttribute('aria-invalid', 'true');
      expect(screen.getByRole('alert')).toHaveTextContent('Invalid email address');
    });
  });

  describe('PasswordInput Component', () => {
    it('toggles visibility from password to text on click', async () => {
      const user = userEvent.setup();
      render(<PasswordInput label="Secret" />);

      const input = screen.getByLabelText(/Secret/i);
      expect(input).toHaveAttribute('type', 'password');

      const toggleBtn = screen.getByLabelText('Show password');
      await user.click(toggleBtn);
      expect(input).toHaveAttribute('type', 'text');

      await user.click(screen.getByLabelText('Hide password'));
      expect(input).toHaveAttribute('type', 'password');
    });
  });

  describe('Card, Badge, Alert Components', () => {
    it('renders Card with header and content', () => {
      render(
        <Card>
          <CardHeader>
            <CardTitle>Metric Card</CardTitle>
          </CardHeader>
          <CardContent>
            <p>Content Body</p>
          </CardContent>
        </Card>
      );

      expect(screen.getByText('Metric Card')).toBeInTheDocument();
      expect(screen.getByText('Content Body')).toBeInTheDocument();
    });

    it('renders Badge with dot indicator', () => {
      render(<Badge variant="success" dot>Active Status</Badge>);

      expect(screen.getByText('Active Status')).toBeInTheDocument();
    });

    it('renders Alert and allows dismissal', async () => {
      const user = userEvent.setup();
      const onDismiss = vi.fn();

      render(
        <Alert type="warning" title="Warning Note" dismissible onDismiss={onDismiss}>
          Please take caution.
        </Alert>
      );

      expect(screen.getByText('Warning Note')).toBeInTheDocument();
      expect(screen.getByText('Please take caution.')).toBeInTheDocument();

      const closeBtn = screen.getByLabelText('Dismiss alert');
      await user.click(closeBtn);
      expect(onDismiss).toHaveBeenCalledOnce();
      expect(screen.queryByText('Warning Note')).not.toBeInTheDocument();
    });
  });

  describe('Avatar & PageHeader Components', () => {
    it('renders Avatar with generated initials', () => {
      render(<Avatar name="Acme Recovery" />);
      expect(screen.getByRole('img')).toHaveTextContent('AR');
    });

    it('renders PageHeader with title, description, and badge', () => {
      render(
        <PageHeader
          title="Payment Recovery"
          description="Real-time recovery operations"
          badge={<Badge variant="info">Live</Badge>}
        />
      );

      expect(screen.getByRole('heading', { name: 'Payment Recovery' })).toBeInTheDocument();
      expect(screen.getByText('Real-time recovery operations')).toBeInTheDocument();
      expect(screen.getByText('Live')).toBeInTheDocument();
    });
  });

  describe('EmptyState & ErrorState Components', () => {
    it('renders EmptyState with action button', () => {
      render(
        <EmptyState
          title="No Records"
          description="Empty database table"
          action={<Button size="sm">Create First</Button>}
        />
      );

      expect(screen.getByText('No Records')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Create First' })).toBeInTheDocument();
    });

    it('renders ErrorState with retry button', async () => {
      const user = userEvent.setup();
      const onRetry = vi.fn();

      render(<ErrorState message="Failed to connect" onRetry={onRetry} />);

      expect(screen.getByText('Failed to connect')).toBeInTheDocument();
      const retryBtn = screen.getByRole('button', { name: /Try Again/i });
      await user.click(retryBtn);
      expect(onRetry).toHaveBeenCalledOnce();
    });
  });

  describe('Sidebar Navigation Component', () => {
    it('renders navigation links and handles collapse toggle', async () => {
      const user = userEvent.setup();
      const setIsCollapsed = vi.fn();
      const setIsMobileOpen = vi.fn();

      render(
        <MemoryRouter initialEntries={['/app']}>
          <Sidebar
            isCollapsed={false}
            setIsCollapsed={setIsCollapsed}
            isMobileOpen={false}
            setIsMobileOpen={setIsMobileOpen}
          />
        </MemoryRouter>
      );

      expect(screen.getByText('Overview')).toBeInTheDocument();
      expect(screen.getByText('Recovery Cases')).toBeInTheDocument();
      expect(screen.getByText('Analytics')).toBeInTheDocument();

      const collapseBtn = screen.getByLabelText('Collapse sidebar');
      await user.click(collapseBtn);
      expect(setIsCollapsed).toHaveBeenCalled();
    });
  });
});
