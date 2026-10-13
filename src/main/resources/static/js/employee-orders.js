(() => {
  const notice = document.getElementById('orderNotice');
  const showNotice = (message, success = false) => {
    if (!notice) return;
    notice.className = `alert ${success ? 'alert-success' : 'alert-danger'}`;
    notice.textContent = message;
    notice.scrollIntoView({ behavior: 'smooth', block: 'center' });
  };
  const postJson = async (url, body) => {
    const response = await fetch(url, {
      method: 'POST', credentials: 'same-origin',
      headers: { Accept: 'application/json', ...(body ? { 'Content-Type': 'application/json' } : {}) },
      body: body ? JSON.stringify(body) : undefined
    });
    const result = await response.json().catch(() => null);
    if (!response.ok || !result?.success) throw new Error(result?.message || `Yêu cầu thất bại (HTTP ${response.status}).`);
    return result;
  };

  const confirmButton = document.getElementById('confirmOrderButton');
  confirmButton?.addEventListener('click', async () => {
    if (!window.confirm('Xác nhận đơn hàng này?')) return;
    confirmButton.disabled = true;
    try {
      const result = await postJson(confirmButton.dataset.url);
      showNotice(result.message || 'Đã xác nhận đơn hàng.', true);
      window.setTimeout(() => window.location.reload(), 600);
    } catch (error) {
      showNotice(error.message);
      confirmButton.disabled = false;
    }
  });

  const form = document.getElementById('shipmentForm');
  if (!form) return;
  const picker = document.getElementById('ghnAddressPicker');
  const provinceSelect = document.getElementById('ghnProvince');
  const wardSelect = document.getElementById('ghnWard');
  const addressInput = document.getElementById('ghnAddressDetail');
  const addressMessage = document.getElementById('ghnAddressMessage');
  const normalize = value => String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/[\u0111\u0110]/g, 'd').toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim();
  const fill = (select, items, label) => {
    select.replaceChildren(new Option(label, ''));
    items.forEach(item => select.add(new Option(item.name, String(item.id))));
  };
  const getData = async url => {
    const response = await fetch(url, { credentials: 'same-origin', headers: { Accept: 'application/json' } });
    const result = await response.json().catch(() => null);
    if (!response.ok || !result?.success || !Array.isArray(result.data)) throw new Error(result?.message || 'Không tải được danh mục GHN.');
    return result.data;
  };
  const exactMatch = (select, candidate) => [...select.options].find(option => option.value && normalize(option.textContent) === normalize(candidate));
  const oldAddress = picker.dataset.currentAddress || '';
  const oldProvince = picker.dataset.currentProvince || '';
  const oldWard = picker.dataset.currentWard || '';
  if (oldAddress) {
    const parts = oldAddress.split(',').map(part => part.trim()).filter(Boolean);
    const administrative = [oldWard, oldProvince].map(normalize).filter(Boolean);
    const details = parts.filter(part => {
      const value = normalize(part);
      return !administrative.some(name => value === name || value.endsWith(` ${name}`))
        && !/^(phuong|p|xa|thi tran|quan|q|huyen|thanh pho|tp|tinh)\b/.test(value);
    });
    if (details.length) addressInput.value = details.join(', ');
  }

  const loadWards = async (provinceId, preferredWard = '') => {
    wardSelect.disabled = true;
    fill(wardSelect, [], 'Đang tải phường/xã…');
    if (!provinceId) { fill(wardSelect, [], 'Chọn tỉnh/thành trước'); return; }
    const wards = await getData(`${picker.dataset.urlWardsPrefix}${encodeURIComponent(provinceId)}/wards`);
    fill(wardSelect, wards, 'Chọn phường/xã');
    const match = exactMatch(wardSelect, preferredWard);
    if (match) wardSelect.value = match.value;
    wardSelect.disabled = false;
  };

  (async () => {
    try {
      const provinces = await getData(picker.dataset.urlProvinces);
      fill(provinceSelect, provinces, 'Chọn tỉnh/thành');
      const match = exactMatch(provinceSelect, oldProvince);
      if (match) {
        provinceSelect.value = match.value;
        await loadWards(match.value, oldWard);
        addressMessage.textContent = 'Đã gợi ý địa chỉ hiện có. Hãy kiểm tra lại tỉnh/thành và phường/xã.';
      } else {
        fill(wardSelect, [], 'Chọn tỉnh/thành trước');
        addressMessage.textContent = 'Chọn tỉnh/thành và phường/xã trong danh mục GHN.';
      }
    } catch (error) {
      fill(provinceSelect, [], 'Không tải được danh mục');
      fill(wardSelect, [], 'Không tải được danh mục');
      addressMessage.className = 'alert alert-danger mt-3 mb-0';
      addressMessage.textContent = error.message;
    }
  })();

  provinceSelect.addEventListener('change', async () => {
    try { await loadWards(provinceSelect.value); }
    catch (error) { addressMessage.className = 'alert alert-danger mt-3 mb-0'; addressMessage.textContent = error.message; }
  });

  form.addEventListener('submit', async event => {
    event.preventDefault();
    if (!window.confirm('Gửi yêu cầu tạo vận đơn thật tới GHN staging?')) return;
    const button = document.getElementById('createShipmentButton');
    button.disabled = true;
    const values = new FormData(form);
    const payload = {
      provinceId: Number(values.get('provinceId')), wardId: Number(values.get('wardId')),
      addressDetail: String(values.get('addressDetail') || '').trim(),
      weight: Number(values.get('weight')), length: Number(values.get('length')),
      width: Number(values.get('width')), height: Number(values.get('height')),
      paymentTypeId: Number(values.get('paymentTypeId')), requiredNote: String(values.get('requiredNote') || '')
    };
    if (!payload.provinceId || !payload.wardId || !payload.addressDetail) {
      showNotice('Chọn tỉnh/thành, phường/xã GHN và nhập địa chỉ chi tiết.');
      button.disabled = false;
      return;
    }
    try {
      await postJson(form.dataset.urlAddress, payload);
      const result = await postJson(form.dataset.url, payload);
      showNotice(result.message || 'Tạo vận đơn GHN thành công.', true);
      window.setTimeout(() => window.location.reload(), 700);
    } catch (error) {
      showNotice(error.message);
      button.disabled = false;
    }
  });
})();
