import { forwardRef, useId, type SelectHTMLAttributes, type ReactNode } from 'react';
import { ChevronDown } from 'lucide-react';

export interface SelectOption {
  value: string;
  label: string;
}

export interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  options?: SelectOption[];
  error?: string;
  helperText?: string;
  children?: ReactNode;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ label, options, error, helperText, id, className = '', children, disabled, ...props }, ref) => {
    const generatedId = useId();
    const selectId = id || generatedId;
    const errorId = `${selectId}-error`;
    const helperId = `${selectId}-helper`;

    return (
      <div className="w-full space-y-1.5 text-left">
        {label && (
          <label htmlFor={selectId} className="block text-xs font-semibold uppercase tracking-wider text-slate-700">
            {label}
            {props.required && <span className="text-rose-500 ml-1" aria-hidden="true">*</span>}
          </label>
        )}

        <div className="relative flex items-center">
          <select
            ref={ref}
            id={selectId}
            disabled={disabled}
            aria-invalid={Boolean(error)}
            aria-describedby={error ? errorId : helperText ? helperId : undefined}
            className={`w-full appearance-none rounded-lg bg-white text-slate-900 text-sm border shadow-2xs transition duration-150 focus:outline-none focus:ring-2 disabled:opacity-50 disabled:bg-slate-50 disabled:cursor-not-allowed pl-3.5 pr-10 py-2 cursor-pointer ${
              error
                ? 'border-rose-300 focus:ring-rose-500/20 focus:border-rose-600 text-rose-900'
                : 'border-slate-200 hover:border-slate-300 focus:ring-emerald-500/20 focus:border-emerald-600'
            } ${className}`}
            {...props}
          >
            {options
              ? options.map((opt) => (
                  <option key={opt.value} value={opt.value} className="bg-white text-slate-900">
                    {opt.label}
                  </option>
                ))
              : children}
          </select>

          <div className="absolute right-3 text-slate-400 pointer-events-none flex items-center justify-center">
            <ChevronDown className="w-4 h-4" />
          </div>
        </div>

        {error ? (
          <p id={errorId} role="alert" className="text-xs font-medium text-rose-600">
            {error}
          </p>
        ) : helperText ? (
          <p id={helperId} className="text-xs text-slate-500">
            {helperText}
          </p>
        ) : null}
      </div>
    );
  }
);

Select.displayName = 'Select';
