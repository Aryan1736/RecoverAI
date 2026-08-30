export interface AvatarProps {
  name: string;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

export function Avatar({ name, size = 'md', className = '' }: AvatarProps) {
  const getInitials = (text: string): string => {
    if (!text) return 'M';
    const parts = text.trim().split(/\s+/);
    if (parts.length === 1) {
      return parts[0].slice(0, 2).toUpperCase();
    }
    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
  };

  const sizeStyles = {
    sm: 'w-7 h-7 text-xs',
    md: 'w-9 h-9 text-xs',
    lg: 'w-11 h-11 text-sm',
  };

  return (
    <div
      aria-label={`Avatar for ${name}`}
      role="img"
      className={`inline-flex items-center justify-center font-bold tracking-tight rounded-full bg-emerald-600 text-white border border-emerald-700/20 select-none shadow-2xs ${sizeStyles[size]} ${className}`}
    >
      {getInitials(name)}
    </div>
  );
}
