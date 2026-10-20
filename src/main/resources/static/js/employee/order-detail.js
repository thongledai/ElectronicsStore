'use strict';

document.addEventListener('DOMContentLoaded', () => {
    const noticeEl = document.getElementById('orderNotice');

    function showNotice(message, type = 'danger') {
        if (!noticeEl) {
            if (typeof showToast === 'function') {
                showToast(type === 'success' ? 'Thành công' : 'Thông báo', message, type);
            } else {
                alert(message);
            }
            return;
        }
        noticeEl.className = `alert alert-${type} delivery-notice mb-3`;
        noticeEl.innerHTML = `<span>${message}</span>`;
        noticeEl.classList.remove('d-none');
        noticeEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    function csrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        return token && header ? { [header]: token } : {};
    }

    /* ---------- 1. Sao chép vào clipboard ---------- */
    document.querySelectorAll('.js-copy-btn, [data-copy-value]').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            e.preventDefault();
            const textToCopy = btn.dataset.copyValue;
            if (!textToCopy) return;

            try {
                if (navigator.clipboard && window.isSecureContext) {
                    await navigator.clipboard.writeText(textToCopy);
                } else {
                    const textArea = document.createElement('textarea');
                    textArea.value = textToCopy;
                    textArea.style.position = 'fixed';
                    textArea.style.left = '-999999px';
                    document.body.appendChild(textArea);
                    textArea.focus();
                    textArea.select();
                    document.execCommand('copy');
                    textArea.remove();
                }

                const originalHtml = btn.innerHTML;
                btn.innerHTML = '<i class="fa-solid fa-check text-success me-1"></i>Đã sao chép';
                setTimeout(() => {
                    btn.innerHTML = originalHtml;
                }, 2000);

                if (typeof showToast === 'function') {
                    showToast('Thành công', `Đã sao chép: ${textToCopy}`, 'success');
                }
            } catch (err) {
                console.error('Không thể sao chép:', err);
            }
        });
    });

    /* ---------- 2. Xác nhận đơn hàng ---------- */
    const confirmBtn = document.getElementById('confirmOrderButton');
    if (confirmBtn) {
        confirmBtn.addEventListener('click', async () => {
            const url = confirmBtn.dataset.url;
            if (!url) return;

            confirmBtn.disabled = true;
            const originalText = confirmBtn.innerHTML;
            confirmBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>Đang xử lý...';

            try {
                const response = await fetch(url, {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: {
                        'Accept': 'application/json',
                        ...csrfHeaders()
                    }
                });

                const data = await response.json().catch(() => null);

                if (response.status === 401) {
                    location.href = '/login?error=unauthorized';
                    return;
                }

                if (!response.ok || (data && data.success === false)) {
                    throw new Error((data && data.message) || 'Xác nhận đơn hàng thất bại.');
                }

                const modalEl = document.getElementById('confirmOrderModal');
                if (modalEl) {
                    const modal = bootstrap.Modal.getInstance(modalEl);
                    if (modal) modal.hide();
                }

                showNotice(data?.message || 'Đã xác nhận đơn hàng thành công!', 'success');
                setTimeout(() => {
                    window.location.reload();
                }, 800);
            } catch (err) {
                showNotice(err.message, 'danger');
                confirmBtn.disabled = false;
                confirmBtn.innerHTML = originalText;
            }
        });
    }

    /* ---------- 3. Tạo vận đơn GHN ---------- */
    const shipmentForm = document.getElementById('shipmentForm');
    if (shipmentForm) {
        shipmentForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const createBtn = document.getElementById('createShipmentButton');
            const url = shipmentForm.dataset.url;
            if (!url) return;

            const weight = parseInt(document.getElementById('weight')?.value, 10);
            const length = parseInt(document.getElementById('length')?.value, 10);
            const width = parseInt(document.getElementById('width')?.value, 10);
            const height = parseInt(document.getElementById('height')?.value, 10);

            if (isNaN(weight) || weight <= 0) {
                showNotice('Khối lượng kiện hàng không hợp lệ.');
                return;
            }

            const payload = { weight, length, width, height };

            if (createBtn) {
                createBtn.disabled = true;
                createBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>Đang tạo vận đơn...';
            }

            try {
                const response = await fetch(url, {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json',
                        ...csrfHeaders()
                    },
                    body: JSON.stringify(payload)
                });

                const data = await response.json().catch(() => null);

                if (response.status === 401) {
                    location.href = '/login?error=unauthorized';
                    return;
                }

                if (!response.ok || (data && data.success === false)) {
                    throw new Error((data && data.message) || 'Không thể tạo vận đơn GHN.');
                }

                showNotice(data?.message || 'Đã tạo vận đơn giao hàng GHN thành công!', 'success');
                setTimeout(() => {
                    window.location.reload();
                }, 800);
            } catch (err) {
                showNotice(err.message, 'danger');
                if (createBtn) {
                    createBtn.disabled = false;
                    createBtn.innerHTML = '<i class="fa-solid fa-truck-fast me-1"></i>Tạo vận đơn GHN';
                }
            }
        });
    }

    /* ---------- 4. Làm mới thông tin vận chuyển GHN ---------- */
    const refreshBtn = document.getElementById('refreshShipmentButton');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', async () => {
            const url = refreshBtn.dataset.url;
            if (!url) return;

            const originalHtml = refreshBtn.innerHTML;
            refreshBtn.disabled = true;
            refreshBtn.innerHTML = '<i class="fa-solid fa-arrows-rotate fa-spin me-1"></i>Đang làm mới...';

            try {
                const response = await fetch(url, {
                    method: 'GET',
                    credentials: 'same-origin',
                    headers: {
                        'Accept': 'application/json',
                        ...csrfHeaders()
                    }
                });

                const data = await response.json().catch(() => null);

                if (!response.ok || (data && data.success === false)) {
                    throw new Error((data && data.message) || 'Không thể làm mới thông tin vận chuyển.');
                }

                const shipment = data.data;
                if (shipment) {
                    if (shipment.ghnOrderCode) {
                        const codeEl = document.getElementById('ghnOrderCode');
                        if (codeEl) codeEl.textContent = shipment.ghnOrderCode;
                    }
                    if (shipment.ghnStatus) {
                        const statusEl = document.getElementById('ghnStatus');
                        if (statusEl) {
                            statusEl.textContent = shipment.ghnStatus;
                            statusEl.dataset.ghnStatus = shipment.ghnStatus;
                        }
                    }
                    if (shipment.ghnShippingFee != null) {
                        const feeEl = document.getElementById('ghnShippingFee');
                        if (feeEl) {
                            feeEl.textContent = new Intl.NumberFormat('vi-VN').format(shipment.ghnShippingFee) + ' ₫';
                        }
                    }
                    if (shipment.expectedDeliveryTime) {
                        const expEl = document.getElementById('expectedDeliveryTime');
                        if (expEl) {
                            const date = new Date(shipment.expectedDeliveryTime);
                            expEl.textContent = date.toLocaleString('vi-VN');
                        }
                    }
                    const updatedEl = document.getElementById('ghnStatusUpdatedAt');
                    if (updatedEl) {
                        const now = new Date();
                        updatedEl.textContent = 'Cập nhật lúc: ' + now.toLocaleTimeString('vi-VN') + ' ' + now.toLocaleDateString('vi-VN');
                    }
                    showNotice('Thông tin vận chuyển đã được làm mới thành công.', 'success');
                }
            } catch (err) {
                showNotice(err.message, 'danger');
            } finally {
                refreshBtn.disabled = false;
                refreshBtn.innerHTML = originalHtml;
            }
        });
    }
});
