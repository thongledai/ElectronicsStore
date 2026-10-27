/**
 * product-review.js - Xử lý hiển thị & gửi đánh giá sản phẩm phong cách Thế Giới Di Động
 */
document.addEventListener('DOMContentLoaded', function () {
    const productDetailEl = document.getElementById('productDetail');
    if (!productDetailEl) return;
    const productId = productDetailEl.getAttribute('data-product-id');
    if (!productId) return;

    let currentStarFilter = null;
    let currentHasImagesOnly = false;
    let currentPage = 0;
    let totalPages = 1;

    const tabReviewCount = document.getElementById('tabReviewCount');
    const tgddAvgScore = document.getElementById('tgddAvgScore');
    const tgddAvgStars = document.getElementById('tgddAvgStars');
    const tgddTotalReviews = document.getElementById('tgddTotalReviews');

    const bar5Star = document.getElementById('bar5Star');
    const bar4Star = document.getElementById('bar4Star');
    const bar3Star = document.getElementById('bar3Star');
    const bar2Star = document.getElementById('bar2Star');
    const bar1Star = document.getElementById('bar1Star');

    const count5Star = document.getElementById('count5Star');
    const count4Star = document.getElementById('count4Star');
    const count3Star = document.getElementById('count3Star');
    const count2Star = document.getElementById('count2Star');
    const count1Star = document.getElementById('count1Star');

    const filterAllCount = document.getElementById('filterAllCount');
    const filter5Count = document.getElementById('filter5Count');
    const filter4Count = document.getElementById('filter4Count');
    const filter3Count = document.getElementById('filter3Count');
    const filter2Count = document.getElementById('filter2Count');
    const filter1Count = document.getElementById('filter1Count');
    const filterImgCount = document.getElementById('filterImgCount');

    const reviewsLoading = document.getElementById('productReviewsLoading');
    const reviewsEmpty = document.getElementById('productReviewsEmpty');
    const reviewsList = document.getElementById('productReviewsList');
    const loadMoreContainer = document.getElementById('reviewLoadMoreContainer');
    const btnLoadMore = document.getElementById('btnLoadMoreReviews');

    function renderStarsHtml(stars) {
        let html = '';
        for (let i = 1; i <= 5; i++) {
            if (i <= stars) {
                html += '<i class="fas fa-star text-warning"></i>';
            } else {
                html += '<i class="far fa-star star-empty"></i>';
            }
        }
        return html;
    }

    function formatTimeAgo(dateStr) {
        if (!dateStr) return '';
        try {
            const date = new Date(dateStr);
            const now = new Date();
            const diffMs = now - date;
            const diffSec = Math.floor(diffMs / 1000);
            const diffMin = Math.floor(diffSec / 60);
            const diffHour = Math.floor(diffMin / 60);
            const diffDay = Math.floor(diffHour / 24);

            if (diffDay > 30) {
                const d = String(date.getDate()).padStart(2, '0');
                const m = String(date.getMonth() + 1).padStart(2, '0');
                const y = date.getFullYear();
                return `${d}/${m}/${y}`;
            }
            if (diffDay > 0) return `${diffDay} ngày trước`;
            if (diffHour > 0) return `${diffHour} giờ trước`;
            if (diffMin > 0) return `${diffMin} phút trước`;
            return 'Vừa xong';
        } catch (e) {
            return dateStr;
        }
    }

    async function loadReviewSummary() {
        try {
            const res = await fetch(`/api/customer/reviews/product/${productId}/summary`);
            const resData = await res.json();
            if (!resData.success || !resData.data) return;

            const s = resData.data;
            const total = s.totalReviews || 0;
            const avg = s.averageRating != null ? s.averageRating.toFixed(1) : '5.0';

            if (tabReviewCount) tabReviewCount.textContent = total;
            if (tgddAvgScore) tgddAvgScore.textContent = avg;
            if (tgddTotalReviews) tgddTotalReviews.textContent = total;
            if (tgddAvgStars) tgddAvgStars.innerHTML = renderStarsHtml(Math.round(parseFloat(avg)));

            if (count5Star) count5Star.textContent = s.count5Star || 0;
            if (count4Star) count4Star.textContent = s.count4Star || 0;
            if (count3Star) count3Star.textContent = s.count3Star || 0;
            if (count2Star) count2Star.textContent = s.count2Star || 0;
            if (count1Star) count1Star.textContent = s.count1Star || 0;

            if (bar5Star) bar5Star.style.width = `${s.percent5Star || 0}%`;
            if (bar4Star) bar4Star.style.width = `${s.percent4Star || 0}%`;
            if (bar3Star) bar3Star.style.width = `${s.percent3Star || 0}%`;
            if (bar2Star) bar2Star.style.width = `${s.percent2Star || 0}%`;
            if (bar1Star) bar1Star.style.width = `${s.percent1Star || 0}%`;

            if (filterAllCount) filterAllCount.textContent = total;
            if (filter5Count) filter5Count.textContent = s.count5Star || 0;
            if (filter4Count) filter4Count.textContent = s.count4Star || 0;
            if (filter3Count) filter3Count.textContent = s.count3Star || 0;
            if (filter2Count) filter2Count.textContent = s.count2Star || 0;
            if (filter1Count) filter1Count.textContent = s.count1Star || 0;
            if (filterImgCount) filterImgCount.textContent = s.countWithImages || 0;
        } catch (err) {
            console.error('Error loading review summary:', err);
        }
    }

    async function loadReviews(page = 0, append = false) {
        currentPage = page;
        if (!append) {
            if (reviewsLoading) reviewsLoading.classList.remove('d-none');
            if (reviewsEmpty) reviewsEmpty.classList.add('d-none');
            if (reviewsList) reviewsList.classList.add('d-none');
            if (loadMoreContainer) loadMoreContainer.classList.add('d-none');
        }

        try {
            let url = `/api/customer/reviews/product/${productId}?page=${page}&size=5`;
            if (currentStarFilter !== null) {
                url += `&star=${currentStarFilter}`;
            }
            if (currentHasImagesOnly) {
                url += `&hasImages=true`;
            }

            const res = await fetch(url);
            const resData = await res.json();

            if (reviewsLoading) reviewsLoading.classList.add('d-none');

            if (!resData.success || !resData.data || !resData.data.content || resData.data.content.length === 0) {
                if (!append) {
                    if (reviewsEmpty) reviewsEmpty.classList.remove('d-none');
                    if (reviewsList) reviewsList.innerHTML = '';
                }
                if (loadMoreContainer) loadMoreContainer.classList.add('d-none');
                return;
            }

            const pageData = resData.data;
            totalPages = pageData.totalPages || 1;

            const reviewsHtml = pageData.content.map(renderReviewCardHtml).join('');

            if (reviewsEmpty) reviewsEmpty.classList.add('d-none');
            if (reviewsList) {
                reviewsList.classList.remove('d-none');
                if (append) {
                    reviewsList.insertAdjacentHTML('beforeend', reviewsHtml);
                } else {
                    reviewsList.innerHTML = reviewsHtml;
                }
            }

            if (currentPage + 1 < totalPages) {
                if (loadMoreContainer) loadMoreContainer.classList.remove('d-none');
            } else {
                if (loadMoreContainer) loadMoreContainer.classList.add('d-none');
            }
        } catch (err) {
            console.error('Error loading reviews:', err);
            if (reviewsLoading) reviewsLoading.classList.add('d-none');
        }
    }

    function renderReviewCardHtml(r) {
        const initial = (r.userName || 'K').charAt(0).toUpperCase();
        const avatarHtml = r.userAvatar
            ? `<div class="tgdd-avatar"><img src="${escapeHtml(r.userAvatar)}" alt="${escapeHtml(r.userName)}"></div>`
            : `<div class="tgdd-avatar">${escapeHtml(initial)}</div>`;

        let imagesHtml = '';
        if (r.imageUrls && r.imageUrls.length > 0) {
            imagesHtml = `<div class="tgdd-review-images">` +
                r.imageUrls.map(img => `
                    <a href="${escapeHtml(img)}" target="_blank" rel="noopener noreferrer">
                        <img src="${escapeHtml(img)}" class="tgdd-review-img-thumb" alt="Hình ảnh đánh giá">
                    </a>
                `).join('') +
                `</div>`;
        }

        let replyHtml = '';
        if (r.adminReply) {
            replyHtml = `
                <div class="tgdd-store-reply">
                    <div class="fw-bold text-primary small mb-1">
                        <i class="fas fa-store me-1"></i>TechNova Electronics phản hồi:
                    </div>
                    <div class="text-dark small">${escapeHtml(r.adminReply)}</div>
                </div>
            `;
        }

        // Huy hiệu kiểu Thế Giới Di Động: chỉ hiện khi khách hàng đã mua sản phẩm tại TechNova
        const verifiedBadge = r.isPurchased
            ? `<span class="tgdd-badge-verified"><i class="fas fa-circle-check me-1"></i>Đã mua tại TechNova</span>`
            : '';

        return `
            <div class="tgdd-review-card">
                <div class="d-flex align-items-start gap-3">
                    ${avatarHtml}
                    <div class="flex-grow-1">
                        <div class="d-flex align-items-center gap-2 flex-wrap">
                            <span class="fw-bold text-dark">${escapeHtml(r.userName || 'Khách hàng')}</span>
                            ${verifiedBadge}
                            <span class="text-muted small ms-auto">${formatTimeAgo(r.createdAt)}</span>
                        </div>
                        <div class="star-rating-stars my-1">
                            ${renderStarsHtml(r.stars || 5)}
                        </div>
                        <div class="tgdd-review-content">
                            ${escapeHtml(r.content || '')}
                        </div>
                        ${imagesHtml}
                        ${replyHtml}
                    </div>
                </div>
            </div>
        `;
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

    // Filter Pills Event
    const filterPills = document.querySelectorAll('.tgdd-filter-pill');
    filterPills.forEach(pill => {
        pill.addEventListener('click', () => {
            filterPills.forEach(p => p.classList.remove('active'));
            pill.classList.add('active');

            const star = pill.getAttribute('data-star');
            const hasImg = pill.getAttribute('data-images');

            if (hasImg === 'true') {
                currentHasImagesOnly = true;
                currentStarFilter = null;
            } else if (star === 'ALL') {
                currentHasImagesOnly = false;
                currentStarFilter = null;
            } else {
                currentHasImagesOnly = false;
                currentStarFilter = parseInt(star);
            }

            loadReviews(0, false);
        });
    });

    if (btnLoadMore) {
        btnLoadMore.addEventListener('click', () => {
            if (currentPage + 1 < totalPages) {
                loadReviews(currentPage + 1, true);
            }
        });
    }

    // ==============================================================
    // XỬ LÝ VIẾT ĐÁNH GIÁ TRỰC TIẾP TRÊN TRANG SẢN PHẨM (TGDĐ STYLE)
    // ==============================================================
    const btnOpenReviewModal = document.getElementById('btnOpenProductReviewModal');
    const productReviewModalEl = document.getElementById('productReviewModal');
    let productReviewModalInstance = null;
    if (productReviewModalEl && window.bootstrap) {
        productReviewModalInstance = new bootstrap.Modal(productReviewModalEl);
    }

    const starPicker = document.getElementById('pReviewStarPicker');
    const starLabel = document.getElementById('pReviewStarLabel');
    const contentInput = document.getElementById('pReviewContentInput');
    const charCountEl = document.getElementById('pReviewCharCount');
    const photoInput = document.getElementById('pReviewPhotoInput');
    const btnSelectPhotos = document.getElementById('btnSelectPReviewPhotos');
    const previewList = document.getElementById('pReviewPhotoPreviewList');
    const btnSubmitReview = document.getElementById('btnSubmitProductReview');

    const starDescriptions = {
        1: 'Rất tệ',
        2: 'Tệ',
        3: 'Bình thường',
        4: 'Hài lòng',
        5: 'Tuyệt vời'
    };

    let selectedStars = 5;
    let uploadedImageUrls = [];

    if (starPicker) {
        const starIcons = starPicker.querySelectorAll('i');
        starIcons.forEach(icon => {
            icon.addEventListener('mouseenter', () => {
                const star = parseInt(icon.getAttribute('data-star'));
                updateModalStarDisplay(star);
            });
            icon.addEventListener('mouseleave', () => {
                updateModalStarDisplay(selectedStars);
            });
            icon.addEventListener('click', () => {
                selectedStars = parseInt(icon.getAttribute('data-star'));
                document.getElementById('pReviewStars').value = selectedStars;
                updateModalStarDisplay(selectedStars);
            });
        });
    }

    function updateModalStarDisplay(star) {
        if (!starPicker) return;
        const icons = starPicker.querySelectorAll('i');
        icons.forEach(i => {
            const s = parseInt(i.getAttribute('data-star'));
            if (s <= star) {
                i.classList.add('active');
            } else {
                i.classList.remove('active');
            }
        });
        if (starLabel) {
            starLabel.textContent = starDescriptions[star] || '';
        }
    }

    if (contentInput && charCountEl) {
        contentInput.addEventListener('input', () => {
            charCountEl.textContent = `${contentInput.value.length}/1000`;
        });
    }

    if (btnSelectPhotos && photoInput) {
        btnSelectPhotos.addEventListener('click', () => photoInput.click());

        photoInput.addEventListener('change', async (e) => {
            const files = Array.from(e.target.files);
            if (!files.length) return;

            if (uploadedImageUrls.length + files.length > 5) {
                alert('Bạn chỉ có thể tải lên tối đa 5 hình ảnh thực tế.');
                return;
            }

            for (const file of files) {
                if (!file.type.startsWith('image/')) continue;
                if (file.size > 5 * 1024 * 1024) {
                    alert(`Ảnh ${file.name} vượt quá dung lượng tối đa 5MB.`);
                    continue;
                }

                const loadingId = 'uploading_' + Math.random().toString(36).substring(2, 9);
                const placeholderEl = document.createElement('div');
                placeholderEl.className = 'review-upload-preview-item';
                placeholderEl.id = loadingId;
                placeholderEl.innerHTML = `
                    <div class="d-flex align-items-center justify-content-center w-100 h-100 bg-light rounded border">
                        <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
                    </div>
                `;
                if (previewList) previewList.appendChild(placeholderEl);

                try {
                    const formData = new FormData();
                    formData.append('file', file);

                    const uploadRes = await fetch('/api/customer/reviews/upload-image', {
                        method: 'POST',
                        body: formData
                    });

                    if (uploadRes.status === 401 || uploadRes.status === 403) {
                        alert('Vui lòng đăng nhập tài khoản khách hàng để tải ảnh và đánh giá.');
                        window.location.href = `/login?redirect=${encodeURIComponent(window.location.pathname)}`;
                        return;
                    }

                    const uploadData = await uploadRes.json();
                    if (uploadRes.ok && uploadData.success && uploadData.data) {
                        const url = uploadData.data;
                        uploadedImageUrls.push(url);

                        placeholderEl.innerHTML = `
                            <img src="${url}" alt="Ảnh đánh giá" class="rounded border">
                            <button type="button" class="btn-remove-preview" title="Xóa ảnh">&times;</button>
                        `;

                        placeholderEl.querySelector('.btn-remove-preview').addEventListener('click', () => {
                            const idx = uploadedImageUrls.indexOf(url);
                            if (idx > -1) uploadedImageUrls.splice(idx, 1);
                            placeholderEl.remove();
                        });
                    } else {
                        placeholderEl.remove();
                        alert('Tải ảnh thất bại: ' + (uploadData.message || 'Lỗi server'));
                    }
                } catch (err) {
                    console.error('Upload photo error:', err);
                    placeholderEl.remove();
                    alert('Lỗi kết nối khi tải ảnh.');
                }
            }
            photoInput.value = '';
        });
    }

    if (btnOpenReviewModal) {
        btnOpenReviewModal.addEventListener('click', () => {
            // Reset form
            selectedStars = 5;
            uploadedImageUrls = [];
            if (document.getElementById('pReviewStars')) document.getElementById('pReviewStars').value = 5;
            if (contentInput) contentInput.value = '';
            if (charCountEl) charCountEl.textContent = '0/1000';
            if (previewList) previewList.innerHTML = '';
            updateModalStarDisplay(5);

            if (productReviewModalInstance) {
                productReviewModalInstance.show();
            }
        });
    }

    if (btnSubmitReview) {
        btnSubmitReview.addEventListener('click', async () => {
            const stars = parseInt(document.getElementById('pReviewStars').value || '5');
            const content = contentInput ? contentInput.value.trim() : '';

            if (!content || content.length < 10) {
                alert('Vui lòng nhập nội dung đánh giá ít nhất 10 ký tự.');
                if (contentInput) contentInput.focus();
                return;
            }

            btnSubmitReview.disabled = true;
            btnSubmitReview.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Đang gửi...';

            try {
                const payload = {
                    productId: productId,
                    orderId: null, // Đánh giá trực tiếp từ trang chi tiết
                    stars: stars,
                    content: content,
                    imageUrls: uploadedImageUrls
                };

                const res = await fetch('/api/customer/reviews', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify(payload)
                });

                if (res.status === 401 || res.status === 403) {
                    alert('Vui lòng đăng nhập tài khoản khách hàng để gửi đánh giá.');
                    window.location.href = `/login?redirect=${encodeURIComponent(window.location.pathname)}`;
                    return;
                }

                const resData = await res.json();
                if (res.ok && resData.success !== false) {
                    if (productReviewModalInstance) productReviewModalInstance.hide();
                    if (typeof showToast === 'function') {
                        showToast('Đã gửi đánh giá', 'Đánh giá của bạn đã được gửi và đang chờ nhân viên kiểm duyệt!', 'success');
                    } else {
                        alert('Đánh giá của bạn đã được gửi thành công và đang chờ nhân viên kiểm duyệt!');
                    }
                } else {
                    alert(resData.message || 'Không thể gửi đánh giá.');
                }
            } catch (err) {
                console.error('Submit review error:', err);
                alert('Lỗi kết nối khi gửi đánh giá.');
            } finally {
                btnSubmitReview.disabled = false;
                btnSubmitReview.innerHTML = '<i class="fas fa-paper-plane me-1"></i>Gửi đánh giá';
            }
        });
    }

    // Khởi động tải dữ liệu
    loadReviewSummary();
    loadReviews(0, false);
});
