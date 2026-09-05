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
          <div className="h-5 w-48 bg-slate-100 rounded animate-pulse" />
          <div className="h-3 w-64 bg-slate-100 rounded animate-pulse mt-1" />
        </CardHeader>
        <CardContent>
          <div className="h-64 w-full bg-slate-50 rounded-xl animate-pulse flex items-center justify-center text-slate-400 text-xs">
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
            icon={<TrendingUp className="w-8 h-8 text-emerald-600" />}
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
    <Card className="overflow-hidden shadow-2xs border-[#E5E9E6] bg-white font-inter">
      <CardHeader className="pb-4 border-b border-[#E5E9E6]/60">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-[#0B8F63]" />
              <span className="text-[11px] font-bold uppercase tracking-[0.08em] text-[#08704F]">
                Recovery Performance
              </span>
            </div>
            <CardTitle className="font-space-grotesk text-lg sm:text-xl font-bold text-[#111318]">
              Recovery Performance Trends
            </CardTitle>
            <CardDescription className="text-xs text-[#667085] flex flex-wrap items-center gap-x-2 gap-y-1">
              <span>Overall recovery rate:</span>
              <span className="font-semibold text-[#08704F] font-mono">
                {overallRecoveryRate ? `${Number(overallRecoveryRate).toFixed(1)}%` : '0.0%'}
              </span>
              <span className="text-[#D1D7D3]">•</span>
              <span>Total recovered:</span>
              <span className="font-mono font-semibold text-[#111318]">
                {formatCurrency(totalRecoveredAmount)}
              </span>
              <span>of</span>
              <span className="font-mono text-[#667085]">{formatCurrency(totalAmountAtRisk)}</span>
              <span>at risk</span>
            </CardDescription>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            {/* Metric Toggle */}
            <div className="flex items-center p-1 bg-[#F1F4F2] rounded-lg border border-[#E5E9E6] text-xs">
              <button
                type="button"
                onClick={() => setActiveMetric('amount')}
                className={`px-3 py-1 rounded-md text-xs font-medium transition-all cursor-pointer ${
                  activeMetric === 'amount'
                    ? 'bg-white text-[#111318] font-semibold shadow-2xs border border-[#E5E9E6]'
                    : 'text-[#667085] hover:text-[#111318]'
                }`}
              >
                Revenue (INR)
              </button>
              <button
                type="button"
                onClick={() => setActiveMetric('cases')}
                className={`px-3 py-1 rounded-md text-xs font-medium transition-all cursor-pointer ${
                  activeMetric === 'cases'
                    ? 'bg-white text-[#111318] font-semibold shadow-2xs border border-[#E5E9E6]'
                    : 'text-[#667085] hover:text-[#111318]'
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
              leftIcon={showTable ? <Eye className="w-3.5 h-3.5 text-[#0B8F63]" /> : <Table className="w-3.5 h-3.5 text-[#667085]" />}
              className="bg-white border-[#E5E9E6] text-[#111318] hover:bg-[#F1F4F2] text-xs font-semibold px-3 py-1.5 rounded-lg shadow-2xs cursor-pointer transition-all duration-150"
            >
              {showTable ? 'Chart' : 'Data Table'}
            </Button>
          </div>
        </div>
      </CardHeader>

      <CardContent className="pt-4">
        {/* Legend */}
        <div className="flex flex-wrap items-center gap-5 text-xs text-[#667085] mb-4 pb-2 border-b border-[#E5E9E6]/60">
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-xs bg-[#0B8F63] shadow-2xs" />
            <span className="font-semibold text-[#111318]">
              {activeMetric === 'amount' ? 'Amount Recovered' : 'Cases Recovered'}
            </span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-3.5 h-0.5 border-t-2 border-dashed border-[#98A2B3] inline-block" />
            <span className="text-[#667085]">
              {activeMetric === 'amount' ? 'Amount at Risk' : 'Cases Ingested'}
            </span>
          </div>
          <div className="ml-auto text-[11px] text-[#98A2B3] hidden sm:flex items-center gap-1.5">
            <Info className="w-3.5 h-3.5 text-[#667085]" />
            Hover points for daily breakdown
          </div>
        </div>

        {showTable ? (
          /* Accessible Data Table View */
          <div className="overflow-x-auto max-h-80 rounded-lg border border-[#E5E9E6]">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-[#E5E9E6] bg-[#F7F8F6] text-[#667085] uppercase tracking-wider font-semibold text-[11px]">
                  <th className="py-2.5 px-3">Date</th>
                  <th className="py-2.5 px-3 text-right">Cases Ingested</th>
                  <th className="py-2.5 px-3 text-right">Cases Recovered</th>
                  <th className="py-2.5 px-3 text-right">Amount at Risk</th>
                  <th className="py-2.5 px-3 text-right">Amount Recovered</th>
                  <th className="py-2.5 px-3 text-right">Recovery Rate</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#E5E9E6] text-[#111318] font-mono">
                {trends.map((t) => (
                  <tr key={t.date} className="hover:bg-[#F1F4F2]/70 transition-colors">
                    <td className="py-2.5 px-3 font-sans text-[#111318] font-medium">{formatShortDate(t.date)}</td>
                    <td className="py-2.5 px-3 text-right">{t.recoveryCasesCreated}</td>
                    <td className="py-2.5 px-3 text-right text-[#08704F] font-semibold">{t.recoveredCaseCount}</td>
                    <td className="py-2.5 px-3 text-right text-[#667085]">{formatCurrency(Number(t.amountAtRisk || 0))}</td>
                    <td className="py-2.5 px-3 text-right text-[#08704F] font-semibold">
                      {formatCurrency(Number(t.amountRecovered || 0))}
                    </td>
                    <td className="py-2.5 px-3 text-right font-bold text-[#111318]">
                      {t.recoveryRate ? `${Number(t.recoveryRate).toFixed(1)}%` : '0.0%'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          /* Interactive Responsive SVG Chart with Left-to-Right Reveal Animation */
          <div className="relative w-full overflow-hidden">
            <style>{`
              @keyframes chartLeftToRightReveal {
                0% {
                  clip-path: inset(0 100% 0 0);
                }
                100% {
                  clip-path: inset(0 0 0 0);
                }
              }
              .chart-reveal-container {
                animation: chartLeftToRightReveal 850ms cubic-bezier(0.4, 0, 0.2, 1) forwards;
              }
            `}</style>
            <svg
              viewBox={`0 0 ${width} ${height}`}
              className="w-full h-auto overflow-visible select-none"
              role="img"
              aria-label="Recovery trend time-series chart"
            >
              <defs>
                <linearGradient id="recoveredGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#0B8F63" stopOpacity="0.18" />
                  <stop offset="100%" stopColor="#0B8F63" stopOpacity="0.01" />
                </linearGradient>
              </defs>

              {/* Horizontal Gridlines & Y-Axis Labels */}
              {[0, 0.25, 0.5, 0.75, 1].map((ratio) => {
                const yVal = paddingTop + chartHeight * (1 - ratio);
                const labelVal = ratio * activeMax;
                return (
                  <g key={ratio} className="text-[#98A2B3]">
                    <line
                      x1={paddingLeft}
                      y1={yVal}
                      x2={width - paddingRight}
                      y2={yVal}
                      stroke="#E5E9E6"
                      strokeDasharray="4 4"
                    />
                    <text
                      x={paddingLeft - 10}
                      y={yVal + 3.5}
                      textAnchor="end"
                      className="text-[10px] fill-[#98A2B3] font-mono"
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
                stroke="#E5E9E6"
                strokeWidth="1.5"
              />

              {/* Area & Lines with Left-to-Right Draw/Reveal Animation */}
              <g className="chart-reveal-container">
                <path d={recoveredAreaPath} fill="url(#recoveredGradient)" />
                <path
                  d={atRiskLinePath}
                  fill="none"
                  stroke="#98A2B3"
                  strokeWidth="2"
                  strokeDasharray="4 4"
                />
                <path
                  d={recoveredLinePath}
                  fill="none"
                  stroke="#0B8F63"
                  strokeWidth="2.5"
                />
              </g>

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
                        stroke="#0B8F63"
                        strokeWidth="1.5"
                        strokeDasharray="2 2"
                        opacity={0.6}
                      />
                    )}

                    {/* Point Indicator */}
                    <circle
                      cx={cx}
                      cy={cyRecovered}
                      r={isHovered ? 6 : 3.5}
                      fill="#0B8F63"
                      stroke="#ffffff"
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

              {/* X-Axis Date Labels */}
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
                    className="text-[10px] fill-[#667085] font-sans"
                  >
                    {formatShortDate(t.date)}
                  </text>
                );
              })}
            </svg>

            {/* Hover Tooltip Overlay - White elevated panel with subtle shadow */}
            {hoveredTrend && hoveredIndex !== null && (
              <div
                className="absolute z-20 pointer-events-none transform -translate-x-1/2 bottom-12 p-3 rounded-xl bg-white border border-[#E5E9E6] shadow-lg text-xs space-y-1.5 min-w-[210px]"
                style={{
                  left: `${(getX(hoveredIndex) / width) * 100}%`,
                }}
              >
                <div className="flex items-center justify-between border-b border-[#E5E9E6] pb-1.5 font-semibold text-[#111318]">
                  <span>{formatShortDate(hoveredTrend.date)}</span>
                  <span className="text-[10px] font-mono text-[#667085]">Daily Snapshot</span>
                </div>
                <div className="text-[11px] space-y-1">
                  <div className="flex items-center justify-between gap-3 text-[#08704F] font-medium">
                    <span className="flex items-center gap-1.5">
                      <span className="w-1.5 h-1.5 rounded-full bg-[#0B8F63]" />
                      Recovered:
                    </span>
                    <span className="font-mono font-bold">
                      {formatCurrency(Number(hoveredTrend.amountRecovered || 0))}
                    </span>
                  </div>
                  <div className="flex items-center justify-between gap-3 text-[#667085]">
                    <span className="flex items-center gap-1.5">
                      <span className="w-1.5 h-1.5 rounded-full bg-[#98A2B3]" />
                      At Risk:
                    </span>
                    <span className="font-mono font-medium text-[#111318]">
                      {formatCurrency(Number(hoveredTrend.amountAtRisk || 0))}
                    </span>
                  </div>
                  <div className="flex items-center justify-between gap-3 text-[#111318] pt-1 border-t border-[#E5E9E6]/60">
                    <span className="text-[#667085]">Recovery Rate:</span>
                    <span className="font-mono font-bold text-[#08704F]">
                      {hoveredTrend.recoveryRate ? `${Number(hoveredTrend.recoveryRate).toFixed(1)}%` : '0.0%'}
                    </span>
                  </div>
                  <div className="flex items-center justify-between gap-3 text-[#667085] text-[10px]">
                    <span>Cases:</span>
                    <span className="font-mono">
                      {hoveredTrend.recoveredCaseCount} / {hoveredTrend.recoveryCasesCreated} recovered
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
