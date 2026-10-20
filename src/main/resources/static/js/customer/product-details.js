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
            updateQuantityButtons();
        });
    }

    if (btnQtyPlus && qtyInput) {
        btnQtyPlus.addEventListener('click', function () {
            var value = parseInt(qtyInput.value, 10) || 1;
            var max   = parseInt(qtyInput.max,   10) || 10;
            if (value < max) value++;
            qtyInput.value = value;
            updateQuantityButtons();
        });
    }

    if (qtyInput) {
        qtyInput.addEventListener('change', function () {
            var value = parseInt(qtyInput.value, 10) || 1;
            var max   = parseInt(qtyInput.max,   10) || 10;
            if (value < 1)   value = 1;
            if (value > max) value = max;
            qtyInput.value = value;
            updateQuantityButtons();
        });
    }

    function updateQuantityButtons() {
        if (!qtyInput) return;
        var value = parseInt(qtyInput.value, 10) || 1;
        var max = parseInt(qtyInput.max, 10) || 10;
        if (btnQtyMinus) btnQtyMinus.disabled = value <= 1;
        if (btnQtyPlus) btnQtyPlus.disabled = value >= max;
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
        if (btnAddToCart) btnAddToCart.dataset.variantId = variant.id || '';

        /* ---- Giá cũ + badge giảm giá ---- */
        if (
            variant.promotionalPrice &&
            Number(variant.promotionalPrice) < Number(variant.price)
        ) {
            if (currentOldPrice) {
                currentOldPrice.textContent = formatVnd(variant.price);
                currentOldPrice.classList.remove('price-hidden');
            }

            if (currentDiscountBadge) {
                var discount = Math.round(
                    (
                        (Number(variant.price) - Number(variant.promotionalPrice)) * 100
                    ) / Number(variant.price)
                );
                currentDiscountBadge.textContent = '-' + discount + '%';
                currentDiscountBadge.classList.remove('price-hidden');
            }

        } else {
            if (currentOldPrice)       currentOldPrice.classList.add('price-hidden');
            if (currentDiscountBadge)  currentDiscountBadge.classList.add('price-hidden');
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
                    var requestedQuantity = parseInt(qtyInput.value, 10) || 1;
                    qtyInput.value = Math.min(requestedQuantity, Number(qtyInput.max));
                }

            } else {
                currentStockStatus.innerHTML =
                    '<strong class="text-danger">Tạm hết hàng</strong>';

                if (btnAddToCart) btnAddToCart.disabled = true;
                if (qtyInput) {
                    qtyInput.max = 1;
                    qtyInput.value = 1;
                }
            }
        }
        updateQuantityButtons();

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
        var controls = document.getElementById('variantConfigControls');
        if (!controls || !Array.isArray(variants) || variants.length === 0) return;

        var groupsByKey = new Map();
        variants.forEach(function (variant) {
            (variant.styleValues || []).forEach(function (styleValue) {
                var key = String(styleValue.styleId || styleValue.styleName || '');
                if (!key) return;
                if (!groupsByKey.has(key)) {
                    groupsByKey.set(key, { key: key, name: styleValue.styleName || 'Cấu hình', values: new Map() });
                }
                var valueKey = String(styleValue.id || styleValue.name || '');
                if (valueKey) groupsByKey.get(key).values.set(valueKey, styleValue.name || valueKey);
            });
        });

        var groups = Array.from(groupsByKey.values());
        function stylePriority(name) {
            var value = String(name || '').toLowerCase();
            if (/color|colour|màu/.test(value)) return 0;
            if (/storage|dung lượng|bộ nhớ|ram/.test(value)) return 1;
            if (/size|kích thước|màn hình/.test(value)) return 2;
            return 3;
        }
        groups.sort(function (a, b) {
            return stylePriority(a.name) - stylePriority(b.name) || a.name.localeCompare(b.name);
        });
        var availableVariants = variants.filter(function (variant) { return Number(variant.quantity) > 0; });
        var selectedVariant = availableVariants[0] || variants[0];
        var selectedValues = new Map();

        function valueFor(variant, styleKey) {
            var value = (variant.styleValues || []).find(function (styleValue) {
                return String(styleValue.styleId || styleValue.styleName || '') === styleKey;
            });
            return value ? String(value.id || value.name || '') : '';
        }

        groups.forEach(function (group) {
            selectedValues.set(group.key, valueFor(selectedVariant, group.key));
        });

        function matches(variant, exceptStyle, candidateValue) {
            return groups.every(function (group) {
                var expected = group.key === exceptStyle ? candidateValue : selectedValues.get(group.key);
                return expected && valueFor(variant, group.key) === expected;
            });
        }

        function matchesThrough(variant, groupIndex, candidateValue) {
            return groups.slice(0, groupIndex).every(function (group) {
                return valueFor(variant, group.key) === selectedValues.get(group.key);
            }) && valueFor(variant, groups[groupIndex].key) === candidateValue;
        }

        function render() {
            controls.replaceChildren();
            if (groups.length === 0) {
                var simpleGroup = document.createElement('div');
                simpleGroup.className = 'variant-config-group';
                var simpleLabel = document.createElement('span');
                simpleLabel.className = 'variant-config-label';
                simpleLabel.textContent = 'Phiên bản';
                simpleGroup.appendChild(simpleLabel);
                variants.forEach(function (variant, index) {
                    var option = document.createElement('button');
                    option.type = 'button';
                    option.className = 'variant-option' + (variant.id === selectedVariant.id ? ' active' : '');
                    option.disabled = Number(variant.quantity) <= 0;
                    option.textContent = 'Cấu hình ' + (index + 1);
                    option.addEventListener('click', function () {
                        selectedVariant = variant;
                        render();
                        applyVariant(variant);
                    });
                    simpleGroup.appendChild(option);
                });
                controls.appendChild(simpleGroup);
                applyVariant(selectedVariant);
                return;
            }

            groups.forEach(function (group) {
                var groupElement = document.createElement('div');
                groupElement.className = 'variant-config-group';
                var label = document.createElement('span');
                label.className = 'variant-config-label';
                label.textContent = group.name;
                groupElement.appendChild(label);

                group.values.forEach(function (valueName, valueKey) {
                    var option = document.createElement('button');
                    option.type = 'button';
                    option.className = 'variant-option' + (selectedValues.get(group.key) === valueKey ? ' active' : '');
                    option.textContent = valueName;
                    option.setAttribute('aria-pressed', String(selectedValues.get(group.key) === valueKey));
                    option.disabled = !variants.some(function (variant) {
                        return Number(variant.quantity) > 0 && matchesThrough(variant, groups.indexOf(group), valueKey);
                    });
                    option.addEventListener('click', function () {
                        if (option.disabled) return;
                        var nextVariant = variants.find(function (variant) {
                            return Number(variant.quantity) > 0 && matches(variant, group.key, valueKey);
                        }) || variants.find(function (variant) {
                            return Number(variant.quantity) > 0 && matchesThrough(variant, groups.indexOf(group), valueKey);
                        });
                        if (!nextVariant) return;
                        selectedVariant = nextVariant;
                        groups.forEach(function (item) {
                            selectedValues.set(item.key, valueFor(nextVariant, item.key));
                        });
                        render();
                        applyVariant(selectedVariant);
                    });
                    groupElement.appendChild(option);
                });
                controls.appendChild(groupElement);
            });

            applyVariant(selectedVariant);
        }

        render();
    }
    /* =========================================================
       LOAD VARIANTS FROM API
       Dùng slug trong URL để gọi /api/products/{slug}.
       Nếu API thành công → setupVariantSwitcher với dữ liệu đầy đủ.
       Nếu API lỗi, thông báo trạng thái thay vì cho chọn nhầm phiên bản.
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
            var controls = document.getElementById('variantConfigControls');
            if (controls) controls.innerHTML = '<span class="text-danger small">Không tải được cấu hình. Vui lòng tải lại trang.</span>';
            if (btnAddToCart) btnAddToCart.disabled = true;
        });
    }

    /* =========================================================
   ADD TO CART
   ========================================================= */
    var addingToCart = false;

    function toast(title, message, type) {
        if (window.showToast) window.showToast(title, message, type);
        else alert(message);
    }

    async function addToCart(button) {
        if (addingToCart) return;

        var variantId = button.dataset.variantId;
        var count = parseInt(qtyInput ? qtyInput.value : 1, 10) || 1;

        if (!variantId) {
            toast('Thông báo', 'Vui lòng chọn phiên bản sản phẩm', 'warning');
            return;
        }

        addingToCart = true;
        try {
            var res = await fetch('/cart/items', {
                method: 'POST',
                credentials: 'same-origin',
                headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
                body: JSON.stringify({ productVariantId: variantId, count: count })
            });

            if (res.status === 401) {
                window.location.href = '/login?error=unauthorized';
                return;
            }

            var body = null;
            try { body = await res.json(); } catch (e) {}

            if (!res.ok || !body || body.success === false) {
                toast('Lỗi', (body && body.message) || 'Không thể thêm vào giỏ hàng', 'danger');
                return;
            }

            toast('Đã thêm', body.message || 'Đã thêm vào giỏ hàng', 'success');
            if (typeof refreshCartBadge === 'function') refreshCartBadge();
        } catch (e) {
            console.error(e);
            toast('Lỗi', 'Không thể kết nối máy chủ', 'danger');
        } finally {
            addingToCart = false;
        }
    }

    /* =========================================================
       CART / WISHLIST — stub handlers
       ========================================================= */

    document.body.addEventListener('click', function (event) {

        var addCartBtn = event.target.closest('.js-add-cart');

        if (addCartBtn) {
            event.preventDefault();
            event.stopPropagation();
            addToCart(addCartBtn);
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

    /* =========================================================
   ADD TO CART (trang danh sách): mặc định 1 sản phẩm,
   variant có giá cao nhất trong sản phẩm
   ========================================================= */
    (function setupListAddToCart() {
        var grid = document.getElementById('productGrid');
        if (!grid) return;

        var pending = new Set();   // chặn bấm liên tục trên cùng một sản phẩm

        function toast(title, message, type) {
            if (window.showToast) window.showToast(title, message, type);
            else alert(message);
        }

        // Giá khách thực trả: giống công thức ở product-details.js
        function effectivePrice(v) {
            var promo = Number(v.promotionalPrice);
            return promo > 0 ? promo : (Number(v.price) || 0);
        }

        // Variant còn hàng có giá cao nhất
        function pickDefaultVariant(variants) {
            var inStock = variants.filter(function (v) { return Number(v.quantity) > 0; });
            if (inStock.length === 0) return null;
            return inStock.reduce(function (best, v) {
                return effectivePrice(v) > effectivePrice(best) ? v : best;
            });
        }

        function getSlug(btn) {
            if (btn.dataset.slug) return btn.dataset.slug;
            var link = btn.closest('.product-card, article, .card')
                ?.querySelector('a[href*="/customer/products/"]');
            if (!link) return null;
            var parts = link.getAttribute('href').split('?')[0].split('/').filter(Boolean);
            return parts[parts.length - 1] || null;
        }

        async function addToCart(btn) {
            var slug = getSlug(btn);
            if (!slug) {
                toast('Lỗi', 'Không xác định được sản phẩm', 'danger');
                return;
            }
            if (pending.has(slug)) return;
            pending.add(slug);

            var oldHtml = btn.innerHTML;
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm"></span>';

            try {
                // 1) Lấy variants của sản phẩm
                var pRes = await fetch('/api/products/' + encodeURIComponent(slug), {
                    headers: { 'Accept': 'application/json' }
                });
                if (!pRes.ok) throw new Error('HTTP ' + pRes.status);
                var pBody = await pRes.json();
                var variants = pBody && pBody.success && pBody.data && pBody.data.variants;

                if (!Array.isArray(variants) || variants.length === 0) {
                    toast('Thông báo', 'Sản phẩm chưa có phiên bản để mua', 'warning');
                    return;
                }

                var variant = pickDefaultVariant(variants);
                if (!variant) {
                    toast('Thông báo', 'Sản phẩm tạm hết hàng', 'warning');
                    return;
                }

                // 2) Thêm vào giỏ, số lượng mặc định = 1
                var res = await fetch('/cart/items', {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: { 'Accept': 'application/json', 'Content-Type': 'application/json' },
                    body: JSON.stringify({ productVariantId: variant.id, count: 1 })
                });

                if (res.status === 401) {
                    window.location.href = '/login?error=unauthorized';
                    return;
                }

                var body = null;
                try { body = await res.json(); } catch (e) {}

                if (!res.ok || !body || body.success === false) {
                    toast('Lỗi', (body && body.message) || 'Không thể thêm vào giỏ hàng', 'danger');
                    return;
                }

                toast('Đã thêm', body.message || 'Đã thêm vào giỏ hàng', 'success');
                if (typeof refreshCartBadge === 'function') refreshCartBadge();
            } catch (e) {
                console.error(e);
                toast('Lỗi', 'Không thể kết nối máy chủ', 'danger');
            } finally {
                pending.delete(slug);
                btn.disabled = false;
                btn.innerHTML = oldHtml;
            }
        }

        // Delegation: vẫn chạy khi lưới sản phẩm được render lại
        grid.addEventListener('click', function (event) {
            var btn = event.target.closest('.js-add-cart');
            if (!btn || btn.disabled) return;
            event.preventDefault();
            event.stopPropagation();
            addToCart(btn);
        });
    })();


}); /* end DOMContentLoaded */
