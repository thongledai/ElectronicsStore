/**
 * reviews.js - Quản lý & duyệt đánh giá sản phẩm cho nhân viên
 */
document.addEventListener('DOMContentLoaded', function () {
    let currentStatus = 'PENDING';
    let currentPage = 0;
    let totalPages = 1;
    let selectedReviewId = null;

    const tabs = document.querySelectorAll('#reviewStatusTabs button');
    const loadingEl = document.getElementById('employeeReviewsLoading');
    const emptyEl = document.getElementById('employeeReviewsEmpty');
    const emptyText = document.getElementById('employeeReviewsEmptyText');
    const tableContainer = document.getElementById('employeeReviewsTableContainer');
    const tableBody = document.getElementById('employeeReviewsTableBody');
    const tabPendingBadge = document.getElementById('tabPendingBadge');
    const sidebarBadge = document.getElementById('sidebarPendingReviewsBadge');

    const paginationContainer = document.getElementById('reviewsPaginationContainer');
    const pageInfo = document.getElementById('reviewsPageInfo');
    const btnPrev = document.getElementById('btnPrevReviewsPage');
    const btnNext = document.getElementById('btnNextReviewsPage');
    const btnRefresh = document.getElementById('btnRefreshReviews');

    const approveModalEl = document.getElementById('approveModal');
    const approveModal = approveModalEl && window.bootstrap ? new bootstrap.Modal(approveModalEl) : null;
    const adminReplyInput = document.getElementById('adminReplyInput');
    const btnConfirmApprove = document.getElementById('btnConfirmApprove');

    const rejectModalEl = document.getElementById('rejectModal');
    const rejectModal = rejectModalEl && window.bootstrap ? new bootstrap.Modal(rejectModalEl) : null;
    const rejectReasonInput = document.getElementById('rejectReasonInput');
    const btnConfirmReject = document.getElementById('btnConfirmReject');

    // 1. Tải số lượng chờ duyệt
    async function updatePendingCount() {
        try {
            const res = await fetch('/api/employee/reviews/count-pending');
            const resData = await res.json();
            if (resData.success) {
                const count = resData.data || 0;
                if (tabPendingBadge) tabPendingBadge.textContent = count;
                if (sidebarBadge) {
                    sidebarBadge.textContent = count;
                    if (count > 0) {
                        sidebarBadge.classList.remove('d-none');
                    } else {
                        sidebarBadge.classList.add('d-none');
                    }
                }
            }
        } catch (e) {
            console.error('Update pending count error:', e);
        }
    }

    // 2. Tải danh sách đánh giá
    async function loadReviews(page = 0) {
        currentPage = page;
        loadingEl.classList.remove('d-none');
        emptyEl.classList.add('d-none');
        tableContainer.classList.add('d-none');
        paginationContainer.classList.add('d-none');

        try {
            let url = '';
            if (currentStatus === 'PENDING') {
                url = `/api/employee/reviews/pending?page=${page}&size=10`;
            } else if (currentStatus === 'ALL') {
                url = `/api/employee/reviews/all?page=${page}&size=10`;
            } else {
                url = `/api/employee/reviews/all?status=${currentStatus}&page=${page}&size=10`;
            }

            const res = await fetch(url);
            const resData = await res.json();

            loadingEl.classList.add('d-none');

            if (!resData.success || !resData.data || !resData.data.content || resData.data.content.length === 0) {
                emptyEl.classList.remove('d-none');
                tableBody.innerHTML = '';
                return;
            }

            const data = resData.data;
            totalPages = data.totalPages || 1;

            tableBody.innerHTML = data.content.map(renderRowHtml).join('');
            tableContainer.classList.remove('d-none');

            // Pagination
            if (totalPages > 1) {
                paginationContainer.classList.remove('d-none');
                pageInfo.textContent = `Trang ${currentPage + 1} / ${totalPages}`;
                btnPrev.disabled = currentPage === 0;
                btnNext.disabled = currentPage + 1 >= totalPages;
            } else {
                paginationContainer.classList.add('d-none');
            }

            bindRowEvents();
        } catch (err) {
            console.error('Load reviews error:', err);
            loadingEl.classList.add('d-none');
            emptyEl.classList.remove('d-none');
            if (emptyText) emptyText.textContent = 'Lỗi kết nối khi tải danh sách đánh giá.';
        }
    }

    function renderRowHtml(r) {
        let starsHtml = '';
        for (let i = 1; i <= 5; i++) {
            starsHtml += i <= r.stars
                ? '<i class="fa-solid fa-star text-warning" style="font-size: 0.85rem;"></i>'
                : '<i class="fa-regular fa-star text-secondary opacity-25" style="font-size: 0.85rem;"></i>';
        }

        let imagesHtml = '';
        if (r.imageUrls && r.imageUrls.length > 0) {
            imagesHtml = `<div class="d-flex gap-1 mt-2">` +
                r.imageUrls.map(img => `
                    <a href="${escapeHtml(img)}" target="_blank" rel="noopener noreferrer">
                        <img src="${escapeHtml(img)}" style="width: 44px; height: 44px; object-fit: cover;" class="rounded border">
                    </a>
                `).join('') +
                `</div>`;
        }

        let statusBadge = '';
        if (r.status === 'PENDING') {
            statusBadge = '<span class="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle px-2 py-1 fw-medium">Chờ duyệt</span>';
        } else if (r.status === 'APPROVED') {
            statusBadge = '<span class="badge bg-success-subtle text-success-emphasis border border-success-subtle px-2 py-1 fw-medium">Đã duyệt</span>';
        } else if (r.status === 'REJECTED') {
            statusBadge = '<span class="badge bg-danger-subtle text-danger-emphasis border border-danger-subtle px-2 py-1 fw-medium">Từ chối</span>';
        }

        let replyHtml = '';
        if (r.adminReply) {
            replyHtml = `<div class="small text-muted mt-1 bg-light p-2 rounded border"><span class="fw-medium text-dark">Phản hồi:</span> ${escapeHtml(r.adminReply)}</div>`;
        }

        let chatBtnHtml = '';
        if (r.userId) {
            chatBtnHtml = `
                <a href="/employee/chat?customerId=${r.userId}" class="btn btn-sm btn-outline-primary" title="Nhắn tin với ${escapeHtml(r.userName || 'khách hàng')}">
                    <i class="fa-regular fa-comment-dots me-1"></i>Chat
                </a>
            `;
        }

        let actionsHtml = '';
        if (r.status === 'PENDING') {
            actionsHtml = `
                <div class="d-flex justify-content-end align-items-center gap-1 flex-wrap">
                    ${chatBtnHtml}
                    <button type="button" class="btn btn-sm btn-primary js-btn-approve" data-id="${r.id}" title="Duyệt đánh giá">
                        <i class="fa-solid fa-check me-1"></i>Duyệt
                    </button>
                    <button type="button" class="btn btn-sm btn-outline-secondary js-btn-reject" data-id="${r.id}" title="Từ chối">
                        Từ chối
                    </button>
                </div>
            `;
        } else {
            actionsHtml = `
                <div class="d-flex justify-content-end align-items-center gap-2">
                    ${chatBtnHtml}
                    <span class="text-muted small">Đã xử lý</span>
                </div>
            `;
        }

        return `
            <tr data-review-id="${r.id}">
                <td>
                    <div class="fw-semibold text-dark">${escapeHtml(r.productName || 'Sản phẩm')}</div>
                    <div class="text-muted small font-monospace">Mã đơn: #${r.orderId ? r.orderId.substring(0, 8) : ''}</div>
                </td>
                <td>
                    <div class="fw-medium text-dark">${escapeHtml(r.userName || 'Khách hàng')}</div>
                    ${r.isPurchased ? '<div class="text-success small" style="font-size: 0.72rem;"><i class="fa-solid fa-check me-1"></i>Đã mua hàng</div>' : ''}
                </td>
                <td>
                    <div class="text-nowrap">${starsHtml}</div>
                </td>
                <td>
                    <div class="text-dark small" style="max-width: 320px; word-break: break-word;">${escapeHtml(r.content || '')}</div>
                    ${imagesHtml}
                    ${replyHtml}
                </td>
                <td>${statusBadge}</td>
                <td>
                    <div class="small text-muted text-nowrap">${formatDate(r.createdAt)}</div>
                </td>
                <td class="text-end">
                    ${actionsHtml}
                </td>
            </tr>
        `;
    }

    function bindRowEvents() {
        document.querySelectorAll('.js-btn-approve').forEach(btn => {
            btn.addEventListener('click', () => {
                selectedReviewId = btn.getAttribute('data-id');
                if (adminReplyInput) adminReplyInput.value = '';
                if (approveModal) approveModal.show();
            });
        });

        document.querySelectorAll('.js-btn-reject').forEach(btn => {
            btn.addEventListener('click', () => {
                selectedReviewId = btn.getAttribute('data-id');
                if (rejectReasonInput) rejectReasonInput.value = '';
                if (rejectModal) rejectModal.show();
            });
        });
    }

    // Xác nhận Duyệt
    if (btnConfirmApprove) {
        btnConfirmApprove.addEventListener('click', async () => {
            if (!selectedReviewId) return;
            const reply = adminReplyInput ? adminReplyInput.value.trim() : '';

            btnConfirmApprove.disabled = true;
            btnConfirmApprove.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang duyệt...';

            try {
                const res = await fetch(`/api/employee/reviews/${selectedReviewId}/approve`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify({ adminReply: reply, status: 'APPROVED' })
                });

                const resData = await res.json();
                if (res.ok && resData.success !== false) {
                    if (approveModal) approveModal.hide();
                    if (typeof showToast === 'function') {
                        showToast('Thành công', 'Đã duyệt đánh giá thành công!', 'success');
                    } else {
                        alert('Đã duyệt đánh giá thành công!');
                    }
                    updatePendingCount();
                    loadReviews(currentPage);
                } else {
                    alert(resData.message || 'Lỗi khi duyệt đánh giá');
                }
            } catch (err) {
                console.error('Approve review error:', err);
                alert('Lỗi kết nối');
            } finally {
                btnConfirmApprove.disabled = false;
                btnConfirmApprove.innerHTML = '<i class="fa-solid fa-check me-1"></i>Xác nhận duyệt';
            }
        });
    }

    // Xác nhận Từ chối
    if (btnConfirmReject) {
        btnConfirmReject.addEventListener('click', async () => {
            if (!selectedReviewId) return;
            const reason = rejectReasonInput ? rejectReasonInput.value.trim() : '';
            if (!reason) {
                alert('Vui lòng nhập lý do từ chối đánh giá.');
                if (rejectReasonInput) rejectReasonInput.focus();
                return;
            }

            btnConfirmReject.disabled = true;
            btnConfirmReject.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang xử lý...';

            try {
                const res = await fetch(`/api/employee/reviews/${selectedReviewId}/reject`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify({ adminReply: reason, status: 'REJECTED' })
                });

                const resData = await res.json();
                if (res.ok && resData.success !== false) {
                    if (rejectModal) rejectModal.hide();
                    if (typeof showToast === 'function') {
                        showToast('Thành công', 'Đã từ chối đánh giá!', 'warning');
                    } else {
                        alert('Đã từ chối đánh giá!');
                    }
                    updatePendingCount();
                    loadReviews(currentPage);
                } else {
                    alert(resData.message || 'Lỗi khi từ chối đánh giá');
                }
            } catch (err) {
                console.error('Reject review error:', err);
                alert('Lỗi kết nối');
            } finally {
                btnConfirmReject.disabled = false;
                btnConfirmReject.innerHTML = '<i class="fa-solid fa-ban me-1"></i>Xác nhận từ chối';
            }
        });
    }

    // Tab clicks
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            tabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            currentStatus = tab.getAttribute('data-status');
            loadReviews(0);
        });
    });

    if (btnPrev) {
        btnPrev.addEventListener('click', () => {
            if (currentPage > 0) loadReviews(currentPage - 1);
        });
    }
    if (btnNext) {
        btnNext.addEventListener('click', () => {
            if (currentPage + 1 < totalPages) loadReviews(currentPage + 1);
        });
    }
    if (btnRefresh) {
        btnRefresh.addEventListener('click', () => {
            updatePendingCount();
            loadReviews(currentPage);
        });
    }

    function formatDate(dateStr) {
        if (!dateStr) return '';
        try {
            const d = new Date(dateStr);
            const day = String(d.getDate()).padStart(2, '0');
            const mon = String(d.getMonth() + 1).padStart(2, '0');
            const h = String(d.getHours()).padStart(2, '0');
            const m = String(d.getMinutes()).padStart(2, '0');
            return `${h}:${m} ${day}/${mon}`;
        } catch {
            return dateStr;
        }
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

    // 3. WebSocket Realtime Notification
    function initWebSocketNotification() {
        try {
            const socket = new SockJS('/ws');
            const stompClient = new StompJs.Client({
                webSocketFactory: () => socket,
                reconnectDelay: 5000
            });

            stompClient.onConnect = function () {
                stompClient.subscribe('/topic/employee/reviews', function (msgOutput) {
                    try {
                        const data = JSON.parse(msgOutput.body);
                        if (data.type === 'NEW_REVIEW') {
                            if (typeof showToast === 'function') {
                                showToast('Đánh giá mới!', data.message || 'Có 1 đánh giá mới cần duyệt', 'info');
                            }
                            updatePendingCount();
                            if (currentStatus === 'PENDING') {
                                loadReviews(0);
                            }
                        }
                    } catch (e) {
                        console.error('Parse WS review notification error:', e);
                    }
                });
            };

            stompClient.activate();
        } catch (e) {
            console.warn('WS review error:', e);
        }
    }

    // Khởi động
    updatePendingCount();
    loadReviews(0);
    initWebSocketNotification();
});
