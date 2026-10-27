/* ==========================================================================
   UTE Shop — Manager Dashboard (Tổng quan)
   ========================================================================== */

'use strict';

(function () {
  const token = localStorage.getItem('technova_jwt');

  function formatVnd(value) {
    const num = Number(value || 0);
    return num.toLocaleString('vi-VN') + ' ₫';
  }

  function formatCompactVnd(value) {
    const num = Number(value || 0);
    if (num >= 1_000_000_000) {
      return '₫' + (num / 1_000_000_000).toFixed(1).replace('.', ',') + 'B';
    }
    if (num >= 1_000_000) {
      return '₫' + (num / 1_000_000).toFixed(1).replace('.', ',') + 'M';
    }
    if (num >= 1_000) {
      return '₫' + (num / 1_000).toFixed(1).replace('.', ',') + 'K';
    }
    return formatVnd(num);
  }

  let revenueChartInstance = null;

  function renderRevenueChart(chartPoints) {
    const canvas = document.getElementById('revenueChart');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    const labels = (chartPoints || []).map(p => p.label);
    const dataValues = (chartPoints || []).map(p => Number(p.value || 0));

    if (revenueChartInstance) {
      revenueChartInstance.destroy();
    }

    // Gradient background
    const gradient = ctx.createLinearGradient(0, 0, 0, 260);
    gradient.addColorStop(0, 'rgba(27, 109, 243, 0.35)');
    gradient.addColorStop(1, 'rgba(27, 109, 243, 0.0)');

    revenueChartInstance = new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: 'Doanh thu (₫)',
          data: dataValues,
          borderColor: '#1b6df3',
          backgroundColor: gradient,
          borderWidth: 2.5,
          fill: true,
          tension: 0.35,
          pointBackgroundColor: '#1b6df3',
          pointBorderColor: '#ffffff',
          pointBorderWidth: 2,
          pointRadius: 4,
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
                return ' ' + formatVnd(context.parsed.y);
              }
            }
          }
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: { font: { size: 12 } }
          },
          y: {
            beginAtZero: true,
            grid: { color: 'rgba(0, 0, 0, 0.05)' },
            ticks: {
              font: { size: 11 },
              callback: function (val) {
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

  async function loadDashboardSummary() {
    try {
      const headers = { 'Accept': 'application/json' };
      if (token) headers['Authorization'] = 'Bearer ' + token;

      const res = await fetch('/api/manager/statistics/summary', { headers }).then(r => r.json());
      if (!res || !res.success || !res.data) {
        console.warn('Không thể tải dữ liệu dashboard:', res);
        return;
      }

      const d = res.data;

      // 1. Cập nhật 4 thẻ KPI
      const kpiRev = document.getElementById('kpiRevenue');
      if (kpiRev) {
        kpiRev.textContent = formatCompactVnd(d.totalRevenue);
        kpiRev.title = formatVnd(d.totalRevenue);
      }

      const kpiRevGrowth = document.getElementById('kpiRevenueGrowth');
      if (kpiRevGrowth) {
        const rate = Number(d.revenueGrowthRate || 0);
        if (rate >= 0) {
          kpiRevGrowth.innerHTML = `<span class="text-success"><i class="fa-solid fa-arrow-up me-1"></i>Tăng ${rate}%</span> so với tháng trước`;
        } else {
          kpiRevGrowth.innerHTML = `<span class="text-danger"><i class="fa-solid fa-arrow-down me-1"></i>Giảm ${Math.abs(rate)}%</span> so với tháng trước`;
        }
      }

      const kpiOrders = document.getElementById('kpiOrders');
      if (kpiOrders) kpiOrders.textContent = Number(d.totalOrders || 0).toLocaleString('vi-VN');

      const kpiPending = document.getElementById('kpiPendingOrders');
      if (kpiPending) kpiPending.textContent = `${Number(d.pendingOrders || 0).toLocaleString('vi-VN')} đơn đang xử lý`;

      const kpiUsers = document.getElementById('kpiUsers');
      if (kpiUsers) kpiUsers.textContent = Number(d.totalUsers || 0).toLocaleString('vi-VN');

      const kpiNewUsers = document.getElementById('kpiNewUsers');
      if (kpiNewUsers) kpiNewUsers.textContent = `${Number(d.newUsersThisMonth || 0).toLocaleString('vi-VN')} người dùng mới tháng này`;

      const kpiProducts = document.getElementById('kpiProducts');
      if (kpiProducts) kpiProducts.textContent = Number(d.totalProducts || 0).toLocaleString('vi-VN');

      const kpiLowStock = document.getElementById('kpiLowStock');
      if (kpiLowStock) {
        const low = Number(d.lowStockProducts || 0);
        if (low > 0) {
          kpiLowStock.innerHTML = `<span class="text-warning fw-semibold">${low} sản phẩm</span> sắp hết hàng`;
        } else {
          kpiLowStock.textContent = 'Kho hàng ổn định';
        }
      }

      // 2. Cập nhật trạng thái đơn hàng
      const statusMap = d.orderStatusCounts || {};
      const countNP = document.getElementById('countNotProcessed');
      if (countNP) countNP.textContent = (statusMap.NOT_PROCESSED || 0).toLocaleString('vi-VN');

      const countConf = document.getElementById('countConfirmed');
      if (countConf) countConf.textContent = (statusMap.CONFIRMED || 0).toLocaleString('vi-VN');

      const countShip = document.getElementById('countShipping');
      if (countShip) countShip.textContent = (statusMap.SHIPPING || 0).toLocaleString('vi-VN');

      const countDelivered = document.getElementById('countDelivered');
      if (countDelivered) countDelivered.textContent = (statusMap.DELIVERED || 0).toLocaleString('vi-VN');

      const countCancelReturn = document.getElementById('countCancelReturn');
      if (countCancelReturn) {
        const cancelTotal = (statusMap.CANCELLED || 0) + (statusMap.RETURNED || 0);
        countCancelReturn.textContent = cancelTotal.toLocaleString('vi-VN');
      }

      // 3. Vẽ biểu đồ 7 ngày
      renderRevenueChart(d.last7DaysRevenue || []);

    } catch (e) {
      console.error('Lỗi khi nạp dữ liệu dashboard:', e);
    }
  }

  document.addEventListener('DOMContentLoaded', () => {
    loadDashboardSummary();
  });
})();
