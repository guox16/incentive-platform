<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, reactive, ref } from 'vue';
import { http } from '../api/http';
import type { AdjustPrizeInventoryRequest, ApiError, AwardUpsertRequest, PrizeResponse, PrizeStatus, PrizeType } from '../api/types';
import AccountHeader from '../components/AccountHeader.vue';

type Scene = 'lottery' | 'redemption';
type Draft = { id?: number; name: string; type: PrizeType; status: PrizeStatus; coverUrl: string; totalStock: number; availableStock: number; awardPayload: string };

const scene = ref<Scene>('lottery');
const allPrizes = ref<PrizeResponse[]>([]);
const loading = ref(true);
const error = ref('');
const keyword = ref('');
const statusFilter = ref<'ALL' | PrizeStatus>('ALL');
const typeFilter = ref<'ALL' | PrizeType>('ALL');
const selected = ref<number[]>([]);
const drawerOpen = ref(false);
const saving = ref(false);
const formError = ref('');
const draft = reactive<Draft>({ name: '', type: 'VIRTUAL', status: 'INACTIVE', coverUrl: '', totalStock: 0, availableStock: 0, awardPayload: '' });
const inventory = reactive({ changeAmount: 0, businessNo: '', remark: '' });

const scenePrizes = computed(() => scene.value === 'lottery'
  ? allPrizes.value
  : allPrizes.value.filter(item => item.type !== 'NONE'));
const filteredPrizes = computed(() => scenePrizes.value.filter(item => {
  const search = keyword.value.trim().toLowerCase();
  return (!search || item.name.toLowerCase().includes(search) || item.code.toLowerCase().includes(search))
    && (statusFilter.value === 'ALL' || item.status === statusFilter.value)
    && (typeFilter.value === 'ALL' || item.type === typeFilter.value);
}));
const selectedAll = computed(() => filteredPrizes.value.length > 0 && filteredPrizes.value.every(item => selected.value.includes(item.id)));
const lotteryCount = computed(() => allPrizes.value.length);
const redemptionCount = computed(() => allPrizes.value.filter(item => item.type !== 'NONE').length);

function message(errorValue: unknown, fallback: string) {
  if (axios.isAxiosError(errorValue)) return (errorValue.response?.data as ApiError | undefined)?.message || fallback;
  return fallback;
}
function typeName(type: PrizeType) { return ({ VIRTUAL: '虚拟权益', POINTS: '积分奖励', NONE: '谢谢参与' })[type]; }
function statusName(status: PrizeStatus) { return ({ ACTIVE: '已上架', INACTIVE: '已下架' })[status]; }
function formattedDate(value: string) { return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(new Date(value)); }
function resetFilters() { keyword.value = ''; statusFilter.value = 'ALL'; typeFilter.value = 'ALL'; }
function selectScene(value: Scene) { scene.value = value; selected.value = []; resetFilters(); }
function toggleAll() { selected.value = selectedAll.value ? [] : filteredPrizes.value.map(item => item.id); }

async function loadPrizes() {
  loading.value = true; error.value = '';
  try { allPrizes.value = (await http.get<PrizeResponse[]>('/awards')).data; }
  catch (cause) { error.value = message(cause, '暂时无法获取奖品列表，请稍后重试。'); }
  finally { loading.value = false; }
}
function openCreate() {
  Object.assign(draft, { id: undefined, name: '', type: 'VIRTUAL', status: 'INACTIVE', coverUrl: '', totalStock: 0, availableStock: 0, awardPayload: '' });
  formError.value = ''; drawerOpen.value = true;
  Object.assign(inventory, { changeAmount: 0, businessNo: '', remark: '' });
}
function openEdit(item: PrizeResponse) {
  Object.assign(draft, { id: item.id, name: item.name, type: item.type, status: item.status, coverUrl: item.coverUrl || '', totalStock: item.totalStock, availableStock: item.availableStock, awardPayload: item.awardPayload || '' });
  formError.value = ''; drawerOpen.value = true;
  Object.assign(inventory, { changeAmount: 0, businessNo: '', remark: '' });
}
async function save() {
  if (saving.value || !draft.name.trim()) { formError.value = '请填写奖品名称。'; return; }
  if (draft.availableStock < 0) { formError.value = '可用库存不能小于 0。'; return; }
  if (draft.type === 'NONE' && (draft.availableStock !== 0 || draft.totalStock !== 0)) { formError.value = '“谢谢参与”的库存必须为 0。'; return; }
  if (draft.id && inventory.changeAmount !== 0 && !inventory.businessNo.trim()) { formError.value = '调整库存时必须填写业务号。'; return; }
  saving.value = true; formError.value = '';
  try {
    const body: AwardUpsertRequest = {
      name: draft.name.trim(), type: draft.type, status: draft.status,
      coverUrl: draft.coverUrl.trim() || null, awardPayload: draft.awardPayload.trim() || null,
      totalStock: draft.id ? draft.totalStock : Number(draft.availableStock),
      availableStock: Number(draft.availableStock),
    };
    if (draft.id) {
      await http.put(`/awards/${draft.id}`, body);
      if (inventory.changeAmount !== 0) {
        const adjustment: AdjustPrizeInventoryRequest = {
          businessNo: inventory.businessNo.trim(), changeAmount: Number(inventory.changeAmount), remark: inventory.remark.trim() || null,
        };
        await http.post(`/awards/${draft.id}/inventory-adjustments`, adjustment);
      }
    } else {
      await http.post('/awards', body);
    }
    drawerOpen.value = false; await loadPrizes();
  } catch (cause) { formError.value = message(cause, '保存失败，请检查填写内容后重试。'); }
  finally { saving.value = false; }
}
async function setStatus(ids: number[], status: PrizeStatus) {
  if (!ids.length) return;
  try {
    await Promise.all(ids.map(async id => {
      const prize = allPrizes.value.find(item => item.id === id); if (!prize) return;
      const body: AwardUpsertRequest = {
        name: prize.name, type: prize.type, status,
        coverUrl: prize.coverUrl, awardPayload: prize.awardPayload,
        totalStock: prize.totalStock, availableStock: prize.availableStock,
      };
      await http.put(`/awards/${id}`, body);
    }));
    selected.value = []; await loadPrizes();
  } catch (cause) { error.value = message(cause, '状态更新失败，请稍后重试。'); }
}
onMounted(loadPrizes);
</script>

<template>
  <!-- THESIS: 奖品主数据以双业务入口和单一可编辑清单归拢，拒绝把抽奖与兑换拆成两套重复后台。 OWN-WORLD: 深海军蓝结构、纸白工作面、钴蓝行动与稀缺暖黄场景信号。 STORY: 运营人员切换业务场景、筛选核对、批量上架，必要时打开抽屉维护奖品。 FIRST VIEWPORT: 左侧管理导航，右侧标题和新建入口，双场景卡片、过滤条及主表格连续可见。 FORM: Operate 模式，固定管理台布局。 FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, and DESIGN.md -->
  <div class="admin-page">
    <AccountHeader active="admin-prizes" />
    <main class="admin-workspace">
      <header class="page-head"><div><div class="crumb">运营管理 <i></i> 奖品管理</div><h1>奖品管理</h1><p>统一维护抽奖奖池与积分商城中可用的奖品主数据。</p></div><button class="primary-button" @click="openCreate">＋ 新建奖品</button></header>
      <section class="scene-switch" aria-label="业务场景切换"><button :class="['scene-card', { active: scene === 'lottery' }]" @click="selectScene('lottery')"><span class="scene-icon">✦</span><span><small>活动奖池</small><strong>抽奖奖品 <em>{{ lotteryCount }}</em></strong><i>用于活动奖池配置</i></span><b>›</b></button><button :class="['scene-card', { active: scene === 'redemption' }]" @click="selectScene('redemption')"><span class="scene-icon">◇</span><span><small>积分商城</small><strong>兑换商品 <em>{{ redemptionCount }}</em></strong><i>用于积分商城兑换</i></span><b>›</b></button></section>
      <section class="filter-bar"><label class="search"><span>⌕</span><input v-model="keyword" placeholder="搜索奖品名称 / 编号" /></label><select v-model="typeFilter"><option value="ALL">全部奖品类型</option><option value="VIRTUAL">虚拟权益</option><option value="POINTS">积分奖励</option><option value="NONE">谢谢参与</option></select><select v-model="statusFilter"><option value="ALL">全部状态</option><option value="ACTIVE">已上架</option><option value="INACTIVE">已下架</option></select><button class="filter-button" @click="loadPrizes">查询</button><button class="reset-button" @click="resetFilters">重置</button></section>
      <p v-if="error" class="notice error">{{ error }} <button @click="loadPrizes">重新加载</button></p>
      <section class="table-panel"><div class="table-toolbar"><span>已选 <strong>{{ selected.length }}</strong> 项</span><div><button :disabled="!selected.length" @click="setStatus(selected, 'ACTIVE')">批量上架</button><button :disabled="!selected.length" @click="setStatus(selected, 'INACTIVE')">批量下架</button></div></div>
        <div v-if="loading" class="state">正在同步奖品数据…</div><div v-else-if="!filteredPrizes.length" class="state"><strong>没有符合条件的奖品</strong><span>可调整筛选条件，或新建一个奖品。</span></div>
        <table v-else><thead><tr><th><input type="checkbox" :checked="selectedAll" @change="toggleAll" /></th><th>奖品信息</th><th>奖品类型</th><th>可用 / 总库存</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead><tbody><tr v-for="item in filteredPrizes" :key="item.id"><td><input v-model="selected" type="checkbox" :value="item.id" /></td><td><div class="prize-info"><span :class="['mini-mark', item.type.toLowerCase()]">{{ item.type === 'VIRTUAL' ? '券' : item.type === 'POINTS' ? '分' : '谢' }}</span><div><strong>{{ item.name }}</strong><small>{{ item.code }}</small></div></div></td><td>{{ typeName(item.type) }}</td><td class="number">{{ item.availableStock }} / {{ item.totalStock }}</td><td><span :class="['status', item.status.toLowerCase()]">{{ statusName(item.status) }}</span></td><td class="date">{{ formattedDate(item.updatedAt) }}</td><td class="actions"><button @click="openEdit(item)">编辑</button><button v-if="item.status !== 'ACTIVE'" @click="setStatus([item.id], 'ACTIVE')">上架</button><button v-else @click="setStatus([item.id], 'INACTIVE')">下架</button></td></tr></tbody></table>
        <footer v-if="!loading && filteredPrizes.length"><span>共 {{ filteredPrizes.length }} 项</span><span>奖品状态与库存以服务端数据为准</span></footer></section>
      <p class="page-note">下架后将停止新的抽奖或兑换配置，不影响已生成的待发奖记录；当前服务未提供抽奖/兑换归属字段，卡片按奖品可用类型切换展示。</p>
    </main>
    <div v-if="drawerOpen" class="drawer-mask" @click.self="drawerOpen = false"><aside class="drawer"><header><div><span>{{ draft.id ? '编辑奖品' : '新建奖品' }}</span><h2>{{ draft.id ? draft.name : '创建奖品主数据' }}</h2></div><button @click="drawerOpen = false">×</button></header><div class="drawer-body"><label><span>奖品名称 <b>*</b></span><input v-model="draft.name" placeholder="例如：50 元通用优惠券" /></label><div class="field-row"><label><span>奖品类型</span><select v-model="draft.type"><option value="VIRTUAL">虚拟权益</option><option value="POINTS">积分奖励</option><option value="NONE">谢谢参与</option></select></label><label><span>{{ draft.id ? '可用 / 总库存' : '初始库存' }}</span><input v-model.number="draft.availableStock" type="number" min="0" :disabled="!!draft.id" /><small v-if="draft.id">{{ draft.availableStock }} / {{ draft.totalStock }}，库存需通过调整接口变更。</small></label></div><label><span>上架状态</span><select v-model="draft.status"><option value="ACTIVE">已上架</option><option value="INACTIVE">已下架</option></select><small v-if="!draft.id">新建奖品默认下架，可确认配置后再上架。</small></label><fieldset v-if="draft.id" class="inventory-field"><legend>人工库存调整</legend><p>保存时将通过库存调整接口记录一笔幂等流水。</p><div class="field-row"><label><span>调整数量</span><input v-model.number="inventory.changeAmount" type="number" /><small>正数增加，负数减少。</small></label><label><span>业务号</span><input v-model="inventory.businessNo" placeholder="例如：ADMIN-20260812-001" /><small>同一奖品重复提交不会重复调整。</small></label></div><label><span>调整备注</span><input v-model="inventory.remark" maxlength="256" placeholder="例如：月度盘点入库" /></label></fieldset><p v-if="formError" class="notice error">{{ formError }}</p></div><footer><button class="reset-button" :disabled="saving" @click="drawerOpen = false">取消</button><button class="primary-button" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存奖品' }}</button></footer></aside></div>
  </div>
</template>

<style scoped>
.admin-page{--blue:#174fa7;--navy:#102f65;--yellow:#f1c84a;--paper:#f8f7f2;--ink:#17213a;--muted:#697287;--line:#d7dde6;min-width:1180px;min-height:100vh;padding-top:74px;color:var(--ink);background:#e8edf3;font-size:13px}.admin-page :deep(.account-header){position:fixed;inset:0 0 auto 0;z-index:8}.admin-workspace{width:min(1370px,calc(100% - 96px));margin:0 auto;padding:42px 0}.page-head{display:flex;align-items:flex-end;justify-content:space-between;padding-bottom:27px;border-bottom:1px solid var(--line)}.crumb{color:var(--muted);font-size:12px}.crumb i{display:inline-block;width:4px;height:4px;margin:0 8px 2px;background:#a6b0bf;border-radius:50%}.page-head h1{margin:11px 0 5px;font-size:34px;letter-spacing:-.025em}.page-head p{margin:0;color:var(--muted)}button{font:inherit;cursor:pointer}.primary-button,.filter-button{min-height:42px;padding:0 17px;color:#fff;background:var(--blue);border:0;border-radius:9px;font-weight:800;box-shadow:0 8px 18px rgb(23 79 167 / 18%)}.scene-switch{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px;margin:24px 0}.scene-card{display:grid;grid-template-columns:44px 1fr auto;align-items:center;gap:13px;padding:18px 20px;color:var(--ink);text-align:left;background:var(--paper);border:1px solid transparent;border-radius:16px;box-shadow:0 7px 20px rgb(38 50 75 / 7%)}.scene-card.active{border-color:#dfbd45;background:#fffdf4}.scene-icon{display:grid;width:40px;height:40px;place-items:center;color:var(--blue);background:#e2ebfa;border-radius:10px 10px 4px 10px;font-size:20px}.scene-card.active .scene-icon{color:var(--navy);background:var(--yellow)}.scene-card small,.scene-card strong,.scene-card i{display:block}.scene-card small{color:var(--blue);font-size:11px;font-weight:800}.scene-card strong{margin:2px 0;color:var(--ink);font-size:17px}.scene-card strong em{margin-left:6px;color:var(--blue);font-size:12px;font-style:normal}.scene-card i{color:var(--muted);font-size:11px;font-style:normal}.scene-card>b{color:#97a2b5;font-size:22px}.filter-bar{display:flex;gap:10px;align-items:center;padding:15px 18px;background:var(--paper);border-radius:12px;box-shadow:0 7px 20px rgb(38 50 75 / 6%)}.search{display:flex;height:38px;align-items:center;gap:8px;flex:1;max-width:360px;padding:0 12px;background:#fff;border:1px solid var(--line);border-radius:8px;color:#8290a5}.search input{width:100%;border:0;outline:0;color:var(--ink)}select,input,textarea{font:inherit}select{height:38px;padding:0 30px 0 11px;color:var(--ink);background:#fff;border:1px solid var(--line);border-radius:8px}.reset-button{min-height:38px;padding:0 13px;color:#536079;background:#e9edf2;border:0;border-radius:8px;font-weight:700}.table-panel{margin-top:16px;overflow:hidden;background:var(--paper);border-radius:16px;box-shadow:0 10px 28px rgb(38 50 75 / 9%)}.table-toolbar{display:flex;align-items:center;justify-content:space-between;padding:16px 20px;border-bottom:1px solid var(--line);color:var(--muted)}.table-toolbar strong{color:var(--blue)}.table-toolbar div{display:flex;gap:8px}.table-toolbar button,.actions button{padding:7px 10px;color:var(--blue);background:#e2ebfa;border:0;border-radius:6px;font-size:12px;font-weight:700}.table-toolbar button:disabled{color:#9aa4b4;background:#eef1f5;cursor:not-allowed}table{width:100%;border-collapse:collapse}th,td{padding:15px 13px;text-align:left;border-bottom:1px solid #e4e8ee}th{color:#697287;background:#f2f4f7;font-size:11px;font-weight:700}th:first-child,td:first-child{width:46px;padding-left:20px}.prize-info{display:flex;align-items:center;gap:10px}.mini-mark{display:grid;width:33px;height:33px;place-items:center;color:#fff;background:var(--blue);border-radius:8px 8px 3px 8px;font-weight:800}.mini-mark.points{color:var(--navy);background:var(--yellow)}.mini-mark.none{color:#687286;background:#e2e6ec}.prize-info strong,.prize-info small{display:block}.prize-info small{margin-top:3px;color:var(--muted);font-size:10px}.number{font-variant-numeric:tabular-nums}.status{display:inline-block;padding:4px 8px;border-radius:5px;font-size:11px;font-weight:700}.status.active{color:#315e3f;background:#e2efe4}.status.draft{color:#835f0c;background:#fff2c5}.status.inactive{color:#687286;background:#e6e9ee}.date{color:var(--muted);font-size:11px}.actions{display:flex;gap:7px}.actions button:last-child{color:#536079;background:#edf0f4}.table-panel footer{display:flex;justify-content:space-between;padding:14px 20px;color:var(--muted);font-size:11px}.state{display:grid;min-height:250px;place-content:center;gap:6px;color:var(--muted);text-align:center}.state strong{color:var(--ink);font-size:16px}.notice{margin:14px 0;padding:10px 12px;border-radius:8px}.notice.error{color:#a23843;background:#f8e9eb}.notice button{margin-left:7px;color:inherit;background:transparent;border:0;text-decoration:underline}.page-note{margin:18px 4px;color:var(--muted);font-size:11px}.drawer-mask{position:fixed;inset:0;z-index:10;background:rgb(16 31 56 / 40%)}.drawer{position:absolute;top:0;right:0;display:flex;width:490px;height:100%;flex-direction:column;background:var(--paper);box-shadow:-18px 0 40px rgb(16 31 56 / 18%)}.drawer header{display:flex;align-items:start;justify-content:space-between;padding:27px 30px 21px;border-bottom:1px solid var(--line)}.drawer header span{color:var(--blue);font-size:12px;font-weight:800}.drawer h2{margin:5px 0 0;font-size:22px}.drawer header button{color:var(--muted);background:transparent;border:0;font-size:28px}.drawer-body{display:grid;gap:18px;overflow:auto;padding:25px 30px}.drawer label{display:grid;gap:7px}.drawer label>span{font-size:12px;font-weight:800}.drawer label b{color:#a23843}.drawer input,.drawer textarea,.drawer select{width:100%;padding:0 11px;color:var(--ink);background:#fff;border:1px solid var(--line);border-radius:9px;outline:0;caret-color:var(--blue);transition:border-color .18s ease,box-shadow .18s ease}.drawer input{height:40px}.drawer textarea{padding:10px 11px;line-height:1.5;resize:vertical}.drawer input::placeholder,.drawer textarea::placeholder{color:var(--muted);opacity:.78}.drawer input:focus,.drawer textarea:focus,.drawer select:focus{border-color:var(--blue);box-shadow:0 0 0 3px rgb(23 79 167 / 14%)}.drawer input:disabled,.drawer textarea:disabled,.drawer select:disabled{color:var(--muted);background:#eef1f5;cursor:not-allowed}.drawer small{color:var(--muted);font-size:11px;line-height:1.5}.field-row{display:grid;grid-template-columns:1fr 1fr;gap:14px}.inventory-field{display:grid;gap:14px;margin:2px 0 0;padding:18px;border:1px solid var(--line);border-radius:12px}.inventory-field legend{padding:0 6px;color:var(--blue);font-size:12px;font-weight:800}.inventory-field>p{margin:0;color:var(--muted);font-size:11px;line-height:1.6}.drawer footer{display:flex;justify-content:flex-end;gap:10px;margin-top:auto;padding:18px 30px;border-top:1px solid var(--line)}.drawer footer .primary-button{min-width:110px}@media(prefers-reduced-motion:reduce){*{transition:none!important}}
</style>
