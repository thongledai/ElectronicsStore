/* ==========================================================================
   TechNova Electronics — Manager Statistics (Thống kê chuyên sâu)
   ========================================================================== */

'use strict';

(function () {
  const token = localStorage.getItem('technova_jwt');

  function formatVnd(value) {
    const num = Number(value || 0);
    return num.toLocaleString('vi-VN') + ' ₫';
  }

  function escHtml(str) {
    return String(str || '').replace(/[&<>"']/g, ch => (
      { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch] || ch));
  }

  let analyticsChartInstance = null;
  let categoryChartInstance = null;

  function renderAnalyticsChart(chartPoints, chartType, metricType) {
    const canvas = document.getElementById('analyticsChart');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    const labels = (chartPoints || []).map(p => p.label);
    const dataValues = (chartPoints || []).map(p => Number(p.value || 0));

    if (analyticsChartInstance) {
      analyticsChartInstance.destroy();
    }

    const isRevenue = (metricType !== 'orders');
    const labelTitle = isRevenue ? 'Doanh thu (₫)' : 'Số đơn hàng';

    let bg = '#1b6df3';
    if (chartType === 'line') {
      const gradient = ctx.createLinearGradient(0, 0, 0, 320);
      gradient.addColorStop(0, 'rgba(27, 109, 243, 0.35)');
      gradient.addColorStop(1, 'rgba(27, 109, 243, 0.0)');
      bg = gradient;
    }

    analyticsChartInstance = new Chart(ctx, {
      type: chartType || 'bar',
      data: {
        labels: labels,
        datasets: [{
          label: labelTitle,
          data: dataValues,
          borderColor: '#1b6df3',
          backgroundColor: bg,
          borderWidth: chartType === 'line' ? 2.5 : 1,
          borderRadius: chartType === 'bar' ? 4 : 0,
          fill: chartType === 'line',
          tension: 0.35,
          pointBackgroundColor: '#1b6df3',
          pointBorderColor: '#ffffff',
          pointBorderWidth: 2,
          pointRadius: chartType === 'line' ? 4 : 0
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: function (context) {
                return ' ' + (isRevenue ? formatVnd(context.parsed.y) : context.parsed.y + ' đơn');
              }
            }
          }
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: { font: { size: 11 } }
          },
          y: {
            beginAtZero: true,
            grid: { color: 'rgba(0, 0, 0, 0.05)' },
            ticks: {
              font: { size: 11 },
              callback: function (val) {
                if (!isRevenue) return val;
                if (val >= 1_000_000) return (val / 1_000_000).toFixed(0) + 'M';
                if (val >= 1_000) return (val / 1_000).toFixed(0) + 'K';
                return val;
              }
            }
          }
        }
      }
    });
  }

  function renderCategoryChart(categories) {
    const canvas = document.getElementById('categoryChart');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    const labels = (categories || []).map(c => c.categoryName);
    const dataValues = (categories || []).map(c => Number(c.totalRevenue || 0));

    if (categoryChartInstance) {
      categoryChartInstance.destroy();
    }

    if (!dataValues.length || dataValues.every(v => v === 0)) {
      // Empty chart placeholder
      return;
    }

    const palette = ['#1b6df3', '#ff7a18', '#12b886', '#e03131', '#7950f2', '#f59f00', '#20c997'];

    categoryChartInstance = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: dataValues,
          backgroundColor: palette.slice(0, labels.length),
          borderWidth: 2,
          borderColor: '#ffffff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: { boxWidth: 12, font: { size: 11 } }
          },
          tooltip: {
            callbacks: {
              label: function (context) {
                return ` ${context.label}: ${formatVnd(context.parsed)}`;
              }
            }
          }
        },
        cutout: '65%'
      }
    });
  }

  async function fetchAnalyticsData() {
    const period = document.getElementById('filterPeriod')?.value || '30days';
    const type = document.getElementById('filterType')?.value || 'revenue';
    const chartType = document.getElementById('filterChartType')?.value || 'bar';
    const startDate = document.getElementById('filterStartDate')?.value || '';
    const endDate = document.getElementById('filterEndDate')?.value || '';

    const params = new URLSearchParams();
    params.set('period', period);
    params.set('type', type);
    if (period === 'custom') {
      if (startDate) params.set('startDate', startDate);
      if (endDate) params.set('endDate', endDate);
    }

    try {
      const headers = { 'Accept': 'application/json' };
      if (token) headers['Authorization'] = 'Bearer ' + token;

      const res = await fetch(`/api/manager/statistics/analytics?${params.toString()}`, { headers }).then(r => r.json());
      if (!res || !res.success || !res.data) {
        console.warn('Không thể tải dữ liệu thống kê:', res);
        return;
      }

      const d = res.data;

      // 1. Cập nhật thẻ tóm tắt
      const statRev = document.getElementById('statPeriodRevenue');
      if (statRev) statRev.textContent = formatVnd(d.totalRevenue);

      const statOrd = document.getElementById('statPeriodOrders');
      if (statOrd) statOrd.textContent = Number(d.totalOrders || 0).toLocaleString('vi-VN');

      const statUsr = document.getElementById('statPeriodUsers');
      if (statUsr) statUsr.textContent = Number(d.newUsers || 0).toLocaleString('vi-VN');

      // 2. Vẽ biểu đồ chính
      renderAnalyticsChart(d.chartData || [], chartType, type);

      // 3. Render Top 5 sản phẩm
      const topTbody = document.getElementById('topProductsBody');
      if (topTbody) {
        if (!d.topProducts || !d.topProducts.length) {
          topTbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3">Chưa có dữ liệu bán hàng trong kỳ này.</td></tr>';
        } else {
          topTbody.innerHTML = d.topProducts.map((p, idx) => `
            <tr>
              <td class="text-center fw-bold text-muted" style="width: 40px;">#${idx + 1}</td>
              <td>
                <div class="d-flex align-items-center gap-2">
                  <img src="${escHtml(p.thumbnailUrl || '/images/placeholder.webp')}" class="rounded border object-fit-cover" style="width: 36px; height: 36px;" onerror="this.onerror=null;this.src='/images/placeholder.webp';">
                  <span class="fw-semibold text-truncate" style="max-width: 260px;" title="${escHtml(p.productName)}">${escHtml(p.productName)}</span>
                </div>
              </td>
              <td class="text-end fw-semibold">${Number(p.totalSold || 0).toLocaleString('vi-VN')}</td>
              <td class="text-end text-primary fw-bold">${formatVnd(p.totalRevenue)}</td>
            </tr>
          `).join('');
        }
      }

      // 4. Render Cơ cấu danh mục
      renderCategoryChart(d.categoryDistribution || []);

      const catTbody = document.getElementById('categoryTableBody');
      if (catTbody) {
        if (!d.categoryDistribution || !d.categoryDistribution.length) {
          catTbody.innerHTML = '<tr><td colspan="3" class="text-center text-muted py-3">Chưa có số liệu danh mục.</td></tr>';
        } else {
          catTbody.innerHTML = d.categoryDistribution.map(c => `
            <tr>
              <td class="fw-medium">${escHtml(c.categoryName)}</td>
              <td class="text-end">${formatVnd(c.totalRevenue)}</td>
              <td class="text-end fw-bold text-success">${c.percentage}%</td>
            </tr>
          `).join('');
        }
      }

    } catch (e) {
      console.error('Lỗi nạp thống kê analytics:', e);
    }
  }

  function initFilterEvents() {
    const periodSelect = document.getElementById('filterPeriod');
    const customDateBox = document.getElementById('customDateBox');

    if (periodSelect && customDateBox) {
      periodSelect.addEventListener('change', () => {
        customDateBox.style.display = periodSelect.value === 'custom' ? 'flex' : 'none';
        fetchAnalyticsData();
      });
    }

    ['filterType', 'filterChartType'].forEach(id => {
      const el = document.getElementById(id);
      if (el) el.addEventListener('change', () => fetchAnalyticsData());
    });

    const btnApply = document.getElementById('btnApplyCustomDate');
    if (btnApply) {
      btnApply.addEventListener('click', () => fetchAnalyticsData());
    }
  }

  document.addEventListener('DOMContentLoaded', () => {
    initFilterEvents();
    fetchAnalyticsData();
  });
})();
