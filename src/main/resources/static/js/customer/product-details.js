/* ==========================================================================
   TechNova Electronics — Customer Product Detail
   ==========================================================================
   Chức năng chính:
     1. Thumbnail gallery: click đổi ảnh lớn (event delegation)
     2. Variant switcher: đổi phiên bản → cập nhật giá, kho, SKU, gallery
     3. Quantity stepper: nút +/−
   Không phụ thuộc vào framework ngoài ngoài Bootstrap (đã load ở layout).
   ========================================================================== */

'use strict';

document.addEventListener('DOMContentLoaded', function () {


    /* =========================================================
       DOM REFERENCES
       ========================================================= */

    const mainImage             = document.getElementById('mainImage');
    const thumbnailGallery      = document.getElementById('thumbnailGallery');

    const currentPrice          = document.getElementById('currentPrice');
    const currentOldPrice       = document.getElementById('currentOldPrice');
    const currentDiscountBadge  = document.getElementById('currentDiscountBadge');
    const currentStockStatus    = document.getElementById('currentStockStatus');
    const currentSku            = document.getElementById('currentSku');

    const qtyInput              = document.getElementById('qtyInput');
    const btnQtyMinus           = document.getElementById('btnQtyMinus');
    const btnQtyPlus            = document.getElementById('btnQtyPlus');
    const btnAddToCart          = document.getElementById('btnAddToCart');


    /* =========================================================
       FORMAT PRICE (VND)
       ========================================================= */

    function formatVnd(value) {
        if (value === null || value === undefined || value === '') {
            return '0 ₫';
        }
        return Number(value).toLocaleString('vi-VN') + ' ₫';
    }


    /* =========================================================
       MAIN IMAGE SETTER
       ========================================================= */

    function setMainImage(imageUrl) {
        if (!mainImage) return;

        if (!imageUrl || String(imageUrl).trim() === '') {
            imageUrl = '/images/placeholder.webp';
        }

        mainImage.onerror = function () {
            this.onerror = null;
            this.src = '/images/placeholder.webp';
        };

        mainImage.src = imageUrl;
    }


    /* =========================================================
       THUMBNAIL GALLERY — event delegation
       Hoạt động cả với thumbnail được render bởi Thymeleaf lẫn
       thumbnail được tạo động bởi renderGallery().
       ========================================================= */

    if (thumbnailGallery) {
        thumbnailGallery.addEventListener('click', function (event) {

            const thumbnail = event.target.closest('.gallery-thumb');

            if (!thumbnail) return;

            event.preventDefault();

            /* Xóa active cũ */
            thumbnailGallery
                .querySelectorAll('.gallery-thumb')
                .forEach(function (item) {
                    item.classList.remove('active');
                });

            /* Đánh dấu active mới và đổi ảnh lớn */
            thumbnail.classList.add('active');
            setMainImage(thumbnail.getAttribute('data-src'));
        });
    }


    /* =========================================================
       RENDER GALLERY (dùng khi đổi variant)
       ========================================================= */

    function renderGallery(imageUrls) {

        var images = [];

        if (Array.isArray(imageUrls)) {
            images = imageUrls.filter(function (url) {
                return url && String(url).trim() !== '';
            });
        }

        if (images.length === 0) {
            images = ['/images/placeholder.webp'];
        }

        /* Ảnh lớn → ảnh đầu tiên */
        setMainImage(images[0]);

        if (!thumbnailGallery) return;

        /* Xoá thumbnail cũ */
        thumbnailGallery.innerHTML = '';

        /* Tạo thumbnail mới */
        images.forEach(function (imageUrl, index) {

            var button = document.createElement('button');
            button.type = 'button';
            button.className = 'gallery-thumb';
            if (index === 0) button.classList.add('active');
            button.setAttribute('data-src', imageUrl);
            button.setAttribute('aria-label', 'Xem ảnh ' + (index + 1));

            var img = document.createElement('img');
            img.src      = imageUrl;
            img.alt      = 'Ảnh sản phẩm ' + (index + 1);
            img.width    = 78;
            img.height   = 78;
            img.style.cssText = 'width:100%;height:100%;object-fit:contain;';
            img.onerror = function () {
                this.onerror = null;
                this.src = '/images/placeholder.webp';
            };

            button.appendChild(img);
            thumbnailGallery.appendChild(button);
        });
    }


    /* =========================================================
       INITIAL MAIN IMAGE — fallback nếu src rỗng
       ========================================================= */

    if (mainImage) {
        mainImage.onerror = function () {
            this.onerror = null;
            this.src = '/images/placeholder.webp';
        };

        if (!mainImage.getAttribute('src')) {
            mainImage.src = '/images/placeholder.webp';
        }
    }


    /* =========================================================
       QUANTITY STEPPER
       ========================================================= */

    if (btnQtyMinus && qtyInput) {
        btnQtyMinus.addEventListener('click', function () {
            var value = parseInt(qtyInput.value, 10) || 1;
            if (value > 1) value--;
            qtyInput.value = value;
        });
    }

    if (btnQtyPlus && qtyInput) {
        btnQtyPlus.addEventListener('click', function () {
            var value = parseInt(qtyInput.value, 10) || 1;
            var max   = parseInt(qtyInput.max,   10) || 10;
            if (value < max) value++;
            qtyInput.value = value;
        });
    }

    if (qtyInput) {
        qtyInput.addEventListener('change', function () {
            var value = parseInt(qtyInput.value, 10) || 1;
            var max   = parseInt(qtyInput.max,   10) || 10;
            if (value < 1)   value = 1;
            if (value > max) value = max;
            qtyInput.value = value;
        });
    }


    /* =========================================================
       APPLY VARIANT DATA TO PAGE
       Hàm này cập nhật giá, kho, SKU và gallery khi đổi variant.
       ========================================================= */

    function applyVariant(variant) {

        /* ---- Giá ---- */
        var effectivePrice =
            variant.promotionalPrice &&
            Number(variant.promotionalPrice) > 0
                ? variant.promotionalPrice
                : variant.price;

        if (currentPrice) {
            currentPrice.textContent = formatVnd(effectivePrice);
        }

        /* ---- Giá cũ + badge giảm giá ---- */
        if (
            variant.promotionalPrice &&
            Number(variant.promotionalPrice) < Number(variant.price)
        ) {
            if (currentOldPrice) {
                currentOldPrice.textContent = formatVnd(variant.price);
                currentOldPrice.hidden = false;
            }

            if (currentDiscountBadge) {
                var discount = Math.round(
                    (
                        (Number(variant.price) - Number(variant.promotionalPrice)) * 100
                    ) / Number(variant.price)
                );
                currentDiscountBadge.textContent = '-' + discount + '%';
                currentDiscountBadge.hidden = false;
            }

        } else {
            if (currentOldPrice)       currentOldPrice.hidden = true;
            if (currentDiscountBadge)  currentDiscountBadge.hidden = true;
        }

        /* ---- Tình trạng kho ---- */
        if (currentStockStatus) {
            if (Number(variant.quantity) > 0) {
                currentStockStatus.innerHTML =
                    '<strong class="text-success">' +
                    'Còn hàng (' + variant.quantity + ' sản phẩm)' +
                    '</strong>';

                if (btnAddToCart) btnAddToCart.disabled = false;

                if (qtyInput) {
                    qtyInput.max = Math.min(Number(variant.quantity), 10);
                }

            } else {
                currentStockStatus.innerHTML =
                    '<strong class="text-danger">Tạm hết hàng</strong>';

                if (btnAddToCart) btnAddToCart.disabled = true;
            }
        }

        /* ---- SKU ---- */
        if (currentSku) {
            currentSku.textContent = variant.sku || 'N/A';
        }

        /* ---- Gallery ---- */
        renderGallery(variant.imageUrls);
    }


    /* =========================================================
       VARIANT SWITCHER
       setupVariantSwitcher() gắn click handler cho các nút variant.
       Nhận mảng variants (từ API) để tìm thông tin chi tiết khi click.
       ========================================================= */

    function setupVariantSwitcher(variants) {

        var variantButtons = document.querySelectorAll('.js-variant-choice');

        variantButtons.forEach(function (button) {
            button.addEventListener('click', function () {

                var variantId = button.getAttribute('data-variant-id');

                var variant = variants.find(function (item) {
                    return String(item.id) === String(variantId);
                });

                if (!variant) return;

                /* Cập nhật trạng thái active */
                variantButtons.forEach(function (item) {
                    item.classList.remove('active');
                });
                button.classList.add('active');

                /* Cập nhật trang */
                applyVariant(variant);
            });
        });
    }


    /* =========================================================
       LOAD VARIANTS FROM API
       Dùng slug trong URL để gọi /api/products/{slug}.
       Nếu API thành công → setupVariantSwitcher với dữ liệu đầy đủ.
       Nếu API lỗi → variant buttons vẫn hiển thị đúng (do Thymeleaf
       đã render sẵn), chỉ thiếu khả năng đổi gallery khi click.
       ========================================================= */

    var pathParts = window.location.pathname
        .split('/')
        .filter(function (item) { return item !== ''; });

    var slug = pathParts.length > 0
        ? pathParts[pathParts.length - 1]
        : null;


    if (slug) {
        fetch('/api/products/' + encodeURIComponent(slug), {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        })
        .then(function (response) {
            if (!response.ok) throw new Error('HTTP ' + response.status);
            return response.json();
        })
        .then(function (response) {
            if (
                response &&
                response.success &&
                response.data &&
                Array.isArray(response.data.variants)
            ) {
                setupVariantSwitcher(response.data.variants);
            }
        })
        .catch(function (error) {
            console.warn('Không thể tải dữ liệu biến thể:', error);

            /*
             * Fallback: gắn click handler mà không có gallery update.
             * Ít nhất nút variant vẫn toggle active state.
             */
            var variantButtons = document.querySelectorAll('.js-variant-choice');
            variantButtons.forEach(function (button) {
                button.addEventListener('click', function () {
                    variantButtons.forEach(function (item) {
                        item.classList.remove('active');
                    });
                    button.classList.add('active');
                });
            });
        });
    }


    /* =========================================================
       CART / WISHLIST — stub handlers
       ========================================================= */

    document.body.addEventListener('click', function (event) {

        var addCartBtn = event.target.closest('.js-add-cart');

        if (addCartBtn) {
            event.preventDefault();
            event.stopPropagation();

            if (window.showToast) {
                window.showToast(UiMessage.NOTICE, UiMessage.CART_FEATURE_UPDATING, 'info');
            } else if (window.toast) {
                window.toast(UiMessage.CART_FEATURE_UPDATING);
            } else {
                alert(UiMessage.CART_FEATURE_UPDATING);
            }
            return;
        }

        var wishBtn = event.target.closest('.js-wishlist');

        if (wishBtn) {
            event.preventDefault();
            event.stopPropagation();

            if (window.showToast) {
                window.showToast(UiMessage.NOTICE, UiMessage.WISHLIST_FEATURE_UPDATING, 'info');
            } else if (window.toast) {
                window.toast(UiMessage.WISHLIST_FEATURE_UPDATING);
            } else {
                alert(UiMessage.WISHLIST_FEATURE_UPDATING);
            }
        }

    }, true);


}); /* end DOMContentLoaded */
