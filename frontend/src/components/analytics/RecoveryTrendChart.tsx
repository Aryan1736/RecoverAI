import { useState } from 'react';
import { Table, Eye, TrendingUp, Info } from 'lucide-react';
import type { DailyRecoveryTrend } from '../../types/analytics';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../ui/Card';
import { Button } from '../ui/Button';
import { EmptyState } from '../ui/EmptyState';

export interface RecoveryTrendChartProps {
  trends: DailyRecoveryTrend[];
  totalAmountAtRisk?: number;
  totalRecoveredAmount?: number;
  overallRecoveryRate?: number;
  isLoading?: boolean;
}

export function RecoveryTrendChart({
  trends,
  totalAmountAtRisk = 0,
  totalRecoveredAmount = 0,
  overallRecoveryRate = 0,
  isLoading = false,
}: RecoveryTrendChartProps) {
  const [activeMetric, setActiveMetric] = useState<'amount' | 'cases'>('amount');
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
  const [showTable, setShowTable] = useState(false);

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(val);
  };

  const formatShortDate = (dateStr: string) => {
    try {
      const d = new Date(dateStr + 'T00:00:00');
      return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    } catch {
      return dateStr;
    }
  };

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <div className="h-5 w-48 bg-slate-800 rounded animate-pulse" />
          <div className="h-3 w-64 bg-slate-800/60 rounded animate-pulse mt-1" />
        </CardHeader>
        <CardContent>
          <div className="h-64 w-full bg-slate-900/60 rounded-xl animate-pulse flex items-center justify-center text-slate-600 text-xs">
            Loading recovery trends...
          </div>
        </CardContent>
      </Card>
    );
  }

  if (!trends || trends.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Recovery Performance Trends</CardTitle>
          <CardDescription>Daily revenue recovery trajectory over selected timeframe</CardDescription>
        </CardHeader>
        <CardContent>
          <EmptyState
            icon={<TrendingUp className="w-8 h-8 text-indigo-400" />}
            title="No Trend Data Available"
            description="There are no recovery cases recorded within this date range to plot trends."
          />
        </CardContent>
      </Card>
    );
  }

  // Chart dimensions and SVG layout
  const width = 800;
  const height = 260;
  const paddingLeft = 65;
  const paddingRight = 20;
  const paddingTop = 25;
  const paddingBottom = 40;

  const chartWidth = width - paddingLeft - paddingRight;
  const chartHeight = height - paddingTop - paddingBottom;

  // Max scale calculation
  const maxAmount = Math.max(
    ...trends.map((t) => Math.max(Number(t.amountAtRisk || 0), Number(t.amountRecovered || 0))),
    100
  );
  const maxCases = Math.max(
    ...trends.map((t) => Math.max(Number(t.recoveryCasesCreated || 0), Number(t.recoveredCaseCount || 0))),
    5
  );

  const activeMax = activeMetric === 'amount' ? maxAmount : maxCases;

  const getX = (index: number) => {
    if (trends.length <= 1) return paddingLeft + chartWidth / 2;
    return paddingLeft + (index / (trends.length - 1)) * chartWidth;
  };

  const getY = (val: number) => {
    const safeVal = Math.max(0, val);
    return paddingTop + chartHeight - (safeVal / activeMax) * chartHeight;
  };

  // Build SVG Paths
  const generateAreaPath = (points: { x: number; y: number }[]) => {
    if (points.length === 0) return '';
    const first = points[0];
    const last = points[points.length - 1];
    const lineCommands = points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ');
    return `${lineCommands} L ${last.x} ${paddingTop + chartHeight} L ${first.x} ${paddingTop + chartHeight} Z`;
  };

  const generateLinePath = (points: { x: number; y: number }[]) => {
    if (points.length === 0) return '';
    return points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x} ${p.y}`).join(' ');
  };

  const recoveredPoints = trends.map((t, idx) => ({
    x: getX(idx),
    y: getY(activeMetric === 'amount' ? Number(t.amountRecovered || 0) : Number(t.recoveredCaseCount || 0)),
  }));

  const atRiskPoints = trends.map((t, idx) => ({
    x: getX(idx),
    y: getY(activeMetric === 'amount' ? Number(t.amountAtRisk || 0) : Number(t.recoveryCasesCreated || 0)),
  }));

  const recoveredAreaPath = generateAreaPath(recoveredPoints);
  const recoveredLinePath = generateLinePath(recoveredPoints);
  const atRiskLinePath = generateLinePath(atRiskPoints);

  const hoveredTrend = hoveredIndex !== null ? trends[hoveredIndex] : null;

  return (
    <Card className="overflow-hidden">
      <CardHeader>
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div>
            <CardTitle>Recovery Performance Trends</CardTitle>
            <CardDescription>
              Overall recovery rate:{' '}
              <span className="font-semibold text-emerald-400 font-mono">
                {overallRecoveryRate ? `${Number(overallRecoveryRate).toFixed(1)}%` : '0.0%'}
              </span>{' '}
              • Total recovered: <span className="font-mono text-slate-200">{formatCurrency(totalRecoveredAmount)}</span> of{' '}
              <span className="font-mono text-slate-400">{formatCurrency(totalAmountAtRisk)}</span> at risk
            </CardDescription>
          </div>

          <div className="flex items-center gap-2">
            {/* Metric Toggle */}
            <div className="flex items-center p-1 bg-slate-950 rounded-lg border border-slate-800 text-xs">
              <button
                type="button"
                onClick={() => setActiveMetric('amount')}
                className={`px-2.5 py-1 rounded-md font-medium transition cursor-pointer ${
                  activeMetric === 'amount'
                    ? 'bg-indigo-600 text-white font-semibold'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                Revenue (INR)
              </button>
              <button
                type="button"
                onClick={() => setActiveMetric('cases')}
                className={`px-2.5 py-1 rounded-md font-medium transition cursor-pointer ${
                  activeMetric === 'cases'
                    ? 'bg-indigo-600 text-white font-semibold'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                Cases Volume
              </button>
            </div>

            {/* Accessible Table Toggle */}
            <Button
              variant="outline"
              size="sm"
              onClick={() => setShowTable(!showTable)}
              title={showTable ? 'Hide data table' : 'Show accessible data table'}
              leftIcon={showTable ? <Eye className="w-3.5 h-3.5" /> : <Table className="w-3.5 h-3.5" />}
            >
              {showTable ? 'Chart' : 'Data Table'}
            </Button>
          </div>
        </div>
      </CardHeader>

      <CardContent>
        {/* Legend */}
        <div className="flex flex-wrap items-center gap-5 text-xs text-slate-400 mb-3 pb-2 border-b border-slate-800/60">
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-xs bg-emerald-500" />
            <span>
              {activeMetric === 'amount' ? 'Amount Recovered' : 'Cases Recovered'}
            </span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-3 h-0.5 border-t-2 border-dashed border-indigo-400 inline-block" />
            <span>
              {activeMetric === 'amount' ? 'Amount at Risk' : 'Cases Ingested'}
            </span>
          </div>
          <div className="ml-auto text-[11px] text-slate-400 hidden sm:flex items-center gap-1">
            <Info className="w-3.5 h-3.5" />
            Hover over points for daily breakdown
          </div>
        </div>

        {showTable ? (
          /* Accessible Data Table View */
          <div className="overflow-x-auto max-h-72">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-800 text-slate-400 uppercase tracking-wider font-semibold">
                  <th className="py-2 px-3">Date</th>
                  <th className="py-2 px-3 text-right">Cases Ingested</th>
                  <th className="py-2 px-3 text-right">Cases Recovered</th>
                  <th className="py-2 px-3 text-right">Amount at Risk</th>
                  <th className="py-2 px-3 text-right">Amount Recovered</th>
                  <th className="py-2 px-3 text-right">Recovery Rate</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-slate-300 font-mono">
                {trends.map((t) => (
                  <tr key={t.date} className="hover:bg-slate-900/50">
                    <td className="py-2 px-3 font-sans text-slate-200">{formatShortDate(t.date)}</td>
                    <td className="py-2 px-3 text-right">{t.recoveryCasesCreated}</td>
                    <td className="py-2 px-3 text-right text-emerald-400">{t.recoveredCaseCount}</td>
                    <td className="py-2 px-3 text-right">{formatCurrency(Number(t.amountAtRisk || 0))}</td>
                    <td className="py-2 px-3 text-right text-emerald-400">
                      {formatCurrency(Number(t.amountRecovered || 0))}
                    </td>
                    <td className="py-2 px-3 text-right">
                      {t.recoveryRate ? `${Number(t.recoveryRate).toFixed(1)}%` : '0.0%'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          /* Interactive Responsive SVG Chart */
          <div className="relative w-full overflow-hidden">
            <svg
              viewBox={`0 0 ${width} ${height}`}
              className="w-full h-auto overflow-visible select-none"
              role="img"
              aria-label="Recovery trend time-series chart"
            >
              <defs>
                <linearGradient id="recoveredGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#10b981" stopOpacity="0.28" />
                  <stop offset="100%" stopColor="#10b981" stopOpacity="0.0" />
                </linearGradient>
              </defs>

              {/* Horizontal Gridlines & Y-Axis Labels */}
              {[0, 0.25, 0.5, 0.75, 1].map((ratio) => {
                const yVal = paddingTop + chartHeight * (1 - ratio);
                const labelVal = ratio * activeMax;
                return (
                  <g key={ratio} className="text-slate-400">
                    <line
                      x1={paddingLeft}
                      y1={yVal}
                      x2={width - paddingRight}
                      y2={yVal}
                      stroke="#334155"
                      strokeDasharray="3 3"
                      strokeOpacity="0.4"
                    />
                    <text
                      x={paddingLeft - 8}
                      y={yVal + 3}
                      textAnchor="end"
                      className="text-[10px] fill-slate-300 font-mono"
                    >
                      {activeMetric === 'amount'
                        ? labelVal >= 1000
                          ? `₹${(labelVal / 1000).toFixed(0)}k`
                          : `₹${labelVal.toFixed(0)}`
                        : labelVal.toFixed(0)}
                    </text>
                  </g>
                );
              })}

              {/* X-Axis Baseline */}
              <line
                x1={paddingLeft}
                y1={paddingTop + chartHeight}
                x2={width - paddingRight}
                y2={paddingTop + chartHeight}
                stroke="#475569"
                strokeWidth="1"
              />

              {/* Area & Lines */}
              <path d={recoveredAreaPath} fill="url(#recoveredGradient)" />
              <path
                d={atRiskLinePath}
                fill="none"
                stroke="#818cf8"
                strokeWidth="2"
                strokeDasharray="4 4"
              />
              <path
                d={recoveredLinePath}
                fill="none"
                stroke="#10b981"
                strokeWidth="2.5"
              />

              {/* Interactive Points and Vertical Guides */}
              {trends.map((t, idx) => {
                const cx = getX(idx);
                const cyRecovered = getY(
                  activeMetric === 'amount' ? Number(t.amountRecovered || 0) : Number(t.recoveredCaseCount || 0)
                );
                const isHovered = hoveredIndex === idx;

                return (
                  <g key={t.date}>
                    {/* Hover vertical guide line */}
                    {isHovered && (
                      <line
                        x1={cx}
                        y1={paddingTop}
                        x2={cx}
                        y2={paddingTop + chartHeight}
                        stroke="#64748b"
                        strokeWidth="1.5"
                        strokeDasharray="2 2"
                      />
                    )}

                    {/* Point Indicator */}
                    <circle
                      cx={cx}
                      cy={cyRecovered}
                      r={isHovered ? 5.5 : 3.5}
                      fill="#10b981"
                      stroke="#0f172a"
                      strokeWidth="2"
                      className="transition-all duration-150"
                    />

                    {/* Transparent interaction target for mouse/focus */}
                    <rect
                      x={cx - chartWidth / (trends.length * 2 || 1)}
                      y={paddingTop}
                      width={chartWidth / (trends.length || 1)}
                      height={chartHeight}
                      fill="transparent"
                      className="cursor-pointer"
                      onMouseEnter={() => setHoveredIndex(idx)}
                      onMouseLeave={() => setHoveredIndex(null)}
                      onFocus={() => setHoveredIndex(idx)}
                      onBlur={() => setHoveredIndex(null)}
                      tabIndex={0}
                      aria-label={`${formatShortDate(t.date)}: Recovered ${
                        activeMetric === 'amount' ? formatCurrency(t.amountRecovered) : `${t.recoveredCaseCount} cases`
                      }`}
                    />
                  </g>
                );
              })}

              {/* X-Axis Date Labels (Step sampled to avoid crowd) */}
              {trends.map((t, idx) => {
                const step = Math.ceil(trends.length / 7);
                if (idx % step !== 0 && idx !== trends.length - 1) return null;
                const cx = getX(idx);
                return (
                  <text
                    key={t.date}
                    x={cx}
                    y={paddingTop + chartHeight + 20}
                    textAnchor="middle"
                    className="text-[10px] fill-slate-300 font-sans"
                  >
                    {formatShortDate(t.date)}
                  </text>
                );
              })}
            </svg>

            {/* Hover Tooltip Overlay */}
            {hoveredTrend && hoveredIndex !== null && (
              <div
                className="absolute z-20 pointer-events-none transform -translate-x-1/2 bottom-12 p-2.5 rounded-xl bg-slate-950 border border-slate-700 shadow-xl text-xs space-y-1"
                style={{
                  left: `${(getX(hoveredIndex) / width) * 100}%`,
                }}
              >
                <div className="font-semibold text-white border-b border-slate-800 pb-1">
                  {formatShortDate(hoveredTrend.date)}
                </div>
                <div className="text-[11px] space-y-0.5">
                  <div className="flex items-center justify-between gap-4 text-emerald-400">
                    <span>Recovered:</span>
                    <span className="font-mono font-semibold">
                      {formatCurrency(Number(hoveredTrend.amountRecovered || 0))} ({hoveredTrend.recoveredCaseCount} cases)
                    </span>
                  </div>
                  <div className="flex items-center justify-between gap-4 text-indigo-300">
                    <span>At Risk:</span>
                    <span className="font-mono font-semibold">
                      {formatCurrency(Number(hoveredTrend.amountAtRisk || 0))} ({hoveredTrend.recoveryCasesCreated} cases)
                    </span>
                  </div>
                  <div className="flex items-center justify-between gap-4 text-slate-400">
                    <span>Rate:</span>
                    <span className="font-mono font-semibold text-slate-200">
                      {hoveredTrend.recoveryRate ? `${Number(hoveredTrend.recoveryRate).toFixed(1)}%` : '0.0%'}
                    </span>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
