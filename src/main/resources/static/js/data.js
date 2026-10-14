/* Danh mục sản phẩm được tải từ API backend; không chứa dữ liệu mẫu. */
'use strict';

var PRODUCTS = [];
var CATEGORIES = [];

const CATALOG_PLACEHOLDER = (window.APP_URL || '/') + 'images/placeholder.webp';

function catalogRequest(url) {
  return fetch(url, { headers: { Accept: 'application/json' } })
    .then(response => {
      if (!response.ok) throw new Error('HTTP ' + response.status);
      return response.json();
    })
    .then(result => {
      if (!result || result.success !== true || result.data == null) {
        throw new Error('API không trả về dữ liệu hợp lệ');
      }
      return result.data;
    });
}

function normalizeCatalogProduct(item) {
  const price = Number(item.effectivePrice ?? item.minPromotionalPrice ?? item.minPrice ?? 0);
  const oldPriceValue = Number(item.minPrice ?? price);
  const category = item.categorySlug || item.category?.slug || '';
  return {
    id: String(item.id),
    name: item.name || '',
    slug: item.slug || '',
    brand: item.brandName || item.brand?.name || '',
    category,
    price,
    oldPrice: oldPriceValue > price ? oldPriceValue : null,
    rating: Number(item.rating ?? 0),
    reviewCount: 0,
    badge: null,
    stock: Number(item.totalStock ?? 0),
    featured: false,
    bestSeller: false,
    tagline: item.description || '',
    description: item.description || '',
    highlights: [],
    specs: {},
    images: [item.thumbnailUrl || CATALOG_PLACEHOLDER],
    reviews: []
  };
}

async function loadProductCatalog() {
  try {
    const firstPage = await catalogRequest((window.APP_URL || '/') + 'api/products?page=0&size=50');
    const records = Array.isArray(firstPage.content) ? [...firstPage.content] : [];
    const pageCount = Math.max(1, Number(firstPage.totalPages || 1));

    for (let page = 1; page < pageCount; page++) {
      const next = await catalogRequest((window.APP_URL || '/') + 'api/products?page=' + page + '&size=50');
      if (Array.isArray(next.content)) records.push(...next.content);
    }

    PRODUCTS.splice(0, PRODUCTS.length, ...records.map(normalizeCatalogProduct));

    try {
      const categories = await catalogRequest((window.APP_URL || '/') + 'api/categories');
      const rows = Array.isArray(categories) ? categories : (Array.isArray(categories.content) ? categories.content : []);
      CATEGORIES.splice(0, CATEGORIES.length, ...rows
        .filter(category => category && category.slug)
        .map(category => ({
          id: category.slug,
          name: category.name || category.slug,
          icon: 'fa-tag',
          image: category.image || CATALOG_PLACEHOLDER,
          blurb: ''
        })));
    } catch (error) {
      console.warn('Không tải được danh mục; tạm suy ra từ sản phẩm.', error);
    }

    if (!CATEGORIES.length) {
      const categories = new Map();
      PRODUCTS.forEach(product => {
        if (product.category && !categories.has(product.category)) {
          categories.set(product.category, { id: product.category, name: product.category, icon: 'fa-tag', image: CATALOG_PLACEHOLDER, blurb: '' });
        }
      });
      CATEGORIES.splice(0, CATEGORIES.length, ...categories.values());
    }
  } catch (error) {
    PRODUCTS.splice(0, PRODUCTS.length);
    CATEGORIES.splice(0, CATEGORIES.length);
    console.error('Không thể tải danh mục sản phẩm từ backend.', error);
  }
}

var productCatalogReady = loadProductCatalog();
window.productCatalogReady = productCatalogReady;

function getProductById(id) {
  return PRODUCTS.find(product => String(product.id) === String(id));
}

function getCategoryName(categoryId) {
  const category = CATEGORIES.find(item => String(item.id) === String(categoryId));
  return category ? category.name : (categoryId || '');
}

function getProductsByCategory(categoryId) {
  return PRODUCTS.filter(product => String(product.category) === String(categoryId));
}

function getRelatedProducts(product, limit = 4) {
  return PRODUCTS
    .filter(item => item.category === product.category && String(item.id) !== String(product.id))
    .slice(0, limit);
}
