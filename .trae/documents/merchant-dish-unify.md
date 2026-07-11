# 商家后台菜品管理改造计划

## 摘要

两项需求：
1. **移除人气标记**：商家后台不允许标记/取消标记菜品为人气菜品（仅管理员可操作）
2. **统一菜品编辑**：商家后台菜品编辑界面、字段、验证规则、功能逻辑与管理员后台完全一致

## 当前状态分析

### 商家后台菜品模态框（简化版）— [merchant.html](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/view/merchant.html#L358-L385)
- 字段：dishName, dishPrice, dishOldPrice, dishImage(URL文本), dishDescription, dishCategory(文本), dishIsDiscount(checkbox), dishIsNew(checkbox), dishIsPopular(checkbox)
- 无图片上传、无分类下拉、无口味定制、无表单错误提示
- 验证：仅 toast 提示（名称非空、价格>0）

### 管理员后台菜品模态框（完整版）— [admin.html](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/view/admin.html#L572-L697)
- 字段：dishMerchantId(select), 图片上传+预览, dishName, dishPrice, dishDesc(textarea), dishCategory(select下拉), dishIsNew(toggle), dishOnShelf(toggle), 口味定制选项组
- 无 oldPrice、无 isDiscount、无 isPopular
- 验证：字段级错误提示（errMerchant, errName, errPrice）+ invalid 样式
- 保存数据：`{ merchantId, name, price, oldPrice: null, image, description, category, isDiscount: 0, isNew, onShelf, options }`

### 后端现状
- [MerchantController.java](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/controller/MerchantController.java#L70-L74)：有 `PUT /api/merchant/dishes/{id}/popular` 端点
- [MerchantService.java](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/service/MerchantService.java#L99)：addMyDish 设置 isPopular；L121 updateMyDish 可更新 isPopular；L155-164 toggleMyDishPopular 方法
- [api.js](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/view/api.js#L138)：merchant.toggleDishPopular 方法
- MerchantService 已支持 options 字段（saveDishOptions/deleteDishOptions）

## 改造方案

### Task 1：移除人气标记功能（后端 + 前端）

#### 1.1 MerchantController.java — 删除 popular 端点
删除 L70-74：
```java
@PutMapping("/dishes/{id}/popular")
public Result<Void> toggleMyDishPopular(@PathVariable Long id) {
    merchantService.toggleMyDishPopular(id);
    return Result.success("人气标记已切换", null);
}
```

#### 1.2 MerchantService.java — 删除方法 + 禁止设置 isPopular
- **删除** L155-164 `toggleMyDishPopular` 方法
- **修改 addMyDish**（L99）：删除 `dish.setIsPopular(dto.getIsPopular() != null ? dto.getIsPopular() : 0);`，替换为 `dish.setIsPopular(0);`（强制默认0，忽略前端传入）
- **修改 updateMyDish**（L121）：删除 `if (dto.getIsPopular() != null) dish.setIsPopular(dto.getIsPopular());`（不更新 isPopular，保留原值）

#### 1.3 api.js — 删除 toggleDishPopular
删除 L138：`toggleDishPopular(id) { return request('/api/merchant/dishes/' + id + '/popular', { method: 'PUT' }); },`

#### 1.4 merchant.html — 删除 popular 相关前端（将在 Task 2 中统一处理）
- renderDishes 桌面表格删除 popular 按钮（L664）
- handleDishAction 删除 popular 分支（L701）
- 菜品模态框删除 isPopular checkbox（L377）→ Task 2 会整体替换模态框

### Task 2：统一菜品编辑功能（merchant.html 前端改造）

#### 2.1 添加缺失的 CSS 类 + 更新 img-preview
在 [merchant.html](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/view/merchant.html#L76-L83) `<style>` 中进行以下修改：

**a) 合并 `.form-input:focus` 规则**（L77）扩展为多选择器：
```css
.form-input:focus, .form-select:focus, .form-textarea:focus { border-color: var(--orange); }
```

**b) 替换 `.img-preview` 规则**（L101-103）为 admin.html 版本（大图预览）：
```css
.img-preview { width: 100%; aspect-ratio: 4/3; border-radius: 14px; overflow: hidden; background: var(--beige); display: flex; align-items: center; justify-content: center; border: 2px dashed var(--beige); cursor: pointer; transition: border-color .25s; position: relative; }
.img-preview:hover { border-color: var(--orange); }
.img-preview img { width: 100%; height: 100%; object-fit: cover; }
.img-preview .placeholder { color: var(--brown-mid); font-size: 13px; text-align: center; padding: 16px; }
```
**注意**：此变更会影响店铺 Logo/封面预览（[L203](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/view/merchant.html#L203), [L211](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/view/merchant.html#L211)），需为这两个元素添加固定宽度约束：
- `#storeLogoPreview` 添加 `style="width: 80px; flex-shrink: 0;"`
- `#storeCoverPreview` 添加 `style="width: 80px; flex-shrink: 0;"`

**c) 追加缺失的 CSS 类**（在 `.form-input` 规则后）：
```css
.form-select, .form-textarea { width: 100%; padding: 12px 16px; border: 1.5px solid var(--beige); border-radius: 14px; font-size: 14px; background: #fff; color: var(--coffee); transition: border-color .2s; outline: none; font-family: inherit; }
.form-textarea { resize: vertical; min-height: 80px; }
.form-error { color: #dc2626; font-size: 12px; margin-top: 4px; font-weight: 600; }
.form-input.invalid, .form-select.invalid, .form-textarea.invalid { border-color: #dc2626; }
.modal-box.lg { max-width: 720px; }
.option-group-card { background: #fff; border: 1px solid var(--beige); border-radius: 14px; padding: 14px; }
.option-group-head { display: flex; gap: 10px; align-items: center; margin-bottom: 10px; }
.option-choice-row { display: flex; gap: 8px; align-items: center; margin-bottom: 6px; }
.option-choice-row input { flex: 1; }
.status-new { background: var(--orange); color: #fff; }
```

#### 2.2 替换菜品模态框 HTML
将 [merchant.html L358-L385](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/view/merchant.html#L358-L385) 的 `#dishModal` 整体替换为 admin 版本（[admin.html L572-L697](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/view/admin.html#L572-L697)），**去掉 merchantId 选择器**（商家后端自动填充 merchantId）：

```html
<!-- ===== 菜品编辑/添加模态框 ===== -->
<div id="dishModal" class="modal-mask">
  <div class="modal-box lg">
    <div class="flex items-center justify-between p-6 border-b sticky top-0 bg-white z-10" style="border-color: var(--beige); border-radius: 24px 24px 0 0;">
      <h3 id="dishModalTitle" class="font-cn-display text-xl" style="color: var(--coffee);">添加菜品</h3>
      <button class="action-btn" data-close-modal="dishModal" aria-label="关闭">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
      </button>
    </div>
    <form id="dishForm" class="p-6 space-y-5" novalidate>
      <input type="hidden" id="dishId">

      <!-- 图片 -->
      <div>
        <label class="form-label">菜品图片</label>
        <div class="grid sm:grid-cols-[200px_1fr] gap-4 items-start">
          <div class="img-preview" id="imgPreview">
            <div class="placeholder">点击上传或输入图片URL</div>
          </div>
          <div class="space-y-3">
            <input type="file" id="dishImgFile" accept="image/*" class="hidden">
            <button type="button" id="uploadImgBtn" class="btn-outline text-sm py-2.5 px-4 w-full justify-center">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
              上传图片
            </button>
            <input type="text" id="dishImgUrl" class="form-input" placeholder="或输入图片URL">
          </div>
        </div>
      </div>

      <div class="grid sm:grid-cols-2 gap-4">
        <div>
          <label class="form-label">菜品名称 <span style="color:#dc2626;">*</span></label>
          <input type="text" id="dishName" class="form-input" placeholder="例如：双层芝士堡" maxlength="30">
          <p class="form-error hidden" id="errName">请输入菜品名称</p>
        </div>
        <div>
          <label class="form-label">价格（元） <span style="color:#dc2626;">*</span></label>
          <input type="number" id="dishPrice" class="form-input" placeholder="0.00" min="0" step="0.01">
          <p class="form-error hidden" id="errPrice">价格必须为大于0的数字</p>
        </div>
      </div>

      <div>
        <label class="form-label">菜品描述</label>
        <textarea id="dishDesc" class="form-textarea" placeholder="简要描述菜品特色、食材、口味..." maxlength="200"></textarea>
      </div>

      <div>
        <label class="form-label">分类</label>
        <select id="dishCategory" class="form-select">
          <option value="汉堡炸鸡">汉堡炸鸡</option>
          <option value="小吃系列">小食</option>
          <option value="川渝美食">川渝美食</option>
          <option value="面食系列">面食系列</option>
          <option value="盖饭系列">盖饭系列</option>
          <option value="日式料理">日式料理</option>
          <option value="饮料甜品">饮料甜品</option>
          <option value="冰激凌/圣代">冰激凌/圣代</option>
          <option value="沙拉轻食">沙拉轻食</option>
          <option value="饮料">饮料</option>
          <option value="套餐">套餐</option>
          <option value="其他">螺蛳粉</option>
        </select>
      </div>

      <div class="grid sm:grid-cols-2 gap-4">
        <div class="flex items-center justify-between p-4 rounded-xl" style="background: var(--beige);">
          <div>
            <p class="font-semibold text-sm" style="color: var(--coffee);">新品标签</p>
            <p class="text-xs" style="color: var(--brown-mid);">在菜品上展示「新品」</p>
          </div>
          <label class="toggle">
            <input type="checkbox" id="dishIsNew">
            <span class="slider"></span>
          </label>
        </div>
        <div class="flex items-center justify-between p-4 rounded-xl" style="background: var(--beige);">
          <div>
            <p class="font-semibold text-sm" style="color: var(--coffee);">上架状态</p>
            <p class="text-xs" style="color: var(--brown-mid);">关闭后用户不可见</p>
          </div>
          <label class="toggle">
            <input type="checkbox" id="dishOnShelf" checked>
            <span class="slider"></span>
          </label>
        </div>
      </div>

      <!-- 支持口味定制 -->
      <div class="p-4 rounded-xl" style="background: var(--beige);">
        <div class="flex items-center justify-between mb-2">
          <div>
            <p class="font-semibold text-sm" style="color: var(--coffee);">支持口味定制</p>
            <p class="text-xs" style="color: var(--brown-mid);">开启后可配置冰度、甜度、辣度等可选属性</p>
          </div>
          <label class="toggle">
            <input type="checkbox" id="dishCustomizable">
            <span class="slider"></span>
          </label>
        </div>
        <div id="dishOptionsBox" class="space-y-3" style="display:none;">
          <div id="dishOptionsList" class="space-y-3"></div>
          <button type="button" id="addOptionGroupBtn" class="btn-outline text-sm py-2 px-4 w-full justify-center">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            添加定制属性
          </button>
        </div>
      </div>

      <div class="flex gap-3 pt-2">
        <button type="button" class="btn-outline flex-1 justify-center" data-close-modal="dishModal">取消</button>
        <button type="submit" class="btn-primary flex-1 justify-center">保存菜品</button>
      </div>
    </form>
  </div>
</div>
```

#### 2.3 替换 JS 函数
将 [merchant.html L640-L749](file:///d:/KAAZI_project/kazi_takeaway_project/src/main/java/com/qiukai/view/merchant.html#L640-L749) 的 renderDishes + handleDishAction + openDishModal + saveDishBtn 整体替换为以下代码：

```javascript
  // ===== 渲染菜品列表 =====
  function renderDishes() {
    const tbody = $('#dishTableBody');
    const cardList = $('#dishCardList');
    if (dishes.length === 0) {
      tbody.innerHTML = '';
      cardList.innerHTML = '';
      $('#dishEmpty').classList.remove('hidden');
      return;
    }
    $('#dishEmpty').classList.add('hidden');

    const isOnShelf = (d) => Number(d.onShelf) === 0;
    const isNewDish = (d) => Number(d.isNew) === 1;
    const isPopularDish = (d) => Number(d.isPopular) === 1;

    // 桌面表格
    tbody.innerHTML = dishes.map(d => {
      const img = d.image || `https://picsum.photos/seed/dish${d.id}/80/80`;
      const statusPills = [];
      if (isOnShelf(d)) statusPills.push('<span class="status-pill status-green"><span class="dot"></span>上架中</span>');
      else statusPills.push('<span class="status-pill status-gray"><span class="dot"></span>已下架</span>');
      if (d.soldOut === 1) statusPills.push('<span class="sold-out-tag">售罄</span>');
      return `<tr>
        <td>
          <div class="flex items-center gap-3">
            <img src="${img}" alt="" class="w-12 h-12 rounded-xl object-cover flex-shrink-0" onerror="this.src='https://picsum.photos/seed/dish${d.id}/80/80'">
            <div class="min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <span class="font-semibold truncate" style="color: var(--coffee);">${escapeHtml(d.name)}</span>
                ${isNewDish(d) ? '<span class="status-pill status-new" style="padding:2px 8px;font-size:10px;">新品</span>' : ''}
                ${isPopularDish(d) ? '<span class="status-pill" style="padding:2px 8px;font-size:10px;background:var(--orange);color:#fff;">人气</span>' : ''}
              </div>
              <p class="text-xs truncate" style="color: var(--brown-mid);">${escapeHtml(d.description) || '—'}</p>
            </div>
          </div>
        </td>
        <td><span class="text-xs font-semibold px-2.5 py-1 rounded-full" style="background: var(--beige); color: var(--brown-mid);">${escapeHtml(d.category || '-')}</span></td>
        <td><span class="font-display text-base" style="color: var(--orange);">¥${money(d.price)}</span></td>
        <td>${d.sales || 0}</td>
        <td>${statusPills.join(' ')}</td>
        <td class="text-right"><div class="flex justify-end gap-1">
          <button class="action-btn toggle" data-action="shelf" data-id="${d.id}" title="上下架"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M3 11h18M3 11a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h18a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2M3 11v8a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-8"/></svg></button>
          <button class="action-btn toggle" data-action="soldout" data-id="${d.id}" title="售罄" ${d.soldOut === 1 ? 'style="color:var(--orange);border-color:var(--orange)"' : ''}><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/></svg></button>
          <button class="action-btn edit" data-action="edit" data-id="${d.id}" title="编辑"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg></button>
          <button class="action-btn delete" data-action="delete" data-id="${d.id}" title="删除"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg></button>
        </div></td>
      </tr>`;
    }).join('');

    // 移动端卡片
    cardList.innerHTML = dishes.map(d => {
      const img = d.image || `https://picsum.photos/seed/dish${d.id}/80/80`;
      return `<div class="bg-white rounded-2xl p-4 card-shadow flex gap-3">
        <img src="${img}" alt="" class="w-16 h-16 rounded-xl object-cover" />
        <div class="flex-1 min-w-0">
          <p class="font-semibold text-sm">${escapeHtml(d.name)}</p>
          <p class="text-xs" style="color:var(--brown-mid)">¥${money(d.price)} · 月售${d.sales||0}</p>
          <div class="flex gap-1 mt-2">
            <button class="action-btn toggle" data-action="shelf" data-id="${d.id}"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M3 11h18M3 11a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2h18a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2M3 11v8a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-8"/></svg></button>
            <button class="action-btn toggle" data-action="soldout" data-id="${d.id}"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/></svg></button>
            <button class="action-btn edit" data-action="edit" data-id="${d.id}"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg></button>
            <button class="action-btn delete" data-action="delete" data-id="${d.id}"><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg></button>
          </div>
        </div>
      </div>`;
    }).join('');
  }

  // 菜品操作事件委托
  $('#dishTableBody').addEventListener('click', handleDishAction);
  $('#dishCardList').addEventListener('click', handleDishAction);

  async function handleDishAction(e) {
    const btn = e.target.closest('[data-action]');
    if (!btn) return;
    const id = btn.dataset.id;
    const action = btn.dataset.action;
    try {
      if (action === 'shelf') { await API.merchant.toggleDishShelf(id); showToast('上下架已切换', 'success'); await loadDishes(); }
      else if (action === 'soldout') { await API.merchant.toggleDishSoldOut(id); showToast('售罄状态已切换', 'success'); await loadDishes(); }
      else if (action === 'edit') { await openDishModal(dishes.find(d => String(d.id) === id)); }
      else if (action === 'delete') {
        showConfirm({ title: '删除菜品？', msg: '删除后不可恢复', okText: '删除', danger: true, onOk: async () => {
          await API.merchant.deleteDish(id); showToast('菜品已删除', 'success'); await loadDishes();
        }});
      }
    } catch (err) { showToast(err.message || '操作失败', 'error'); }
  }

  $('#addDishBtn').addEventListener('click', () => openDishModal(null));

  // ===== 菜品模态框 =====
  async function openDishModal(dish) {
    const form = $('#dishForm');
    form.reset();
    $$('.form-error').forEach(e => e.classList.add('hidden'));
    $$('.form-input, .form-select, .form-textarea').forEach(i => i.classList.remove('invalid'));
    // 重置口味定制
    $('#dishOptionsList').innerHTML = '';
    $('#dishOptionsBox').style.display = 'none';
    $('#dishCustomizable').checked = false;
    editingDishId = dish ? dish.id : null;

    if (dish) {
      $('#dishModalTitle').textContent = '编辑菜品';
      $('#dishName').value = dish.name || '';
      $('#dishPrice').value = dish.price || '';
      $('#dishDesc').value = dish.description || '';
      $('#dishCategory').value = dish.category || '';
      $('#dishIsNew').checked = Number(dish.isNew) === 1;
      $('#dishOnShelf').checked = Number(dish.onShelf) === 0;
      $('#dishImgUrl').value = dish.image || '';
      updateImgPreview(dish.image);
      // 加载已有规格选项
      try {
        const detail = await API.menu.getDishDetail(dish.id);
        if (detail && detail.options && detail.options.length > 0) {
          window.__loadingExistingOptions = true;
          $('#dishCustomizable').checked = true;
          $('#dishOptionsBox').style.display = '';
          detail.options.forEach(g => addOptionGroup(g.name, g.optionType, g.choices));
          delete window.__loadingExistingOptions;
        }
      } catch (e) {
        console.error('加载菜品选项失败:', e);
        showToast('加载口味选项失败: ' + (e.message || e), 'error');
      }
    } else {
      $('#dishModalTitle').textContent = '添加菜品';
      updateImgPreview('');
    }
    openModal('dishModal');
  }

  // ===== 口味定制：动态表单 =====
  $('#dishCustomizable').addEventListener('change', (e) => {
    $('#dishOptionsBox').style.display = e.target.checked ? '' : 'none';
    if (window.__loadingExistingOptions) return;
    if (e.target.checked && $('#dishOptionsList').children.length === 0) {
      addOptionGroup('辣度', 'single', [{ name: '不辣', priceAdd: 0 }, { name: '微辣', priceAdd: 0 }, { name: '中辣', priceAdd: 0 }]);
    }
  });
  $('#addOptionGroupBtn').addEventListener('click', () => addOptionGroup('', 'single', []));

  function addOptionGroup(name, optionType, choices) {
    const list = $('#dishOptionsList');
    const wrap = document.createElement('div');
    wrap.className = 'option-group-card';
    wrap.innerHTML = `
      <div class="option-group-head">
        <input type="text" class="form-input opt-name" placeholder="属性名（如冰度、甜度、辣度）" value="${escapeHtml(name || '')}" style="flex:1;">
        <select class="form-select opt-type" style="width:auto;">
          <option value="single" ${optionType === 'single' ? 'selected' : ''}>单选</option>
          <option value="multi" ${optionType === 'multi' ? 'selected' : ''}>多选</option>
        </select>
        <button type="button" class="action-btn delete" title="删除该属性">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
        </button>
      </div>
      <div class="opt-choices space-y-1.5"></div>
      <button type="button" class="btn-outline text-xs py-1.5 px-3 mt-2 add-choice-btn">+ 添加选项值</button>
    `;
    list.appendChild(wrap);
    const choicesBox = wrap.querySelector('.opt-choices');
    if (choices && choices.length) {
      choices.forEach(c => addChoiceRow(choicesBox, c.name, c.priceAdd));
    } else {
      addChoiceRow(choicesBox, '', 0);
    }
    wrap.querySelector('.action-btn.delete').addEventListener('click', () => wrap.remove());
    wrap.querySelector('.add-choice-btn').addEventListener('click', () => addChoiceRow(choicesBox, '', 0));
  }

  function addChoiceRow(container, name, priceAdd) {
    const row = document.createElement('div');
    row.className = 'option-choice-row';
    row.innerHTML = `
      <input type="text" class="form-input opt-choice-name" placeholder="选项名（如少冰、中辣）" value="${escapeHtml(name || '')}">
      <input type="number" class="form-input opt-choice-price" placeholder="加价" value="${priceAdd || 0}" min="0" step="0.5" style="width:90px;">
      <span class="text-xs" style="color: var(--brown-mid);">元</span>
      <button type="button" class="action-btn delete" title="删除">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
      </button>
    `;
    container.appendChild(row);
    row.querySelector('.action-btn.delete').addEventListener('click', () => row.remove());
  }

  function collectDishOptions() {
    if (!$('#dishCustomizable').checked) return null;
    const groups = [];
    Array.from($('#dishOptionsList').children).forEach(wrap => {
      const name = wrap.querySelector('.opt-name').value.trim();
      if (!name) return;
      const optionType = wrap.querySelector('.opt-type').value;
      const choices = [];
      Array.from(wrap.querySelectorAll('.option-choice-row')).forEach(row => {
        const cName = row.querySelector('.opt-choice-name').value.trim();
        if (!cName) return;
        const cPrice = parseFloat(row.querySelector('.opt-choice-price').value) || 0;
        choices.push({ name: cName, priceAdd: cPrice });
      });
      if (choices.length === 0) return;
      groups.push({ name, optionType, choices });
    });
    return groups.length > 0 ? groups : null;
  }

  // 图片预览
  function updateImgPreview(src) {
    const preview = $('#imgPreview');
    if (src) {
      preview.innerHTML = `<img src="${escapeHtml(src)}" alt="预览">`;
    } else {
      preview.innerHTML = `<div class="placeholder">点击上传或输入图片URL</div>`;
    }
  }

  // 上传图片
  $('#uploadImgBtn').addEventListener('click', () => $('#dishImgFile').click());
  $('#dishImgFile').addEventListener('change', async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) { showToast('请选择图片文件', 'error'); return; }
    if (file.size > 5 * 1024 * 1024) { showToast('图片大小不能超过 5MB', 'error'); return; }
    showToast('正在上传图片...', '');
    try {
      const url = await API.util.uploadImage(file);
      $('#dishImgUrl').value = url;
      updateImgPreview(url);
      showToast('图片上传成功', 'success');
    } catch (err) { showToast(err.message || '图片上传失败', 'error'); }
  });
  $('#dishImgUrl').addEventListener('input', (e) => updateImgPreview(e.target.value.trim()));
  $('#imgPreview').addEventListener('click', () => $('#dishImgFile').click());

  // 表单验证
  function validateDishForm() {
    let valid = true;
    const name = $('#dishName').value.trim();
    const price = parseFloat($('#dishPrice').value);
    if (!name) {
      $('#errName').classList.remove('hidden');
      $('#dishName').classList.add('invalid');
      valid = false;
    } else {
      $('#errName').classList.add('hidden');
      $('#dishName').classList.remove('invalid');
    }
    if (!price || price <= 0 || isNaN(price)) {
      $('#errPrice').classList.remove('hidden');
      $('#dishPrice').classList.add('invalid');
      valid = false;
    } else {
      $('#errPrice').classList.add('hidden');
      $('#dishPrice').classList.remove('invalid');
    }
    return valid;
  }

  // 保存菜品
  $('#dishForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!validateDishForm()) { showToast('请检查表单填写', 'error'); return; }
    const img = $('#dishImgUrl').value.trim() || `https://picsum.photos/seed/dish${Date.now()}/400/300`;
    const customizable = $('#dishCustomizable').checked;
    const collected = customizable ? collectDishOptions() : null;
    const options = customizable ? (collected && collected.length > 0 ? collected : []) : null;
    const data = {
      name: $('#dishName').value.trim(),
      price: parseFloat($('#dishPrice').value),
      oldPrice: null,
      image: img,
      description: $('#dishDesc').value.trim(),
      category: $('#dishCategory').value,
      isDiscount: 0,
      isNew: $('#dishIsNew').checked ? 1 : 0,
      onShelf: $('#dishOnShelf').checked ? 0 : 1,
      options: options,
    };
    try {
      if (editingDishId) { await API.merchant.updateDish(editingDishId, data); showToast('菜品已更新', 'success'); }
      else { await API.merchant.addDish(data); showToast('菜品已添加', 'success'); }
      closeModal('dishModal');
      await loadDishes();
    } catch (e) { showToast('保存失败：' + e.message, 'error'); }
  });
```

**与 admin 的关键差异（合理保留）：**
- 无 merchantId 选择器（后端自动填充）
- validateDishForm 无 merchantId 验证
- 保存调用 API.merchant.addDish/updateDish（非 API.admin.*）
- renderDishes 保留售罄(soldout)按钮（商家专属功能）
- renderDishes 移除人气(popular)按钮，但保留人气标签显示

## 假设与决策

1. **oldPrice 和 isDiscount 字段**：管理员后台表单不包含这两个字段，保存时 oldPrice=null, isDiscount=0。按"完全一致"要求，商家后台也移除这两个字段。这意味着商家通过编辑表单无法设置折扣原价——与管理员后台行为一致。
2. **merchantId 字段**：管理员后台有商家选择器，商家后台不需要（后端 `getMyMerchantId()` 自动填充）。这是固有的上下文差异，不属于"编辑菜品功能"范畴。
3. **售罄(soldOut)按钮**：管理员后台无此功能，但售罄是商家专属运营功能（project_memory 中明确记录），保留在列表操作中，不影响编辑模态框的一致性。
4. **人气标签显示**：虽然商家不能标记人气，但列表中仍显示人气标签（只读），让商家了解哪些菜品被管理员标记为人气。
5. **isPopular 后端强制忽略**：addMyDish 强制 isPopular=0，updateMyDish 不更新 isPopular。即使前端伪造请求也无法设置。

## 验证步骤

1. **编译测试**：`mvn compile` 确认 Java 代码无编译错误
2. **启动服务器**：`mvn spring-boot:run` 启动应用
3. **功能验证**：
   - 商家登录 → 菜单管理 → 确认列表无"人气"操作按钮
   - 确认列表中人气菜品仍显示"人气"标签（只读）
   - 点击"添加菜品" → 确认模态框包含：图片上传、名称、价格、描述、分类下拉、新品开关、上架开关、口味定制
   - 确认模态框无：原价、折扣checkbox、人气checkbox
   - 添加菜品（含口味定制选项）→ 确认保存成功
   - 编辑菜品 → 确认口味选项正确加载
   - 表单验证：空名称、价格0 → 确认显示字段级错误
   - 图片上传 → 确认预览正常
   - 调用 `PUT /api/merchant/dishes/{id}/popular` → 确认返回 404（端点已删除）
   - 通过 API 直接发送 isPopular=1 → 确认被忽略（菜品 isPopular 仍为 0）
