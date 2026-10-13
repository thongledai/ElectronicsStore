
(() => {
  'use strict';

  const notice = document.getElementById('orderNotice');

  const showNotice = (message, success = false) => {
    if (!notice) return;

    notice.className = `alert ${success ? 'alert-success' : 'alert-danger'}`;
    notice.textContent = message;
    notice.classList.remove('d-none');
    notice.setAttribute('role', 'status');

    notice.scrollIntoView({ behavior: 'smooth', block: 'center' });
  };

  const setBusy = (button, busy, busyText) => {
    if (!button) return;

    if (busy) {
      button.dataset.originalText = button.textContent.trim();
      button.disabled = true;
      button.textContent = busyText;
    } else {
      button.disabled = false;
      button.textContent = button.dataset.originalText || button.textContent;
    }
  };

  const readResponse = async response => {
    const result = await response.json().catch(() => null);

    if (response.status === 401) {
      throw new Error('Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại.');
    }

    if (response.status === 403) {
      throw new Error('Bạn không có quyền thực hiện thao tác này.');
    }

    if (!response.ok || !result?.success) {
      throw new Error(
        result?.message || `Yêu cầu thất bại (HTTP ${response.status}).`
      );
    }

    return result;
  };

  const postJson = async (url, body) => {
    const options = {
      method: 'POST',
      credentials: 'same-origin',
      headers: {
        Accept: 'application/json'
      }
    };

    if (body !== undefined && body !== null) {
      options.headers['Content-Type'] = 'application/json';
      options.body = JSON.stringify(body);
    }

    const response = await fetch(url, options);
    return readResponse(response);
  };

  const getJson = async url => {
    const response = await fetch(url, {
      method: 'GET',
      credentials: 'same-origin',
      headers: { Accept: 'application/json' }
    });

    return readResponse(response);
  };

  // Xác nhận đơn hàng theo endpoint hiện có.
  const confirmButton = document.getElementById('confirmOrderButton');

  confirmButton?.addEventListener('click', async () => {
    if (!window.confirm('Xác nhận đơn hàng này?')) return;

    setBusy(confirmButton, true, 'Đang xác nhận…');

    try {
      const result = await postJson(confirmButton.dataset.url);
      showNotice(result.message || 'Đã xác nhận đơn hàng.', true);
      window.setTimeout(() => window.location.reload(), 700);
    } catch (error) {
      showNotice(error.message || 'Không thể xác nhận đơn hàng.');
      setBusy(confirmButton, false);
    }
  });

  // Tải lại thông tin vận đơn từ backend/database.
  const refreshButton = document.getElementById('refreshShipmentButton');

  refreshButton?.addEventListener('click', async () => {
    setBusy(refreshButton, true, 'Đang tải…');

    try {
      const result = await getJson(refreshButton.dataset.url);
      const shipment = result.data;

      if (shipment) {
        const codeElement = document.getElementById('ghnOrderCode');
        const statusElement = document.getElementById('ghnStatus');

        if (codeElement && shipment.ghnOrderCode) {
          codeElement.textContent = shipment.ghnOrderCode;
        }

        if (statusElement) {
          statusElement.textContent =
            shipment.ghnStatus || 'Chưa có dữ liệu trạng thái';
        }
      }

      showNotice(result.message || 'Đã tải thông tin vận chuyển.', true);
    } catch (error) {
      showNotice(error.message || 'Không tải được thông tin vận chuyển.');
    } finally {
      setBusy(refreshButton, false);
    }
  });

  const form = document.getElementById('shipmentForm');
  if (!form) return;

  const picker = document.getElementById('ghnAddressPicker');
  const provinceSelect = document.getElementById('ghnProvince');
  const wardSelect = document.getElementById('ghnWard');
  const addressInput = document.getElementById('ghnAddressDetail');
  const addressMessage = document.getElementById('ghnAddressMessage');

  const previewButton = document.getElementById('previewShipmentButton');
  const createButton = document.getElementById('createShipmentButton');
  const previewSection = document.getElementById('ghnPreviewSection');
  const previewPayloadElement = document.querySelector('#ghnPreviewPayload code');

  let previewedPayloadKey = null;
  let loadingCatalog = false;
  let submittingShipment = false;
  let loadingPreview = false;

  const normalize = value =>
    String(value || '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[\u0111\u0110]/g, 'd')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, ' ')
      .trim();

  const fillSelect = (select, items, placeholder) => {
    select.replaceChildren(new Option(placeholder, ''));

    items.forEach(item => {
      select.add(new Option(item.name, String(item.id)));
    });
  };

  const getData = async url => {
    const result = await getJson(url);

    if (!Array.isArray(result.data)) {
      throw new Error('Dữ liệu danh mục GHN không đúng định dạng.');
    }

    return result.data;
  };

  const findExactName = (select, candidate) => {
    const expected = normalize(candidate);
    if (!expected) return null;

    return [...select.options].find(option =>
      option.value && normalize(option.textContent) === expected
    ) || null;
  };

  const setAddressMessage = (message, type = 'info') => {
    if (!addressMessage) return;

    const classes = {
      info: 'alert alert-info mt-3 mb-0',
      success: 'alert alert-success mt-3 mb-0',
      warning: 'alert alert-warning mt-3 mb-0',
      danger: 'alert alert-danger mt-3 mb-0'
    };

    addressMessage.className = classes[type] || classes.info;
    addressMessage.textContent = message;
  };

  const invalidatePreview = () => {
    previewedPayloadKey = null;

    if (createButton && !submittingShipment) {
      createButton.disabled = true;
    }

    if (previewSection) {
      previewSection.hidden = true;
    }

    if (previewPayloadElement) {
      previewPayloadElement.textContent = '';
    }
  };

  const oldProvince = picker.dataset.currentProvince || '';
  const oldWard = picker.dataset.currentWard || '';

  const loadWards = async (provinceId, preferredWard = '') => {
    wardSelect.disabled = true;
    fillSelect(wardSelect, [], 'Đang tải phường/xã…');
    invalidatePreview();

    if (!provinceId) {
      fillSelect(wardSelect, [], 'Chọn tỉnh/thành trước');
      return;
    }

    const prefix = picker.dataset.urlWardsPrefix;
    const wards = await getData(
      `${prefix}${encodeURIComponent(provinceId)}/wards`
    );

    fillSelect(wardSelect, wards, 'Chọn phường/xã');

    const matchedWard = findExactName(wardSelect, preferredWard);
    if (matchedWard) {
      wardSelect.value = matchedWard.value;
    }

    wardSelect.disabled = false;
  };

  const loadAddressCatalog = async () => {
    loadingCatalog = true;
    provinceSelect.disabled = true;
    wardSelect.disabled = true;

    try {
      const provinces = await getData(picker.dataset.urlProvinces);
      fillSelect(provinceSelect, provinces, 'Chọn tỉnh/thành');

      const matchedProvince = findExactName(provinceSelect, oldProvince);

      if (matchedProvince) {
        provinceSelect.value = matchedProvince.value;
        await loadWards(matchedProvince.value, oldWard);

        const matchedWard = findExactName(wardSelect, oldWard);

        if (matchedWard) {
          wardSelect.value = matchedWard.value;
          setAddressMessage(
            'Đã tải danh mục. Kiểm tra địa chỉ gốc của đơn và nhập số nhà, tên đường thực tế trước khi Preview.',
            'info'
          );
        } else {
          setAddressMessage(
            'Không tìm thấy phường/xã cũ trong danh mục của tỉnh/thành đã chọn. Vui lòng chọn lại.',
            'warning'
          );
        }
      } else {
        fillSelect(wardSelect, [], 'Chọn tỉnh/thành trước');
        setAddressMessage(
          'Hãy chọn tỉnh/thành và phường/xã chính xác từ danh mục GHN.',
          'info'
        );
      }
    } catch (error) {
      fillSelect(provinceSelect, [], 'Không tải được danh mục');
      fillSelect(wardSelect, [], 'Không tải được danh mục');

      setAddressMessage(
        error.message || 'Không tải được danh mục địa chỉ GHN.',
        'danger'
      );
    } finally {
      loadingCatalog = false;
      provinceSelect.disabled = false;
    }
  };

  provinceSelect.addEventListener('change', async () => {
    invalidatePreview();

    try {
      await loadWards(provinceSelect.value);
      setAddressMessage(
        'Đã tải phường/xã. Hãy chọn đúng địa chỉ nhận hàng.',
        'info'
      );
    } catch (error) {
      wardSelect.disabled = true;
      setAddressMessage(
        error.message || 'Không tải được danh sách phường/xã.',
        'danger'
      );
    }
  });

  wardSelect.addEventListener('change', invalidatePreview);
  addressInput.addEventListener('input', invalidatePreview);

  [
    'weight',
    'length',
    'width',
    'height',
    'paymentTypeId'
  ].forEach(id => {
    document.getElementById(id)?.addEventListener('change', invalidatePreview);
    document.getElementById(id)?.addEventListener('input', invalidatePreview);
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
    if (!payload.provinceId || !payload.wardId) {
      return 'Vui lòng chọn tỉnh/thành và phường/xã từ danh mục GHN.';
    }

    if (!payload.addressDetail) {
      return 'Vui lòng nhập địa chỉ chi tiết.';
    }

    if (payload.addressDetail.length > 500) {
      return 'Địa chỉ chi tiết không được vượt quá 500 ký tự.';
    }

    if (!Number.isInteger(payload.weight) ||
        payload.weight < 1 || payload.weight > 19999) {
      return 'Khối lượng phải là số nguyên từ 1 đến 19999 gram.';
    }

    for (const field of ['length', 'width', 'height']) {
      if (!Number.isInteger(payload[field]) ||
          payload[field] < 1 || payload[field] > 200) {
        return 'Dài, rộng và cao phải là số nguyên từ 1 đến 200 cm.';
      }
    }

    if (![1, 2].includes(payload.paymentTypeId)) {
      return 'Vui lòng chọn bên trả phí hợp lệ.';
    }

    const provinceOption = provinceSelect.selectedOptions[0];
    const wardOption = wardSelect.selectedOptions[0];

    if (!provinceOption?.value || !wardOption?.value) {
      return 'Không xác định được tên tỉnh/thành hoặc phường/xã đã chọn.';
    }

    return null;
  };

  const payloadKey = payload => JSON.stringify(payload);

  const previewShipment = async () => {
    if (loadingCatalog || loadingPreview || submittingShipment) return;

    const payload = buildPayload();
    const validationError = validatePayload(payload);

    if (validationError) {
      invalidatePreview();
      showNotice(validationError);
      return;
    }

    loadingPreview = true;
    setBusy(previewButton, true, 'Đang tạo Preview…');
    createButton.disabled = true;

    try {
      const result = await postJson(form.dataset.urlPreview, payload);
      const data = result.data;

      if (!data || !data.payload) {
        throw new Error(
          'Backend không trả về payload Preview. Kiểm tra DTO và endpoint Preview.'
        );
      }

      if (previewPayloadElement) {
        previewPayloadElement.textContent = JSON.stringify(data.payload, null, 2);
      }

      if (previewSection) {
        previewSection.hidden = false;
      }

      previewedPayloadKey = payloadKey(payload);
      createButton.disabled = false;

      const provinceName = data.provinceName ||
        provinceSelect.selectedOptions[0].textContent;
      const wardName = data.wardName ||
        wardSelect.selectedOptions[0].textContent;

      setAddressMessage(
        `Địa chỉ Preview: ${wardName}, ${provinceName}. Hãy kiểm tra payload trước khi tạo vận đơn.`,
        'success'
      );

      showNotice(
        result.message || 'Preview thành công. Hãy kiểm tra payload trước khi tạo vận đơn.',
        true
      );
    } catch (error) {
      invalidatePreview();
      showNotice(error.message || 'Không thể tạo Preview.');
    } finally {
      loadingPreview = false;
      setBusy(previewButton, false);
    }
  };

  previewButton.addEventListener('click', previewShipment);

  form.addEventListener('submit', async event => {
    event.preventDefault();

    if (loadingCatalog || loadingPreview || submittingShipment) return;

    const payload = buildPayload();
    const validationError = validatePayload(payload);

    if (validationError) {
      showNotice(validationError);
      return;
    }

    if (previewedPayloadKey !== payloadKey(payload)) {
      showNotice('Dữ liệu đã thay đổi hoặc chưa Preview. Hãy xem trước payload lại.');
      invalidatePreview();
      return;
    }

    if (!window.confirm(
      'Bạn xác nhận gửi yêu cầu tạo vận đơn thật tới GHN staging với địa chỉ đang hiển thị?'
    )) {
      return;
    }

    submittingShipment = true;
    setBusy(createButton, true, 'Đang tạo vận đơn…');
    previewButton.disabled = true;

    try {
      /*
       * Lưu lựa chọn địa chỉ qua endpoint hiện có.
       * Backend phải xác minh provinceId/wardId và quan hệ cha-con,
       * sau đó tự lấy tên chuẩn GHN trước khi dựng payload tạo đơn.
       */
      await postJson(form.dataset.urlAddress, payload);

      const result = await postJson(form.dataset.url, payload);
      const shipment = result.data;

      if (shipment?.ghnOrderCode) {
        const codeElement = document.getElementById('ghnOrderCode');
        if (codeElement) {
          codeElement.textContent = shipment.ghnOrderCode;
        }
      }

      showNotice(
        result.message || 'Tạo vận đơn GHN thành công.',
        true
      );

      window.setTimeout(() => window.location.reload(), 900);
    } catch (error) {
      showNotice(error.message || 'Không thể tạo vận đơn GHN.');

      submittingShipment = false;
      setBusy(createButton, false);
      previewButton.disabled = false;

      /*
       * Sau lỗi tạo đơn, yêu cầu Preview lại trước khi gửi lần nữa.
       * Điều này không thay thế cơ chế chống tạo trùng ở backend.
       */
      invalidatePreview();
    }
  });

  loadAddressCatalog();
})();
