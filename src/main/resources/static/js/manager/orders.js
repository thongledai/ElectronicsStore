/**
 * Manager Orders Management - Clean, Minimalist & Printable
 */

(function () {
  'use strict';

  let currentPage = 0;
  const pageSize = 10;
  let currentOrderId = null;
  let currentOrderData = null;

  document.addEventListener('DOMContentLoaded', () => {
    initFilters();
    initPagination();
    initModalActions();
    loadOrders(0);
  });

  function getAuthHeaders() {
    const token = (window.STORE && typeof window.STORE.getToken === 'function')
      ? window.STORE.getToken()
      : localStorage.getItem('technova_jwt');
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    return headers;
  }

  function formatCurrency(val) {
    if (val === null || val === undefined) return '₫0';
    return '₫' + Number(val).toLocaleString('vi-VN');
  }

  function formatDateTime(iso) {
    if (!iso) return '--';
    const d = new Date(iso);
    if (isNaN(d.getTime())) return iso;
    const pad = (n) => String(n).padStart(2, '0');
    return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  function renderStatusPill(status) {
    const s = (status || '').toUpperCase();
    switch (s) {
      case 'NOT_PROCESSED':
        return '<span class="status blue">Chờ xác nhận</span>';
      case 'CONFIRMED':
        return '<span class="status yellow">Đã xác nhận</span>';
      case 'SHIPPING':
        return '<span class="status purple">Đang giao</span>';
      case 'DELIVERED':
        return '<span class="status green">Đã giao</span>';
      case 'CANCELLED':
        return '<span class="status red">Đã hủy</span>';
      case 'RETURNED':
        return '<span class="status orange">Trả hàng</span>';
      default:
        return `<span class="status">${status || 'Chưa rõ'}</span>`;
    }
  }

  function initFilters() {
    const keywordInput = document.getElementById('filterKeyword');
    const statusSelect = document.getElementById('filterStatus');

    let debounceTimer;
    if (keywordInput) {
      keywordInput.addEventListener('input', () => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => loadOrders(0), 400);
      });
    }

    if (statusSelect) {
      statusSelect.addEventListener('change', () => loadOrders(0));
    }
  }

  function initPagination() {
    const btnPrev = document.getElementById('btnPrevPage');
    const btnNext = document.getElementById('btnNextPage');

    if (btnPrev) {
      btnPrev.addEventListener('click', () => {
        if (currentPage > 0) loadOrders(currentPage - 1);
      });
    }
    if (btnNext) {
      btnNext.addEventListener('click', () => {
        loadOrders(currentPage + 1);
      });
    }
  }

  function initModalActions() {
    const btnSave = document.getElementById('btnSaveStatus');
    if (btnSave) {
      btnSave.addEventListener('click', saveStatusUpdate);
    }

    const btnPrint = document.getElementById('btnPrintOrder');
    if (btnPrint) {
      btnPrint.addEventListener('click', () => {
        if (currentOrderData) printOrder(currentOrderData);
      });
    }
  }

  async function loadOrders(page) {
    currentPage = page;
    const tbody = document.getElementById('ordersTableBody');
    const totalRecordsText = document.getElementById('totalRecordsText');
    const pageInfoText = document.getElementById('pageInfoText');
    const btnPrev = document.getElementById('btnPrevPage');
    const btnNext = document.getElementById('btnNextPage');
    const btnCurrent = document.getElementById('btnCurrentPage');

    tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted">Đang tải dữ liệu...</td></tr>';

    const keyword = (document.getElementById('filterKeyword')?.value || '').trim();
    const status = document.getElementById('filterStatus')?.value || '';

    const params = new URLSearchParams();
    params.set('page', page);
    params.set('size', pageSize);
    if (keyword) params.set('keyword', keyword);
    if (status) params.set('status', status);

    try {
      const res = await fetch(`/api/manager/orders?${params.toString()}`, {
        headers: getAuthHeaders()
      });

      if (!res.ok) throw new Error(`Lỗi máy chủ (${res.status})`);

      const body = await res.json();
      const pageData = body.data;
      const list = pageData.content || [];

      totalRecordsText.textContent = `Tổng cộng: ${pageData.totalElements} bản ghi`;
      pageInfoText.textContent = `Trang ${pageData.totalPages > 0 ? pageData.page + 1 : 1} / ${pageData.totalPages || 1}`;
      if (btnCurrent) btnCurrent.textContent = (pageData.page + 1) || 1;

      if (btnPrev) btnPrev.disabled = pageData.first;
      if (btnNext) btnNext.disabled = pageData.last;

      if (list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted">Không có đơn hàng nào.</td></tr>';
        return;
      }

      tbody.innerHTML = list.map(item => `
        <tr>
          <td><strong>${item.shortCode}</strong></td>
          <td>
            <div>${escapeHtml(item.customerName)}</div>
            <div class="muted small">${item.customerPhone || ''}</div>
          </td>
          <td>${formatDateTime(item.createdAt)}</td>
          <td><strong>${formatCurrency(item.total)}</strong></td>
          <td>${renderStatusPill(item.statusName)}</td>
          <td class="text-end">
            <button class="action btn-view" data-id="${item.id}" title="Xem chi tiết">
              <i class="fa-regular fa-eye"></i>
            </button>
          </td>
        </tr>
      `).join('');

      document.querySelectorAll('.btn-view').forEach(btn => {
        btn.addEventListener('click', (e) => {
          const id = e.currentTarget.getAttribute('data-id');
          if (id) openOrderDetail(id);
        });
      });

    } catch (err) {
      console.error(err);
      tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-danger">Không thể tải dữ liệu: ${err.message}</td></tr>`;
    }
  }

  async function openOrderDetail(orderId) {
    try {
      const res = await fetch(`/api/manager/orders/${orderId}`, {
        headers: getAuthHeaders()
      });

      if (!res.ok) throw new Error('Không thể tải chi tiết đơn hàng');

      const body = await res.json();
      const order = body.data;
      currentOrderId = order.id;
      currentOrderData = order;

      document.getElementById('modalShortCode').textContent = order.shortCode;
      document.getElementById('modalCustomerName').textContent = order.toName || order.customerName || '--';
      document.getElementById('modalCustomerPhone').textContent = order.phone || order.customerPhone || '--';
      document.getElementById('modalCreatedAt').textContent = formatDateTime(order.createdAt);
      document.getElementById('modalPayment').textContent = (order.paymentMethodName || 'COD') + (order.isPaidBefore ? ' (Đã thanh toán)' : ' (Chưa thanh toán)');
      document.getElementById('modalAddress').textContent = order.fullAddress || '--';

      const noteEl = document.getElementById('modalNote');
      const noteContainer = document.getElementById('modalNoteContainer');
      if (order.note) {
        noteEl.textContent = order.note;
        noteContainer.style.display = '';
      } else {
        noteContainer.style.display = 'none';
      }

      const itemsBody = document.getElementById('modalItemsBody');
      itemsBody.innerHTML = (order.items || []).map(item => `
        <tr>
          <td>
            <div class="fw-medium">${escapeHtml(item.productName)}</div>
            ${item.variantDescription ? `<div class="muted small">${escapeHtml(item.variantDescription)}</div>` : ''}
          </td>
          <td class="text-end">${formatCurrency(item.unitPrice)}</td>
          <td class="text-center">${item.quantity}</td>
          <td class="text-end fw-semibold">${formatCurrency(item.subtotal)}</td>
        </tr>
      `).join('');

      document.getElementById('modalSubtotal').textContent = formatCurrency(order.subtotal);
      document.getElementById('modalShipping').textContent = formatCurrency(order.shippingFee);
      document.getElementById('modalDiscount').textContent = '-' + formatCurrency(order.discount);
      document.getElementById('modalTotal').textContent = formatCurrency(order.total);

      const statusSelect = document.getElementById('modalStatusSelect');
      if (statusSelect) statusSelect.value = order.statusName;
      document.getElementById('modalStatusNote').value = '';

      const modalEl = document.getElementById('orderDetailModal');
      bootstrap.Modal.getOrCreateInstance(modalEl).show();

    } catch (err) {
      alert('Lỗi: ' + err.message);
    }
  }

  async function saveStatusUpdate() {
    if (!currentOrderId) return;

    const newStatus = document.getElementById('modalStatusSelect').value;
    const note = document.getElementById('modalStatusNote').value.trim();

    if (currentOrderData && newStatus === currentOrderData.statusName && !note) {
      alert('Trạng thái không thay đổi');
      return;
    }

    if (newStatus === 'CANCELLED') {
      if (!confirm('Xác nhận hủy đơn hàng này? Số lượng tồn kho sẽ được hoàn trả tự động.')) {
        return;
      }
    }

    try {
      const res = await fetch(`/api/manager/orders/${currentOrderId}/status`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify({ newStatus, note })
      });

      if (!res.ok) {
        const err = await res.json();
        throw new Error(err.message || 'Cập nhật thất bại');
      }

      alert('Đã cập nhật trạng thái đơn hàng thành công');
      const modalEl = document.getElementById('orderDetailModal');
      bootstrap.Modal.getOrCreateInstance(modalEl).hide();

      loadOrders(currentPage);
    } catch (err) {
      alert('Lỗi: ' + err.message);
    }
  }

  function printOrder(order) {
    if (!order) return;

    document.getElementById('printOrderCode').textContent = order.shortCode;
    document.getElementById('printOrderDate').textContent = 'Ngày đặt: ' + formatDateTime(order.createdAt);
    document.getElementById('printReceiverName').textContent = order.toName || order.customerName || '--';
    document.getElementById('printReceiverPhone').textContent = 'SĐT: ' + (order.phone || order.customerPhone || '--');
    document.getElementById('printReceiverAddress').textContent = 'Địa chỉ: ' + (order.fullAddress || '--');
    document.getElementById('printNote').textContent = order.note ? `Ghi chú: ${order.note}` : '';

    document.getElementById('printPaymentMethod').textContent = order.paymentMethodName || 'COD';
    document.getElementById('printGhnCode').textContent = order.ghnOrderCode || 'Chưa có';
    document.getElementById('printStatus').textContent = order.statusName || '--';

    document.getElementById('printSubtotal').textContent = formatCurrency(order.subtotal);
    document.getElementById('printShipping').textContent = formatCurrency(order.shippingFee);
    document.getElementById('printDiscount').textContent = formatCurrency(order.discount);
    document.getElementById('printTotal').textContent = formatCurrency(order.total);

    const printItemsBody = document.getElementById('printItemsTableBody');
    printItemsBody.innerHTML = (order.items || []).map((item, idx) => `
      <tr>
        <td style="padding: 8px; border: 1px solid #ddd; text-align: center;">${idx + 1}</td>
        <td style="padding: 8px; border: 1px solid #ddd;">
          <strong>${escapeHtml(item.productName)}</strong>
          <div style="font-size: 11px; color: #666;">${escapeHtml(item.variantDescription || '')}</div>
        </td>
        <td style="padding: 8px; border: 1px solid #ddd; text-align: right;">${formatCurrency(item.unitPrice)}</td>
        <td style="padding: 8px; border: 1px solid #ddd; text-align: center;">${item.quantity}</td>
        <td style="padding: 8px; border: 1px solid #ddd; text-align: right;">${formatCurrency(item.subtotal)}</td>
      </tr>
    `).join('');

    const printAreaContent = document.getElementById('printArea').innerHTML;
    const win = window.open('', '_blank', 'width=900,height=650');
    win.document.open();
    win.document.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>Phiếu Giao Đơn Hàng - ${order.shortCode}</title>
          <style>
            @page { margin: 15mm; size: auto; }
            body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; margin: 0; padding: 20px; }
          </style>
        </head>
        <body onload="window.print(); window.close();">
          ${printAreaContent}
        </body>
      </html>
    `);
    win.document.close();
  }

  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

})();
