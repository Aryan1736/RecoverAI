import { useState, useId } from 'react';
import { Calendar, AlertCircle } from 'lucide-react';
import type { DateRangeParams, DateRangePreset } from '../../types/analytics';
import { Button } from '../ui/Button';

export interface DateRangeSelectorProps {
  initialPreset?: DateRangePreset;
  onChange: (range: DateRangeParams) => void;
  disabled?: boolean;
}

function formatDate(date: Date): string {
  return date.toISOString().split('T')[0];
}

function getPresetRange(preset: DateRangePreset): DateRangeParams {
  const now = new Date();
  const to = formatDate(now);

  const fromDate = new Date(now);
  switch (preset) {
    case '7d':
      fromDate.setDate(now.getDate() - 7);
      break;
    case '30d':
      fromDate.setDate(now.getDate() - 30);
      break;
    case '90d':
      fromDate.setDate(now.getDate() - 90);
      break;
    case '12m':
      fromDate.setDate(now.getDate() - 365);
      break;
    default:
      fromDate.setDate(now.getDate() - 30);
      break;
  }
  return { from: formatDate(fromDate), to };
}

export function DateRangeSelector({
  initialPreset = '30d',
  onChange,
  disabled = false,
}: DateRangeSelectorProps) {
  const [selectedPreset, setSelectedPreset] = useState<DateRangePreset>(initialPreset);
  const [showCustom, setShowCustom] = useState(false);
  const [customFrom, setCustomFrom] = useState(() => getPresetRange('30d').from || '');
  const [customTo, setCustomTo] = useState(() => getPresetRange('30d').to || '');
  const [customError, setCustomError] = useState<string | null>(null);

  const fromId = useId();
  const toId = useId();

  const presets: { id: DateRangePreset; label: string }[] = [
    { id: '7d', label: 'Last 7 Days' },
    { id: '30d', label: 'Last 30 Days' },
    { id: '90d', label: 'Last 90 Days' },
    { id: '12m', label: 'Last 12 Months' },
    { id: 'custom', label: 'Custom' },
  ];

  const handleSelectPreset = (preset: DateRangePreset) => {
    if (disabled) return;
    setSelectedPreset(preset);

    if (preset === 'custom') {
      setShowCustom(true);
    } else {
      setShowCustom(false);
      setCustomError(null);
      const range = getPresetRange(preset);
      onChange(range);
    }
  };

  const handleApplyCustom = (e: React.FormEvent) => {
    e.preventDefault();
    if (!customFrom || !customTo) {
      setCustomError('Both start date and end date are required');
      return;
    }

    const start = new Date(customFrom);
    const end = new Date(customTo);

    if (isNaN(start.getTime()) || isNaN(end.getTime())) {
      setCustomError('Invalid date format');
      return;
    }

    if (start > end) {
      setCustomError('Start date must be before or equal to end date');
      return;
    }

    const diffDays = Math.round((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
    if (diffDays > 365) {
      setCustomError('Date range cannot exceed 365 days');
      return;
    }

    setCustomError(null);
    onChange({ from: customFrom, to: customTo });
  };

  return (
    <div className="space-y-3 font-inter">
      {/* Preset Pill Buttons */}
      <div className="inline-flex flex-wrap items-center gap-1 p-1 bg-[#F1F4F2] rounded-xl border border-[#E5E9E6] text-xs">
        <div className="flex items-center gap-1.5 px-2.5 py-1 text-[#667085] font-medium">
          <Calendar className="w-3.5 h-3.5 text-[#0B8F63]" />
          <span className="hidden sm:inline text-[11px] font-semibold uppercase tracking-wider text-[#667085]">Window:</span>
        </div>

        {presets.map((preset) => {
          const isActive = selectedPreset === preset.id;
          return (
            <button
              key={preset.id}
              type="button"
              disabled={disabled}
              onClick={() => handleSelectPreset(preset.id)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all duration-150 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed ${
                isActive
                  ? 'bg-[#E8F7F0] text-[#08704F] font-semibold border border-[#0B8F63]/30 shadow-2xs'
                  : 'text-[#667085] hover:text-[#111318] hover:bg-white/80 border border-transparent'
              }`}
            >
              {preset.label}
            </button>
          );
        })}
      </div>

      {/* Custom Date Form if selected */}
      {showCustom && (
        <form
          onSubmit={handleApplyCustom}
          className="p-3 bg-white rounded-xl border border-[#E5E9E6] shadow-2xs flex flex-col sm:flex-row items-start sm:items-end gap-3 text-xs"
        >
          <div className="space-y-1">
            <label htmlFor={fromId} className="block text-[11px] font-bold text-[#667085] uppercase tracking-wider">
              From Date
            </label>
            <input
              id={fromId}
              type="date"
              value={customFrom}
              disabled={disabled}
              onChange={(e) => setCustomFrom(e.target.value)}
              className="px-3 py-1.5 bg-[#F7F8F6] border border-[#E5E9E6] rounded-lg text-[#111318] focus:outline-none focus:ring-2 focus:ring-[#0B8F63]/20 focus:border-[#0B8F63] shadow-2xs font-mono"
            />
          </div>

          <div className="space-y-1">
            <label htmlFor={toId} className="block text-[11px] font-bold text-[#667085] uppercase tracking-wider">
              To Date
            </label>
            <input
              id={toId}
              type="date"
              value={customTo}
              disabled={disabled}
              onChange={(e) => setCustomTo(e.target.value)}
              className="px-3 py-1.5 bg-[#F7F8F6] border border-[#E5E9E6] rounded-lg text-[#111318] focus:outline-none focus:ring-2 focus:ring-[#0B8F63]/20 focus:border-[#0B8F63] shadow-2xs font-mono"
            />
          </div>

          <Button
            type="submit"
            size="sm"
            variant="primary"
            disabled={disabled}
            className="bg-[#0B8F63] hover:bg-[#08704F] text-white font-semibold text-xs px-3 py-1.5 rounded-lg shadow-2xs cursor-pointer transition-all duration-150"
          >
            Apply Range
          </Button>

          {customError && (
            <div className="flex items-center gap-1.5 text-[#DC2626] text-xs self-center font-medium">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{customError}</span>
            </div>
          )}
        </form>
      )}
    </div>
  );
}
