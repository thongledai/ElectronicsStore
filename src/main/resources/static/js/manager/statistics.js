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

  const statusTextMap = {
    'DELIVERED': 'Đã giao hàng (Thực thu)',
    'NOT_PROCESSED': 'Chờ xác nhận',
    'CONFIRMED': 'Đã xác nhận',
    'SHIPPING': 'Đang giao hàng',
    'CANCELLED': 'Đã hủy',
    'RETURNED': 'Trả hàng',
    'ALL': 'Tất cả trạng thái'
  };

  let analyticsChartInstance = null;
  let categoryChartInstance = null;

  function renderAnalyticsChart(chartPoints, chartType, metricType, status) {
    const canvas = document.getElementById('analyticsChart');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    const labels = (chartPoints || []).map(p => p.label);
    const dataValues = (chartPoints || []).map(p => Number(p.value || 0));

    if (analyticsChartInstance) {
      analyticsChartInstance.destroy();
    }

    const isRevenue = (metricType !== 'orders');
    const isDelivered = (status === 'DELIVERED');
    const labelTitle = isRevenue
      ? (isDelivered ? 'Doanh thu (₫)' : `Giá trị đơn (${statusTextMap[status] || status}) (₫)`)
      : 'Số đơn hàng';

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
          pointRadius: chartType === 'line' ? 4 : 0,
          pointHoverRadius: 6
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
                return isRevenue
                  ? ` ${context.dataset.label}: ${formatVnd(context.parsed.y)}`
                  : ` ${context.dataset.label}: ${context.parsed.y.toLocaleString('vi-VN')} đơn`;
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
            grid: { color: 'rgba(0,0,0,0.06)' },
            ticks: {
              font: { size: 11 },
              callback: function (val) {
                if (!isRevenue) return val;
                if (val >= 1000000000) return (val / 1000000000).toFixed(1) + ' tỷ';
                if (val >= 1000000) return (val / 1000000).toFixed(0) + ' tr';
                if (val >= 1000) return (val / 1000).toFixed(0) + ' k';
                return val;
              }
            }
          }
        }
      }
    });
  }

  function renderCategoryDoughnut(categories) {
    const canvas = document.getElementById('categoryChart');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    const labels = (categories || []).map(c => c.categoryName);
    const dataValues = (categories || []).map(c => Number(c.totalRevenue || 0));

    if (categoryChartInstance) {
      categoryChartInstance.destroy();
    }

    const palette = ['#1b6df3', '#12b886', '#f59f00', '#e03131', '#845ef7', '#20c997', '#fd7e14', '#495057'];

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
    const status = document.getElementById('filterOrderStatus')?.value || 'DELIVERED';
    const type = document.getElementById('filterType')?.value || 'revenue';
    const chartType = document.getElementById('filterChartType')?.value || 'bar';
    const startDate = document.getElementById('filterStartDate')?.value || '';
    const endDate = document.getElementById('filterEndDate')?.value || '';

    const params = new URLSearchParams();
    params.set('period', period);
    params.set('type', type);
    params.set('status', status);
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

      // 1. Cập nhật thẻ tóm tắt (Tiêu đề và giá trị linh hoạt theo trạng thái)
      const statLabel = document.getElementById('statPeriodRevenueLabel');
      const statRev = document.getElementById('statPeriodRevenue');
      const statOrders = document.getElementById('statPeriodOrders');
      const statUsers = document.getElementById('statPeriodUsers');
      const statNote = document.getElementById('statPeriodRevenueNote');

      if (statLabel) {
        if (status === 'DELIVERED') {
          statLabel.textContent = 'Doanh thu thực thu';
        } else if (status === 'ALL') {
          statLabel.textContent = 'Tổng giá trị đơn hàng';
        } else {
          statLabel.textContent = 'Giá trị đơn: ' + (statusTextMap[status] || status);
        }
      }

      if (statRev) statRev.textContent = formatVnd(d.totalRevenue);
      if (statOrders) statOrders.textContent = (d.totalOrders || 0).toLocaleString('vi-VN') + ' đơn';
      if (statUsers) statUsers.textContent = (d.newUsers || 0).toLocaleString('vi-VN');

      if (statNote) {
        if (status === 'DELIVERED') {
          statNote.textContent = 'Đơn hàng giao thành công';
        } else if (status === 'ALL') {
          statNote.textContent = 'Tất cả trạng thái (trừ đơn hủy)';
        } else {
          statNote.textContent = 'Tổng tiền các đơn ' + (statusTextMap[status] || status);
        }
      }

      // 2. Tiêu đề biểu đồ
      const chartTitle = document.getElementById('mainChartTitle');
      const chartSubtitle = document.getElementById('chartSubtitle');
      const isRev = (type !== 'orders');

      if (chartTitle) {
        if (isRev) {
          chartTitle.textContent = (status === 'DELIVERED')
            ? 'Xu hướng Doanh thu (Đã giao thành công)'
            : `Xu hướng Giá trị đơn hàng (${statusTextMap[status] || status})`;
        } else {
          chartTitle.textContent = `Xu hướng Số lượng đơn hàng (${statusTextMap[status] || status})`;
        }
      }
      if (chartSubtitle) {
        chartSubtitle.textContent = `Lọc: ${statusTextMap[status] || status} · ${period === 'this_year' ? '12 tháng trong năm' : 'Theo ngày'}`;
      }

      // 3. Render biểu đồ chính với trạng thái tương ứng
      renderAnalyticsChart(d.chartData, chartType, type, status);

      // 4. Render Top 5 sản phẩm bán chạy (Cố định đơn DELIVERED)
      const topTbody = document.getElementById('topProductsBody');
      if (topTbody) {
        if (!d.topProducts || d.topProducts.length === 0) {
          topTbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-4">Không có dữ liệu trong khoảng thời gian này.</td></tr>';
        } else {
          topTbody.innerHTML = d.topProducts.map(p => `
            <tr>
              <td>
                <img src="${p.thumbnailUrl || '/images/placeholder.webp'}"
                     alt="${escHtml(p.productName)}"
                     style="width: 40px; height: 40px; object-fit: cover; border-radius: 6px; border: 1px solid #e9ecef;"
                     onerror="this.src='/images/placeholder.webp'">
              </td>
              <td><span class="fw-medium">${escHtml(p.productName)}</span></td>
              <td class="text-center font-monospace">${(p.totalSold || 0).toLocaleString('vi-VN')}</td>
              <td class="text-end fw-bold text-primary">${formatVnd(p.totalRevenue)}</td>
            </tr>
          `).join('');
        }
      }

      // 5. Render Cơ cấu danh mục
      renderCategoryDoughnut(d.categoryDistribution);
      const catTbody = document.getElementById('categoryTableBody');
      if (catTbody) {
        if (!d.categoryDistribution || d.categoryDistribution.length === 0) {
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

    ['filterType', 'filterChartType', 'filterOrderStatus'].forEach(id => {
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
