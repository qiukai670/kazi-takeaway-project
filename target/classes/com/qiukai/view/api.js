/**
 * 咔滋外卖 前端 API 工具库
 * 统一封装 fetch 调用、错误处理、登录态管理
 * 所有页面通过 <script src="api.js"></script> 引入
 */

const API = (function () {

  /** 统一 fetch 封装：自动带 cookie、解析 JSON、抛业务错误 */
  async function request(url, options = {}) {
    const opts = {
      credentials: 'include', // 携带 HttpOnly cookie（kazi_token）
      headers: { 'Content-Type': 'application/json' },
      ...options,
    };
    if (opts.body && typeof opts.body === 'object') {
      opts.body = JSON.stringify(opts.body);
    }
    let resp;
    try {
      resp = await fetch(url, opts);
    } catch (e) {
      throw new Error('网络异常，请稍后重试');
    }
    let data;
    try {
      data = await resp.json();
    } catch (e) {
      throw new Error('服务器响应格式错误');
    }
    if (data.code !== 200) {
      // 401 未登录 → 跳转登录页（避免在登录页本身死循环）
      if (data.code === 401 && !location.pathname.endsWith('login.html')) {
        redirectToLogin();
        throw new Error('请先登录');
      }
      throw new Error(data.message || '请求失败');
    }
    return data.data;
  }

  function redirectToLogin() {
    const current = encodeURIComponent(location.pathname.split('/').pop() + location.search);
    location.href = 'login.html?redirect=' + current;
  }

  // ==================== 认证 ====================

  const auth = {
    /** 用户登录（手机号） */
    login(account, password, rememberMe = true) {
      return request('/api/user/login', {
        method: 'POST',
        body: { account, password, rememberMe },
      });
    },

    /** 管理员登录（用户名） */
    adminLogin(account, password) {
      return request('/api/user/admin/login', {
        method: 'POST',
        body: { account, password, rememberMe: true },
      });
    },

    /** 注册 */
    register(body) {
      return request('/api/user/register', {
        method: 'POST',
        body,
      });
    },

    /** 退出登录 */
    logout() {
      return request('/api/user/logout', { method: 'POST' });
    },

    /** 获取当前登录用户信息（未登录返回 null，不触发跳转） */
    async currentUser() {
      try {
        const resp = await fetch('/api/user/info', { credentials: 'include' });
        const data = await resp.json();
        if (data.code === 200) return data.data;
        return null;
      } catch (e) {
        return null;
      }
    },

    /** 判断是否已登录（返回 boolean） */
    async isLoggedIn() {
      const u = await this.currentUser();
      return !!u;
    },

    /** 要求登录：未登录则跳转登录页（带 redirect 参数），返回是否已登录 */
    async requireLogin() {
      const u = await this.currentUser();
      if (!u) {
        redirectToLogin();
        return false;
      }
      return true;
    },
  };

  // ==================== 菜单浏览 ====================

  const menu = {
    /** 商家列表（可按分类、关键词筛选） */
    listMerchants(category, keyword) {
      const params = new URLSearchParams();
      if (category) params.set('category', category);
      if (keyword) params.set('keyword', keyword);
      const qs = params.toString();
      return request('/api/merchants' + (qs ? '?' + qs : ''));
    },

    /** 好评商家列表（首页"好评商家"板块专用） */
    listRecommendedMerchants() {
      return request('/api/merchants/recommended');
    },

    /** 商家详情（含优惠与满减规则） */
    getMerchantDetail(id) {
      return request('/api/merchant/' + id);
    },

    /** 商家菜品列表 */
    listDishes(merchantId) {
      return request('/api/dishes?merchantId=' + merchantId);
    },

    /** 菜品详情（含规格选项） */
    getDishDetail(id) {
      return request('/api/dishes/' + id);
    },

    /** 人气菜品列表（跨商家） */
    listPopularDishes() {
      return request('/api/dishes/popular');
    },
  };

  // ==================== 购物车 ====================

  const cart = {
    /** 加入购物车 */
    add(body) {
      return request('/api/cart', { method: 'POST', body });
    },

    /** 修改数量 */
    updateQty(id, qty) {
      return request('/api/cart/' + id + '/qty?qty=' + qty, { method: 'PUT' });
    },

    /** 删除单项 */
    remove(id) {
      return request('/api/cart/' + id, { method: 'DELETE' });
    },

    /** 清空指定商家购物车 */
    clear(merchantId) {
      return request('/api/cart/merchant/' + merchantId, { method: 'DELETE' });
    },

    /** 获取购物车汇总 */
    get(merchantId) {
      return request('/api/cart?merchantId=' + merchantId);
    },
  };

  // ==================== 订单 ====================

  const order = {
    /** 创建订单 */
    create(body) {
      return request('/api/orders', { method: 'POST', body });
    },

    /** 支付订单 */
    pay(id, payMethod) {
      const qs = payMethod ? '?payMethod=' + payMethod : '';
      return request('/api/orders/' + id + '/pay' + qs, { method: 'PUT' });
    },

    /** 修改留言 */
    updateNote(id, note) {
      return request('/api/orders/' + id + '/note?note=' + encodeURIComponent(note), { method: 'PUT' });
    },

    /** 确认收货 */
    confirmReceipt(id) {
      return request('/api/orders/' + id + '/confirm', { method: 'PUT' });
    },

    /** 取消订单 */
    cancel(id) {
      return request('/api/orders/' + id + '/cancel', { method: 'PUT' });
    },

    /** 用户订单列表 */
    list(status) {
      const qs = status ? '?status=' + status : '';
      return request('/api/orders' + qs);
    },

    /** 订单详情 */
    detail(id) {
      return request('/api/orders/' + id);
    },
  };

  // ==================== 用户中心 ====================

  const user = {
    /** 修改个人信息 */
    updateProfile(body) {
      return request('/api/user/profile', { method: 'PUT', body });
    },

    /** 修改头像 */
    updateAvatar(avatar) {
      return request('/api/user/avatar?avatar=' + encodeURIComponent(avatar), { method: 'PUT' });
    },

    // 银行卡
    listBankCards() { return request('/api/user/bank-cards'); },
    bindBankCard(body) { return request('/api/user/bank-cards', { method: 'POST', body }); },
    unbindBankCard(id) { return request('/api/user/bank-cards/' + id, { method: 'DELETE' }); },
    setDefaultBankCard(id) { return request('/api/user/bank-cards/' + id + '/default', { method: 'PUT' }); },

    // 收货地址
    listAddresses() { return request('/api/user/addresses'); },
    addAddress(body) { return request('/api/user/addresses', { method: 'POST', body }); },
    updateAddress(id, body) { return request('/api/user/addresses/' + id, { method: 'PUT', body }); },
    deleteAddress(id) { return request('/api/user/addresses/' + id, { method: 'DELETE' }); },

    // 收藏
    listFavorites() { return request('/api/user/favorites'); },
    addFavorite(merchantId) { return request('/api/user/favorites/' + merchantId, { method: 'POST' }); },
    removeFavorite(merchantId) { return request('/api/user/favorites/' + merchantId, { method: 'DELETE' }); },

    // 评价
    listMyReviews() { return request('/api/user/reviews'); },
    addReview(body) { return request('/api/user/reviews', { method: 'POST', body }); },
    getReview(id) { return request('/api/user/reviews/' + id); },
    updateReview(id, body) { return request('/api/user/reviews/' + id, { method: 'PUT', body }); },
    deleteReview(id) { return request('/api/user/reviews/' + id, { method: 'DELETE' }); },
  };

  // ==================== 后台管理 ====================

  const admin = {
    // 商家管理
    listMerchants() { return request('/api/admin/merchants'); },
    addMerchant(body) { return request('/api/admin/merchants', { method: 'POST', body }); },
    updateMerchant(id, body) { return request('/api/admin/merchants/' + id, { method: 'PUT', body }); },
    deleteMerchant(id) { return request('/api/admin/merchants/' + id, { method: 'DELETE' }); },
    toggleMerchantRecommended(id) { return request('/api/admin/merchants/' + id + '/recommended', { method: 'PUT' }); },
    batchToggleMerchantRecommended(ids, isRecommended) { return request('/api/admin/merchants/batch-recommended', { method: 'PUT', body: { ids, isRecommended } }); },

    // 菜品管理
    listDishes(merchantId) {
      const qs = merchantId ? '?merchantId=' + merchantId : '';
      return request('/api/admin/dishes' + qs);
    },
    addDish(body) { return request('/api/admin/dishes', { method: 'POST', body }); },
    updateDish(id, body) { return request('/api/admin/dishes/' + id, { method: 'PUT', body }); },
    deleteDish(id) { return request('/api/admin/dishes/' + id, { method: 'DELETE' }); },
    toggleShelf(id) { return request('/api/admin/dishes/' + id + '/shelf', { method: 'PUT' }); },
    togglePopular(id) { return request('/api/admin/dishes/' + id + '/popular', { method: 'PUT' }); },

    // 订单管理
    listOrders(status) {
      const qs = status ? '?status=' + status : '';
      return request('/api/admin/orders' + qs);
    },
    orderDetail(id) { return request('/api/admin/orders/' + id); },
    assignOrder(id) { return request('/api/admin/orders/' + id + '/assign', { method: 'PUT' }); },
    confirmOrder(id) { return request('/api/admin/orders/' + id + '/confirm', { method: 'PUT' }); },
    dispatchOrder(id) { return request('/api/admin/orders/' + id + '/dispatch', { method: 'PUT' }); },
  };

  // ==================== 工具方法 ====================

  const util = {
    /** 获取 URL 查询参数 */
    getParam(name) {
      return new URLSearchParams(location.search).get(name);
    },

    /** 金额格式化：保留 2 位小数 */
    money(n) {
      if (n == null) return '0.00';
      return Number(n).toFixed(2);
    },

    /** 通用 Toast 提示 */
    toast(msg, type = '') {
      const existing = document.querySelector('.api-toast');
      if (existing) existing.remove();
      const t = document.createElement('div');
      t.className = 'api-toast ' + type;
      const bg = type === 'success' ? 'var(--green,#00704f)' : (type === 'error' ? '#dc2626' : 'var(--coffee,#7d2c0f)');
      t.style.cssText = 'position:fixed;left:50%;top:90px;transform:translateX(-50%) translateY(-20px);background:' + bg + ';color:#fff;padding:12px 24px;border-radius:100px;font-size:14px;font-weight:600;box-shadow:0 12px 32px -8px rgba(0,0,0,.3);z-index:99999;opacity:0;transition:all .35s cubic-bezier(.22,1,.36,1);white-space:nowrap;max-width:90vw;';
      t.textContent = msg;
      document.body.appendChild(t);
      requestAnimationFrame(() => { t.style.opacity = '1'; t.style.transform = 'translateX(-50%) translateY(0)'; });
      setTimeout(() => { t.style.opacity = '0'; t.style.transform = 'translateX(-50%) translateY(-20px)'; setTimeout(() => t.remove(), 400); }, 2200);
    },

    /** HTML 转义，防止 XSS */
    escapeHtml(str) {
      if (str == null) return '';
      return String(str).replace(/[&<>"']/g, m => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[m]));
    },

    /** 格式化日期时间 */
    formatTime(dt) {
      if (!dt) return '';
      // 兼容 LocalDateTime 序列化的数组格式 [y,m,d,h,m,s]
      if (Array.isArray(dt)) {
        const [y, m, d, h = 0, mi = 0] = dt;
        return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(mi).padStart(2, '0')}`;
      }
      return String(dt).replace('T', ' ').substring(0, 16);
    },

    /**
     * 上传图片文件到服务器
     * @param {File} file - 用户选择的图片文件
     * @returns {Promise<string>} 服务器返回的图片访问路径，如 /uploads/xxx.png
     */
    async uploadImage(file) {
      const formData = new FormData();
      formData.append('file', file);
      let resp;
      try {
        resp = await fetch('/api/upload', {
          method: 'POST',
          body: formData,
          credentials: 'include',
        });
      } catch (e) {
        throw new Error('网络异常，请稍后重试');
      }
      let data;
      try {
        data = await resp.json();
      } catch (e) {
        throw new Error('服务器响应格式错误');
      }
      if (data.code !== 200) {
        throw new Error(data.message || '上传失败');
      }
      return data.data;
    },
  };

  return { auth, menu, cart, order, user, admin, util, request };
})();
