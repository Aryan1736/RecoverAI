import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from './Button';

export interface PaginationProps {
  page: number; // 0-indexed
  totalPages: number;
  totalElements?: number;
  size?: number;
  onPageChange: (newPage: number) => void;
  className?: string;
}

export function Pagination({
  page,
  totalPages,
  totalElements,
  size = 20,
  onPageChange,
  className = '',
}: PaginationProps) {
  if (totalPages <= 1 && (totalElements === undefined || totalElements === 0)) {
    return null;
  }

  const startRecord = page * size + 1;
  const endRecord = totalElements !== undefined ? Math.min((page + 1) * size, totalElements) : (page + 1) * size;

  // Generate page numbers to display (compact window)
  const getPageNumbers = () => {
    const pages: number[] = [];
    const maxVisible = 5;
    let start = Math.max(0, page - Math.floor(maxVisible / 2));
    const end = Math.min(totalPages, start + maxVisible);

    if (end - start < maxVisible) {
      start = Math.max(0, end - maxVisible);
    }

    for (let i = start; i < end; i++) {
      pages.push(i);
    }
    return pages;
  };

  return (
    <nav
      aria-label="Pagination Navigation"
      className={`flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 py-3 text-xs text-slate-400 ${className}`}
    >
      {totalElements !== undefined ? (
        <div>
          Showing <span className="font-medium text-slate-200">{startRecord}</span> to{' '}
          <span className="font-medium text-slate-200">{endRecord}</span> of{' '}
          <span className="font-medium text-slate-200">{totalElements}</span> results
        </div>
      ) : (
        <div>
          Page <span className="font-medium text-slate-200">{page + 1}</span> of{' '}
          <span className="font-medium text-slate-200">{totalPages}</span>
        </div>
      )}

      <div className="flex items-center gap-1.5 self-center sm:self-auto">
        <Button
          variant="outline"
          size="sm"
          disabled={page <= 0}
          onClick={() => onPageChange(page - 1)}
          aria-label="Go to previous page"
          leftIcon={<ChevronLeft className="w-4 h-4" />}
        >
          Previous
        </Button>

        <div className="hidden sm:flex items-center gap-1">
          {getPageNumbers().map((p) => {
            const isCurrent = p === page;
            return (
              <button
                key={p}
                type="button"
                onClick={() => onPageChange(p)}
                aria-current={isCurrent ? 'page' : undefined}
                aria-label={`Page ${p + 1}`}
                className={`min-w-8 h-8 px-2 rounded-lg text-xs font-medium transition cursor-pointer ${
                  isCurrent
                    ? 'bg-indigo-600 text-white font-bold'
                    : 'text-slate-400 hover:text-white hover:bg-slate-800'
                }`}
              >
                {p + 1}
              </button>
            );
          })}
        </div>

        <Button
          variant="outline"
          size="sm"
          disabled={page >= totalPages - 1 || totalPages === 0}
          onClick={() => onPageChange(page + 1)}
          aria-label="Go to next page"
          rightIcon={<ChevronRight className="w-4 h-4" />}
        >
          Next
        </Button>
      </div>
    </nav>
  );
}
