import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnChanges,
  SimpleChanges,
  ViewChild,
  ElementRef,
  AfterViewInit,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData, ChartType, ChartOptions } from 'chart.js';
import {
  Chart,
  LineController,
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,
  TimeScale,
  Filler,
  Tooltip,
  Legend,
} from 'chart.js';
import { PricePoint } from '../../models/shared.models';
import { LoadingSpinnerComponent } from '../loading-spinner/loading-spinner.component';

Chart.register(
  LineController,
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,
  TimeScale,
  Filler,
  Tooltip,
  Legend
);

@Component({
  selector: 'app-price-chart',
  standalone: true,
  imports: [CommonModule, BaseChartDirective, LoadingSpinnerComponent],
  templateUrl: './price-chart.component.html',
  styleUrls: ['./price-chart.component.scss'],
})
export class PriceChartComponent implements OnChanges, AfterViewInit {
  @Input() priceHistory: PricePoint[] = [];
  @Input() range: string = '1M';
  @Input() loading: boolean = false;

  @Output() rangeChange = new EventEmitter<string>();

  @ViewChild(BaseChartDirective) chart?: BaseChartDirective;

  ranges: string[] = ['1M', '3M', '6M', '1Y', '5Y', 'MAX'];

  lineChartType: 'line' = 'line';

  lineChartData: ChartData<'line'> = {
    labels: [],
    datasets: [
      {
        data: [],
        label: 'Price',
        borderColor: '#3b82f6',
        backgroundColor: (ctx: any) => {
          if (!ctx.chart.chartArea) return 'rgba(59,130,246,0.1)';
          const gradient = ctx.chart.ctx.createLinearGradient(
            0,
            ctx.chart.chartArea.top,
            0,
            ctx.chart.chartArea.bottom
          );
          gradient.addColorStop(0, 'rgba(59,130,246,0.25)');
          gradient.addColorStop(1, 'rgba(59,130,246,0.02)');
          return gradient;
        },
        fill: true,
        tension: 0.35,
        pointRadius: 0,
        pointHoverRadius: 5,
        pointHoverBackgroundColor: '#3b82f6',
        borderWidth: 2,
      },
    ],
  };

  lineChartOptions: ChartOptions<'line'> = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: {
      intersect: false,
      mode: 'index',
    },
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#1f2937',
        titleColor: '#f9fafb',
        bodyColor: '#d1d5db',
        borderColor: '#374151',
        borderWidth: 1,
        padding: 12,
        cornerRadius: 8,
        displayColors: false,
        callbacks: {
          label: (ctx) => `$${(ctx.parsed.y as number).toFixed(2)}`,
        },
      },
    },
    scales: {
      x: {
        display: true,
        grid: { display: false },
        ticks: {
          maxTicksLimit: 8,
          color: '#9ca3af',
          font: { size: 11 },
        },
      },
      y: {
        display: true,
        position: 'right',
        grid: {
          color: '#f3f4f6',
        },
        ticks: {
          color: '#9ca3af',
          font: { size: 11 },
          callback: (value) => `$${value}`,
        },
      },
    },
  };

  ngAfterViewInit(): void {
    this.updateChart();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['priceHistory'] && this.priceHistory) {
      this.updateChart();
    }
  }

  onRangeSelect(range: string): void {
    this.range = range;
    this.rangeChange.emit(range);
  }

  private updateChart(): void {
    if (!this.priceHistory || this.priceHistory.length === 0) {
      this.lineChartData.labels = [];
      this.lineChartData.datasets[0].data = [];
      return;
    }

    const sorted = [...this.priceHistory].sort(
      (a, b) => new Date(a.date).getTime() - new Date(b.date).getTime()
    );

    this.lineChartData.labels = sorted.map((p) => {
      const d = new Date(p.date);
      return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    });

    this.lineChartData.datasets[0].data = sorted.map((p) => p.adjustedClose ?? p.close);

    // Force chart update
    this.lineChartData = { ...this.lineChartData };

    setTimeout(() => {
      this.chart?.update();
    });
  }
}
