(() => {
  'use strict';

  const notice = document.getElementById('orderNotice');

  const ORDER_STATES = {
    NOT_PROCESSED: { label: 'Chờ xác nhận', className: 'status-waiting' },
    CONFIRMED: { label: 'Đã xác nhận', className: 'status-confirmed' },
    SHIPPING: { label: 'Đang giao hàng', className: 'status-shipping' },
    DELIVERED: { label: 'Đã giao hàng', className: 'status-delivered' },
    CANCELLED: { label: 'Đã hủy', className: 'status-cancelled' },
    RETURNED: { label: 'Đã hoàn hàng', className: 'status-returned' }
  };

  const GHN_STATES = {
    created: 'Đã tạo vận đơn',
    ready_to_pick: 'Chờ lấy hàng',
    picking: 'Đang lấy hàng',
    money_collect_picking: 'Đang lấy hàng',
    picked: 'Đã lấy hàng',
    storing: 'Đang ở kho',
    sorting: 'Đang phân loại',
    transporting: 'Đang trung chuyển',
    delivering: 'Đang giao hàng',
    money_collect_delivering: 'Đang giao hàng',
    delivered: 'Giao hàng thành công',
    delivery_fail: 'Giao hàng chưa thành công',
    waiting_to_return: 'Chờ hoàn hàng',
    return: 'Đang hoàn hàng',
    return_transporting: 'Đang hoàn hàng',
    return_sorting: 'Đang phân loại hàng hoàn',
    returning: 'Đang hoàn hàng',
    return_fail: 'Hoàn hàng chưa thành công',
    returned: 'Đã hoàn hàng',
    cancel: 'Đã hủy vận đơn',
    exception: 'Đơn hàng cần xử lý',
    damage: 'Hàng hóa bị hư hỏng',
    lost: 'Đơn hàng thất lạc',
    partial_return: 'Đã hoàn một phần',
    ready_to_return: 'Chờ hoàn hàng'
  };

  const STATUS_CLASSES = [
    'status-waiting', 'status-confirmed', 'status-shipping',
    'status-delivered', 'status-cancelled', 'status-returned',
    'status-neutral'
  ];

  const showNotice = (message, success) => {
    if (!notice) return;
    notice.className = 'alert delivery-notice ' + (success ? 'alert-success' : 'alert-danger');
    notice.textContent = message;
    notice.classList.remove('d-none');
    notice.setAttribute('role', 'status');
    notice.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  };

  const hideNotice = () => {
    if (!notice) return;
    notice.classList.add('d-none');
    notice.textContent = '';
  };

  const setBusy = (button, busy, busyText) => {
    if (!button) return;
    if (busy) {
      if (!button.dataset.originalText) button.dataset.originalText = button.textContent.trim();
      button.disabled = true;
      button.textContent = busyText;
      button.setAttribute('aria-busy', 'true');
    } else {
      button.disabled = false;
      button.textContent = button.dataset.originalText || button.textContent;
      button.removeAttribute('aria-busy');
    }
  };

  const readResponse = async response => {
    const result = await response.json().catch(() => null);

    if (response.status === 401) {
      throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
    }
    if (response.status === 403) {
      throw new Error('Tài khoản không có quyền thực hiện thao tác này.');
    }
    if (!response.ok || !result || result.success !== true) {
      throw new Error((result && result.message) || ('Không thể hoàn thành yêu cầu (HTTP ' + response.status + ').'));
    }
    return result;
  };

  const getJson = async url => {
    const response = await fetch(url, {
      method: 'GET',
      credentials: 'same-origin',
      headers: { Accept: 'application/json' },
      cache: 'no-store'
    });
    return readResponse(response);
  };

  const postJson = async (url, body) => {
    const response = await fetch(url, {
      method: 'POST',
      credentials: 'same-origin',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(body)
    });
    return readResponse(response);
  };

  const setStatus = (element, label, className) => {
    if (!element) return;
    element.textContent = label;
    element.classList.remove(...STATUS_CLASSES);
    element.classList.add(className || 'status-neutral');
  };

  const normalizeOrderStatus = value => String(value || '').trim().toUpperCase();

  const applyOrderStatus = element => {
    const status = normalizeOrderStatus(element.dataset.orderStatus);
    const mapped = ORDER_STATES[status];
    setStatus(element, mapped ? mapped.label : 'Đang cập nhật', mapped ? mapped.className : 'status-neutral');
  };

  const normalizeGhnStatus = value => String(value || '').trim().toLowerCase();

  const applyGhnStatus = element => {
    const status = normalizeGhnStatus(element.dataset.ghnStatus);
    const label = status ? (GHN_STATES[status] || 'Đang cập nhật') : 'Chưa có thông tin';
    let className = 'status-neutral';
    if (['delivered'].includes(status)) className = 'status-delivered';
    else if (['cancel', 'returned', 'return_fail', 'delivery_fail', 'lost', 'damage', 'exception'].includes(status)) className = 'status-cancelled';
    else if (['picked', 'storing', 'sorting', 'transporting', 'delivering', 'money_collect_delivering', 'return', 'return_transporting', 'return_sorting', 'returning'].includes(status)) className = 'status-shipping';
    else if (['created', 'ready_to_pick', 'picking', 'money_collect_picking'].includes(status)) className = 'status-confirmed';
    setStatus(element, label, className);
  };

  const formatDateTime = value => {
    if (!value) return '';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return new Intl.DateTimeFormat('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    }).format(date);
  };

  const formatCurrency = value => {
    if (value === null || value === undefined || value === '') return 'Chưa có dữ liệu';
    const amount = Number(value);
    if (!Number.isFinite(amount)) return 'Chưa có dữ liệu';
    return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(amount) + ' ₫';
  };

  document.querySelectorAll('[data-order-status]').forEach(applyOrderStatus);
  document.querySelectorAll('[data-ghn-status]').forEach(applyGhnStatus);
  document.querySelectorAll('.ghn-event-time').forEach(element => {
    const formatted = formatDateTime(element.dateTime);
    if (formatted) element.textContent = formatted;
  });

  document.querySelectorAll('[data-copy-value]').forEach(button => {
    button.addEventListener('click', async () => {
      const value = button.dataset.copyValue;
      if (!value) return;
      try {
        if (navigator.clipboard && window.isSecureContext) {
          await navigator.clipboard.writeText(value);
        } else {
          const input = document.createElement('textarea');
          input.value = value;
          input.setAttribute('readonly', '');
          input.style.position = 'fixed';
          input.style.opacity = '0';
          document.body.appendChild(input);
          input.select();
          const copied = document.execCommand('copy');
          input.remove();
          if (!copied) throw new Error('copy-failed');
        }
        const original = button.dataset.originalText || button.textContent.trim();
        button.dataset.originalText = original;
        button.textContent = 'Đã sao chép';
        window.setTimeout(() => { button.textContent = original; }, 1400);
      } catch (_) {
        showNotice('Không thể sao chép tự động. Vui lòng chọn và sao chép mã thủ công.', false);
      }
    });
  });

  const confirmButton = document.getElementById('confirmOrderButton');
  if (confirmButton) {
    confirmButton.addEventListener('click', async () => {
      setBusy(confirmButton, true, 'Đang xác nhận…');
      hideNotice();
      try {
        const result = await (async () => {
          const response = await fetch(confirmButton.dataset.url, {
            method: 'POST',
            credentials: 'same-origin',
            headers: { Accept: 'application/json' }
          });
          return readResponse(response);
        })();
        showNotice(result.message || 'Đã xác nhận đơn hàng.', true);
        window.setTimeout(() => window.location.reload(), 650);
      } catch (error) {
        showNotice(error.message || 'Không thể xác nhận đơn hàng.', false);
        setBusy(confirmButton, false);
      }
    });
  }

  const refreshButton = document.getElementById('refreshShipmentButton');

  const updateShipmentView = shipment => {
    if (!shipment) return false;
    let statusChanged = false;

    const codeElement = document.getElementById('ghnOrderCode');
    if (codeElement && shipment.ghnOrderCode) codeElement.textContent = shipment.ghnOrderCode;

    const statusElement = document.getElementById('ghnStatus');
    if (statusElement) {
      const previousStatus = normalizeGhnStatus(statusElement.dataset.ghnStatus);
      const nextStatus = normalizeGhnStatus(shipment.ghnStatus);
      if (previousStatus && nextStatus && previousStatus !== nextStatus) statusChanged = true;
      statusElement.dataset.ghnStatus = nextStatus;
      applyGhnStatus(statusElement);
    }

    const orderStatusElement = document.getElementById('orderStatus');
    if (orderStatusElement && shipment.orderStatus) {
      const previousOrderStatus = normalizeOrderStatus(orderStatusElement.dataset.orderStatus);
      const nextOrderStatus = normalizeOrderStatus(shipment.orderStatus);
      if (previousOrderStatus && nextOrderStatus && previousOrderStatus !== nextOrderStatus) statusChanged = true;
      orderStatusElement.dataset.orderStatus = nextOrderStatus;
      applyOrderStatus(orderStatusElement);
    }

    const feeElement = document.getElementById('ghnShippingFee');
    if (feeElement) feeElement.textContent = formatCurrency(shipment.ghnShippingFee);

    const expectedElement = document.getElementById('expectedDeliveryTime');
    if (expectedElement) {
      const formatted = formatDateTime(shipment.expectedDeliveryTime);
      expectedElement.textContent = formatted || 'Chưa có dữ liệu';
    }

    const updatedElement = document.getElementById('ghnStatusUpdatedAt');
    if (updatedElement && shipment.ghnStatusUpdatedAt) {
      updatedElement.textContent = 'Cập nhật lúc ' + formatDateTime(shipment.ghnStatusUpdatedAt);
    }

    return statusChanged;
  };

  const refreshShipment = async silent => {
    if (!refreshButton || !refreshButton.dataset.url) return;
    if (!silent) setBusy(refreshButton, true, 'Đang cập nhật…');
    try {
      const result = await getJson(refreshButton.dataset.url);
      const statusChanged = updateShipmentView(result.data);
      if (!silent) showNotice(result.message || 'Đã cập nhật thông tin vận chuyển.', true);
      if (statusChanged) window.setTimeout(() => window.location.reload(), 1200);
    } catch (error) {
      if (!silent) showNotice(error.message || 'Không tải được thông tin vận chuyển.', false);
    } finally {
      if (!silent) setBusy(refreshButton, false);
    }
  };

  if (refreshButton) {
    refreshButton.addEventListener('click', () => refreshShipment(false));
    window.setInterval(() => {
      if (!document.hidden) refreshShipment(true);
    }, 15000);
  }

  const orderListRows = Array.from(document.querySelectorAll('tr[data-shipment-url]'));
  const refreshOrderListStatuses = async () => {
    const rows = orderListRows.filter(row => row.dataset.hasShipment === 'true');
    await Promise.allSettled(rows.map(async row => {
      const result = await getJson(row.dataset.shipmentUrl);
      const shipment = result.data;
      if (!shipment) return;
      const orderStatus = row.querySelector('[data-order-status]');
      if (orderStatus && shipment.orderStatus) {
        orderStatus.dataset.orderStatus = shipment.orderStatus;
        applyOrderStatus(orderStatus);
      }
      const ghnStatus = row.querySelector('[data-ghn-status]');
      if (ghnStatus) {
        ghnStatus.dataset.ghnStatus = shipment.ghnStatus || '';
        applyGhnStatus(ghnStatus);
      }
      const codeElement = row.querySelector('.delivery-ghn-code');
      if (codeElement && shipment.ghnOrderCode) codeElement.textContent = shipment.ghnOrderCode;
    }));
  };

  if (orderListRows.length > 0) {
    window.setInterval(() => {
      if (!document.hidden) refreshOrderListStatuses();
    }, 20000);
  }

  const form = document.getElementById('shipmentForm');
  if (!form) return;

  const picker = document.getElementById('ghnAddressPicker');
  const provinceSelect = document.getElementById('ghnProvince');
  const wardSelect = document.getElementById('ghnWard');
  const addressInput = document.getElementById('ghnAddressDetail');
  const addressMessage = document.getElementById('ghnAddressMessage');
  const createButton = document.getElementById('createShipmentButton');

  let loadingCatalog = false;
  let submittingShipment = false;

  const normalize = value => String(value || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[\u0111\u0110]/g, 'd')
    .toLowerCase()
    .replace(/^(tp|thanh pho)\s+/, '')
    .replace(/[^a-z0-9]+/g, ' ')
    .trim();

  const fillSelect = (select, items, placeholder) => {
    select.replaceChildren(new Option(placeholder, ''));
    items.forEach(item => select.add(new Option(item.name, String(item.id))));
  };

  const setAddressMessage = (message, type) => {
    if (!addressMessage) return;
    const classes = {
      info: 'alert alert-info delivery-notice mt-3 mb-0',
      success: 'alert alert-success delivery-notice mt-3 mb-0',
      warning: 'alert alert-warning delivery-notice mt-3 mb-0',
      danger: 'alert alert-danger delivery-notice mt-3 mb-0'
    };
    addressMessage.className = classes[type] || classes.info;
    addressMessage.textContent = message;
  };

  const getArrayData = async url => {
    const result = await getJson(url);
    if (!Array.isArray(result.data)) throw new Error('Danh mục địa chỉ không đúng định dạng.');
    return result.data;
  };

  const findExactName = (select, candidate) => {
    const expected = normalize(candidate);
    if (!expected) return null;
    return Array.from(select.options).find(option => option.value && normalize(option.textContent) === expected) || null;
  };

  const loadWards = async (provinceId, preferredWard) => {
    wardSelect.disabled = true;
    fillSelect(wardSelect, [], 'Đang tải phường/xã…');
    if (!provinceId) {
      fillSelect(wardSelect, [], 'Vui lòng chọn tỉnh/thành trước');
      return;
    }
    const wards = await getArrayData(picker.dataset.urlWardsPrefix + encodeURIComponent(provinceId) + '/wards');
    fillSelect(wardSelect, wards, 'Chọn phường/xã');
    const matchedWard = findExactName(wardSelect, preferredWard);
    if (matchedWard) wardSelect.value = matchedWard.value;
    wardSelect.disabled = false;
  };

  const loadAddressCatalog = async () => {
    loadingCatalog = true;
    provinceSelect.disabled = true;
    wardSelect.disabled = true;

    try {
      const provinces = await getArrayData(picker.dataset.urlProvinces);
      fillSelect(provinceSelect, provinces, 'Chọn tỉnh/thành phố');
      const matchedProvince = findExactName(provinceSelect, picker.dataset.currentProvince || '');
      if (matchedProvince) {
        provinceSelect.value = matchedProvince.value;
        await loadWards(matchedProvince.value, picker.dataset.currentWard || '');
        const currentWard = picker.dataset.currentWard || '';
        setAddressMessage(
          currentWard && !findExactName(wardSelect, currentWard)
            ? 'Địa chỉ phường/xã cũ không trùng danh mục hiện tại. Hãy chọn phường/xã giao hàng chính xác.'
            : 'Chọn đúng địa chỉ giao hàng, sau đó nhấn Tạo vận đơn.',
          currentWard && !findExactName(wardSelect, currentWard) ? 'warning' : 'info'
        );
      } else {
        fillSelect(wardSelect, [], 'Chọn tỉnh/thành trước');
        setAddressMessage('Chọn tỉnh/thành và phường/xã đúng theo địa chỉ giao hàng.', 'info');
      }
    } catch (error) {
      fillSelect(provinceSelect, [], 'Không tải được danh mục');
      fillSelect(wardSelect, [], 'Không tải được danh mục');
      setAddressMessage(error.message || 'Không tải được danh mục GHN. Vui lòng tải lại trang.', 'danger');
    } finally {
      loadingCatalog = false;
      provinceSelect.disabled = false;
    }
  };

  provinceSelect.addEventListener('change', async () => {
    try {
      await loadWards(provinceSelect.value, '');
      setAddressMessage('Đã tải danh sách phường/xã. Vui lòng chọn địa chỉ nhận hàng.', 'info');
    } catch (error) {
      wardSelect.disabled = true;
      setAddressMessage(error.message || 'Không tải được danh sách phường/xã.', 'danger');
    }
  });

  const buildPayload = () => {
    const values = new FormData(form);
    return {
      provinceId: Number(values.get('provinceId')),
      wardId: Number(values.get('wardId')),
      addressDetail: String(values.get('addressDetail') || '').trim(),
      weight: Number(values.get('weight')),
      length: Number(values.get('length')),
      width: Number(values.get('width')),
      height: Number(values.get('height')),
      paymentTypeId: Number(values.get('paymentTypeId'))
    };
  };

  const validatePayload = payload => {
    if (!payload.provinceId || !payload.wardId) return 'Vui lòng chọn tỉnh/thành và phường/xã.';
    if (!payload.addressDetail) return 'Vui lòng nhập số nhà và tên đường.';
    if (payload.addressDetail.length > 500) return 'Địa chỉ chi tiết tối đa 500 ký tự.';
    if (!Number.isInteger(payload.weight) || payload.weight < 1 || payload.weight > 19999) return 'Khối lượng phải từ 1 đến 19.999 gram.';
    for (const field of ['length', 'width', 'height']) {
      if (!Number.isInteger(payload[field]) || payload[field] < 1 || payload[field] > 200) {
        return 'Kích thước chiều dài, rộng và cao phải từ 1 đến 200 cm.';
      }
    }
    if (![1, 2].includes(payload.paymentTypeId)) return 'Vui lòng chọn bên thanh toán phí vận chuyển.';
    if (!provinceSelect.value || !wardSelect.value) return 'Danh mục địa chỉ chưa sẵn sàng. Vui lòng chọn lại địa chỉ.';
    return null;
  };

  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (loadingCatalog || submittingShipment) return;
    hideNotice();

    if (!form.reportValidity()) return;

    const payload = buildPayload();
    const validationError = validatePayload(payload);
    if (validationError) {
      showNotice(validationError, false);
      return;
    }

    submittingShipment = true;
    setBusy(createButton, true, 'Đang tạo vận đơn…');

    try {
      const result = await postJson(form.dataset.url, payload);
      const shipment = result.data || {};

      if (shipment.ghnOrderCode) {
        const codeElement = document.getElementById('ghnOrderCode');
        if (codeElement) codeElement.textContent = shipment.ghnOrderCode;
      }
      showNotice(result.message || 'Tạo vận đơn thành công.', true);
      setAddressMessage('Vận đơn đã được tạo thành công.', 'success');
      window.setTimeout(() => window.location.reload(), 800);
    } catch (error) {
      submittingShipment = false;
      setBusy(createButton, false);
      showNotice(error.message || 'Không thể tạo vận đơn. Vui lòng kiểm tra thông tin và thử lại.', false);
    }
  });

  loadAddressCatalog();
})();