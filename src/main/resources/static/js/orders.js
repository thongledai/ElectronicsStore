/**
 * orders.js - Xử lý hiển thị, lọc và hủy đơn hàng cho khách hàng
 */
document.addEventListener('DOMContentLoaded', () => {
  let currentStatus = 'ALL';
  let allOrders = [];
  let currentCancellingOrderId = null;
  let orderDetailModalInstance = null;
  let cancelOrderModalInstance = null;

  // DOM Elements
  const tabsContainer = document.getElementById('orderStatusTabs');
  const loadingEl = document.getElementById('ordersLoading');
  const containerEl = document.getElementById('ordersContainer');
  const emptyEl = document.getElementById('ordersEmpty');
  const emptyMsgEl = document.getElementById('ordersEmptyMsg');
  const countSummaryEl = document.getElementById('orderCountSummary');

  // Modals
  const orderDetailModalEl = document.getElementById('orderDetailModal');
  if (orderDetailModalEl && window.bootstrap) {
    orderDetailModalInstance = new bootstrap.Modal(orderDetailModalEl);
  }
  const cancelOrderModalEl = document.getElementById('cancelOrderModal');
  if (cancelOrderModalEl && window.bootstrap) {
    cancelOrderModalInstance = new bootstrap.Modal(cancelOrderModalEl);
  }

  const cancelReasonSelect = document.getElementById('cancelReasonSelect');
  const customReasonArea = document.getElementById('customReasonArea');
  const cancelCustomReason = document.getElementById('cancelCustomReason');
  const confirmCancelOrderBtn = document.getElementById('confirmCancelOrderBtn');

  // Format currency VND
  function formatMoney(amount) {
    if (amount === null || amount === undefined) return '0 đ';
    return Number(amount).toLocaleString('vi-VN') + ' đ';
  }

  // Format date time
  function formatDateTime(dateStr) {
    if (!dateStr) return '';
    try {
      const d = new Date(dateStr);
      if (isNaN(d.getTime())) return dateStr;
      const day = String(d.getDate()).padStart(2, '0');
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const year = d.getFullYear();
      const hours = String(d.getHours()).padStart(2, '0');
      const minutes = String(d.getMinutes()).padStart(2, '0');
      return `${hours}:${minutes} ${day}/${month}/${year}`;
    } catch {
      return dateStr;
    }
  }

  // Badge class & icon mapping
  function getStatusBadge(status, displayName) {
    let badgeClass = 'bg-secondary text-white';
    let icon = 'fa-circle-info';

    switch ((status || '').toUpperCase()) {
      case 'NOT_PROCESSED':
        badgeClass = 'bg-warning text-dark';
        icon = 'fa-clock';
        break;
      case 'CONFIRMED':
      case 'SHIPPING':
        badgeClass = 'bg-primary text-white';
        icon = 'fa-truck-fast';
        break;
      case 'DELIVERED':
        badgeClass = 'bg-success text-white';
        icon = 'fa-box-archive';
        break;
      case 'CANCELLED':
        badgeClass = 'bg-danger text-white';
        icon = 'fa-ban';
        break;
      case 'RETURNED':
        badgeClass = 'bg-secondary text-white';
        icon = 'fa-rotate-left';
        break;
    }

    return `<span class="badge ${badgeClass} px-2 py-1 fs-7">
      <i class="fas ${icon} me-1"></i>${escapeHtml(displayName || status)}
    </span>`;
  }

  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  // Lọc an toàn 2 lớp tại client: đảm bảo trạng thái hiển thị trùng khớp 100% với tab đang chọn
  function filterOrdersByStatus(orders, status) {
    if (!orders || !Array.isArray(orders)) return [];
    if (!status || status === 'ALL') return orders;

    const target = status.trim().toUpperCase();
    if (target === 'SHIPPING') {
      return orders.filter(o => {
        const s = (o.status || '').toUpperCase();
        return s === 'SHIPPING' || s === 'CONFIRMED';
      });
    }
    return orders.filter(o => (o.status || '').toUpperCase() === target);
  }

  // 1. Tải danh sách đơn hàng từ Server
  async function loadOrders(status = 'ALL') {
    currentStatus = status;

    // Reset giao diện trước khi tải
    loadingEl.classList.remove('d-none');
    loadingEl.style.setProperty('display', 'block', 'important');

    containerEl.innerHTML = '';
    containerEl.classList.add('d-none');
    containerEl.style.setProperty('display', 'none', 'important');

    emptyEl.classList.add('d-none');
    emptyEl.style.setProperty('display', 'none', 'important');

    if (countSummaryEl) countSummaryEl.textContent = 'Đang tải...';

    try {
      const timestamp = Date.now();
      const url = status && status !== 'ALL'
        ? `/orders?status=${encodeURIComponent(status)}&_t=${timestamp}`
        : `/orders?_t=${timestamp}`;

      const response = await fetch(url, {
        cache: 'no-store',
        headers: {
          'Accept': 'application/json'
        }
      });

      if (!response.ok) {
        if (response.status === 401) {
          window.location.href = '/login?error=unauthorized';
          return;
        }
        throw new Error(`Lỗi tải đơn hàng: ${response.status}`);
      }

      const resData = await response.json();
      const rawOrders = resData.data || [];
      // Đảm bảo dữ liệu hiển thị lọc đúng theo tab hiện tại (phòng vệ cả 2 phía client & server)
      allOrders = filterOrdersByStatus(rawOrders, currentStatus);
      renderOrders(allOrders);
    } catch (err) {
      console.error('loadOrders error:', err);
      loadingEl.classList.add('d-none');
      loadingEl.style.setProperty('display', 'none', 'important');

      containerEl.innerHTML = `<div class="alert alert-danger">Không thể tải danh sách đơn hàng. Vui lòng tải lại trang.</div>`;
      containerEl.classList.remove('d-none');
      containerEl.style.setProperty('display', 'block', 'important');

      if (countSummaryEl) countSummaryEl.textContent = 'Lỗi kết nối';
    }
  }

  // 2. Render danh sách đơn hàng
  function renderOrders(orders) {
    loadingEl.classList.add('d-none');
    loadingEl.style.setProperty('display', 'none', 'important');

    // LUÔN XÓA SẠCH NỘI DUNG CŨ
    containerEl.innerHTML = '';

    if (!orders || orders.length === 0) {
      // Ẩn danh sách hoàn toàn
      containerEl.classList.add('d-none');
      containerEl.style.setProperty('display', 'none', 'important');

      // Hiện thông báo trống
      emptyEl.classList.remove('d-none');
      emptyEl.style.setProperty('display', 'block', 'important');

      const tabNames = {
        'ALL': 'Bạn chưa có đơn hàng nào.',
        'NOT_PROCESSED': 'Bạn không có đơn hàng nào đang chờ xử lý.',
        'SHIPPING': 'Bạn không có đơn hàng nào đang được giao.',
        'DELIVERED': 'Bạn không có đơn hàng nào đã hoàn thành.',
        'CANCELLED': 'Bạn không có đơn hàng nào đã hủy.',
        'RETURNED': 'Bạn không có đơn hàng nào đã trả hàng.'
      };
      emptyMsgEl.textContent = tabNames[currentStatus] || 'Chưa có đơn hàng nào.';
      if (countSummaryEl) countSummaryEl.textContent = '0 đơn hàng';
      return;
    }

    // Ẩn thông báo trống
    emptyEl.classList.add('d-none');
    emptyEl.style.setProperty('display', 'none', 'important');

    // Hiện danh sách
    containerEl.classList.remove('d-none');
    containerEl.style.setProperty('display', 'flex', 'important');
    if (countSummaryEl) countSummaryEl.textContent = `Hiển thị ${orders.length} đơn hàng`;

    containerEl.innerHTML = orders.map(order => createOrderCardHtml(order)).join('');

    // Gắn sự kiện cho các nút trong từng card
    bindCardEvents();
  }

  // Tạo HTML cho mỗi Order Card
  function createOrderCardHtml(order) {
    const itemsHtml = (order.items || []).map(item => `
      <div class="d-flex align-items-center py-2 border-bottom border-light-subtle">
        <div class="flex-shrink-0 me-3">
          <img src="${escapeHtml(item.imageUrl || '/images/products/placeholder.jpg')}" 
               alt="${escapeHtml(item.productName)}" 
               class="rounded border object-fit-cover" 
               style="width: 64px; height: 64px;"
               onerror="this.src='/images/products/placeholder.jpg'">
        </div>
        <div class="flex-grow-1 min-w-0">
          <div class="fw-semibold text-dark text-truncate">${escapeHtml(item.productName)}</div>
          ${item.variantDescription ? `<div class="small text-muted text-truncate">${escapeHtml(item.variantDescription)}</div>` : ''}
          <div class="small text-muted mt-1">Số lượng: <span class="fw-bold">${item.quantity}</span></div>
        </div>
        <div class="text-end flex-shrink-0 ms-3">
          <div class="fw-bold text-dark">${formatMoney(item.subtotal)}</div>
          <div class="small text-muted">${formatMoney(item.unitPrice)}/món</div>
        </div>
      </div>
    `).join('');

    const isCancelable = order.canCancel === true || order.status === 'NOT_PROCESSED';

    return `
      <div class="card border-0 shadow-sm rounded-3 overflow-hidden order-card" data-order-id="${order.id}">
        <!-- Header -->
        <div class="card-header bg-white py-3 border-bottom d-flex flex-wrap justify-content-between align-items-center gap-2">
          <div class="d-flex align-items-center gap-2">
            <span class="fw-bold text-primary fs-6">${escapeHtml(order.orderCode || '#ORD')}</span>
            <span class="text-muted small">· ${formatDateTime(order.createdAt)}</span>
          </div>
          <div>
            ${getStatusBadge(order.status, order.statusDisplayName)}
          </div>
        </div>

        <!-- Body: items -->
        <div class="card-body py-2 px-3 px-md-4">
          <div class="order-items-list">
            ${itemsHtml}
          </div>
        </div>

        <!-- Footer: summary & actions -->
        <div class="card-footer bg-light-subtle py-3 px-3 px-md-4 border-top">
          <div class="d-flex flex-wrap justify-content-between align-items-center gap-3">
            <!-- Delivery & Payment Info -->
            <div class="small text-muted">
              <div>
                <i class="fas fa-credit-card me-1 text-primary"></i>${escapeHtml(order.paymentMethod || 'COD')} 
                <span class="badge ${order.isPaid ? 'bg-success-subtle text-success border border-success' : 'bg-warning-subtle text-warning-emphasis border border-warning'} ms-1">
                  ${order.isPaid ? 'Đã thanh toán' : 'Chưa thanh toán'}
                </span>
              </div>
              ${order.ghnOrderCode ? `
                <div class="mt-1">
                  <i class="fas fa-truck-fast me-1 text-info"></i>Mã vận đơn: <span class="fw-bold font-monospace text-dark">${escapeHtml(order.ghnOrderCode)}</span>
                </div>
              ` : ''}
            </div>

            <!-- Total and Action buttons -->
            <div class="d-flex flex-wrap align-items-center gap-2 ms-auto">
              <div class="text-end me-2">
                <span class="small text-muted d-block">Tổng thanh toán:</span>
                <span class="fw-bold text-danger fs-5">${formatMoney(order.total)}</span>
              </div>
              
              <button type="button" class="btn btn-sm btn-outline-secondary js-view-detail-btn" data-order-id="${order.id}">
                <i class="fas fa-eye me-1"></i>Chi tiết
              </button>

              ${isCancelable ? `
                <button type="button" class="btn btn-sm btn-outline-danger js-cancel-order-btn" 
                        data-order-id="${order.id}" 
                        data-order-code="${escapeHtml(order.orderCode || '')}">
                  <i class="fas fa-ban me-1"></i>Hủy đơn
                </button>
              ` : ''}

              <a href="/customer/products" class="btn btn-sm btn-outline-primary">
                <i class="fas fa-bag-shopping me-1"></i>Mua lại
              </a>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  // 3. Gắn sự kiện các nút
  function bindCardEvents() {
    // Nút xem chi tiết
    document.querySelectorAll('.js-view-detail-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const orderId = e.currentTarget.getAttribute('data-order-id');
        openOrderDetail(orderId);
      });
    });

    // Nút hủy đơn hàng
    document.querySelectorAll('.js-cancel-order-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const orderId = e.currentTarget.getAttribute('data-order-id');
        const orderCode = e.currentTarget.getAttribute('data-order-code');
        openCancelModal(orderId, orderCode);
      });
    });
  }

  // 4. Mở Modal Chi tiết đơn hàng
  function openOrderDetail(orderId) {
    const order = allOrders.find(o => String(o.id) === String(orderId));
    if (!order) return;

    document.getElementById('modalOrderCode').textContent = order.orderCode || '#ORD';
    document.getElementById('modalOrderDate').textContent = `Thời gian đặt hàng: ${formatDateTime(order.createdAt)}`;
    document.getElementById('modalOrderStatusBadge').innerHTML = getStatusBadge(order.status, order.statusDisplayName);

    // GHN
    const ghnArea = document.getElementById('modalGhnInfoArea');
    if (order.ghnOrderCode) {
      ghnArea.style.display = 'block';
      document.getElementById('modalGhnCode').textContent = order.ghnOrderCode;
    } else {
      ghnArea.style.display = 'none';
    }

    // Expected delivery
    const expectedArea = document.getElementById('modalExpectedDeliveryArea');
    if (order.expectedDeliveryDate) {
      expectedArea.style.display = 'block';
      document.getElementById('modalExpectedDeliveryDate').textContent = formatDateTime(order.expectedDeliveryDate);
    } else {
      expectedArea.style.display = 'none';
    }

    // Receiver info
    document.getElementById('modalReceiverName').textContent = order.receiverName || 'Khách hàng';
    document.getElementById('modalReceiverPhone').textContent = order.receiverPhone || '';
    document.getElementById('modalReceiverAddress').textContent = order.shippingAddress || '';

    // Note
    const noteArea = document.getElementById('modalNoteArea');
    if (order.note && order.note.trim()) {
      noteArea.style.display = 'block';
      document.getElementById('modalOrderNote').textContent = order.note;
    } else {
      noteArea.style.display = 'none';
    }

    // Payment info
    document.getElementById('modalPaymentMethod').textContent = order.paymentMethod || 'COD';
    document.getElementById('modalPaymentStatus').innerHTML = order.isPaid
      ? '<span class="text-success"><i class="fas fa-check me-1"></i>Đã thanh toán</span>'
      : '<span class="text-warning-emphasis"><i class="fas fa-clock me-1"></i>Thanh toán khi nhận hàng (COD)</span>';

    // Items list
    const itemsContainer = document.getElementById('modalOrderItemsList');
    itemsContainer.innerHTML = (order.items || []).map(item => `
      <div class="list-group-item d-flex align-items-center py-2 px-3">
        <img src="${escapeHtml(item.imageUrl || '/images/products/placeholder.jpg')}" 
             alt="${escapeHtml(item.productName)}" 
             class="rounded border me-3 object-fit-cover" 
             style="width: 52px; height: 52px;"
             onerror="this.src='/images/products/placeholder.jpg'">
        <div class="flex-grow-1 min-w-0">
          <div class="fw-semibold text-truncate">${escapeHtml(item.productName)}</div>
          ${item.variantDescription ? `<div class="small text-muted text-truncate">${escapeHtml(item.variantDescription)}</div>` : ''}
          <div class="small text-muted">Số lượng: ${item.quantity} x ${formatMoney(item.unitPrice)}</div>
        </div>
        <div class="fw-bold text-dark text-end ms-2">${formatMoney(item.subtotal)}</div>
      </div>
    `).join('');

    // Money breakdown
    document.getElementById('modalSubtotal').textContent = formatMoney(order.subtotal);
    const discountRow = document.getElementById('modalDiscountRow');
    if (order.discount && Number(order.discount) > 0) {
      discountRow.style.display = 'flex';
      document.getElementById('modalDiscount').textContent = `−${formatMoney(order.discount)}`;
    } else {
      discountRow.style.display = 'none';
    }
    document.getElementById('modalShippingFee').textContent = formatMoney(order.shippingFee);
    document.getElementById('modalTotal').textContent = formatMoney(order.total);

    // Cancel button in modal
    const modalCancelBtn = document.getElementById('modalCancelBtn');
    if (order.canCancel === true || order.status === 'NOT_PROCESSED') {
      modalCancelBtn.style.display = 'inline-block';
      modalCancelBtn.onclick = () => {
        if (orderDetailModalInstance) orderDetailModalInstance.hide();
        openCancelModal(order.id, order.orderCode);
      };
    } else {
      modalCancelBtn.style.display = 'none';
    }

    if (orderDetailModalInstance) {
      orderDetailModalInstance.show();
    }
  }

  // 5. Mở Modal Xác nhận hủy đơn
  function openCancelModal(orderId, orderCode) {
    currentCancellingOrderId = orderId;
    document.getElementById('cancelModalOrderCode').textContent = orderCode || '#ORD';
    cancelReasonSelect.value = 'Tôi muốn thay đổi địa chỉ hoặc số điện thoại nhận hàng';
    customReasonArea.style.display = 'none';
    cancelCustomReason.value = '';

    if (cancelOrderModalInstance) {
      cancelOrderModalInstance.show();
    }
  }

  // Đổi lựa chọn lý do hủy
  if (cancelReasonSelect) {
    cancelReasonSelect.addEventListener('change', () => {
      if (cancelReasonSelect.value === 'Khác') {
        customReasonArea.style.display = 'block';
        cancelCustomReason.focus();
      } else {
        customReasonArea.style.display = 'none';
      }
    });
  }

  // Xác nhận hủy đơn
  if (confirmCancelOrderBtn) {
    confirmCancelOrderBtn.addEventListener('click', async () => {
      if (!currentCancellingOrderId) return;

      let reason = cancelReasonSelect.value;
      if (reason === 'Khác') {
        reason = cancelCustomReason.value.trim() || 'Lý do khác';
      }

      confirmCancelOrderBtn.disabled = true;
      confirmCancelOrderBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang xử lý...';

      try {
        const response = await fetch(`/orders/${currentCancellingOrderId}/cancel`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          },
          body: JSON.stringify({ reason: reason })
        });

        const resData = await response.json();

        if (response.ok && resData.success !== false) {
          if (cancelOrderModalInstance) cancelOrderModalInstance.hide();
          if (typeof showToast === 'function') {
            showToast('Đã hủy đơn hàng', 'Đơn hàng của bạn đã được hủy thành công', 'success');
          } else {
            alert('Hủy đơn hàng thành công!');
          }
          // Tải lại danh sách đơn
          loadOrders(currentStatus);
        } else {
          const msg = resData.message || 'Không thể hủy đơn hàng này.';
          if (typeof showToast === 'function') {
            showToast('Lỗi hủy đơn', msg, 'danger');
          } else {
            alert(msg);
          }
        }
      } catch (err) {
        console.error('Cancel order error:', err);
        if (typeof showToast === 'function') {
          showToast('Lỗi kết nối', 'Không thể gửi yêu cầu hủy đơn', 'danger');
        } else {
          alert('Không thể kết nối đến máy chủ.');
        }
      } finally {
        confirmCancelOrderBtn.disabled = false;
        confirmCancelOrderBtn.innerHTML = '<i class="fas fa-ban me-1"></i>Xác nhận hủy đơn';
      }
    });
  }

  // 6. Xử lý click chuyển Tabs trạng thái
  if (tabsContainer) {
    tabsContainer.querySelectorAll('button[data-status]').forEach(tabBtn => {
      tabBtn.addEventListener('click', (e) => {
        tabsContainer.querySelectorAll('button[data-status]').forEach(b => b.classList.remove('active'));
        e.currentTarget.classList.add('active');
        const targetStatus = e.currentTarget.getAttribute('data-status');
        loadOrders(targetStatus);
      });
    });
  }

  // Khởi động: Tải đơn hàng ban đầu
  loadOrders('ALL');
});
