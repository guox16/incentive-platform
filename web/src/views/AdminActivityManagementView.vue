<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, reactive, ref } from 'vue';
import { http } from '../api/http';
import type { ActivityStatus, AdminActivityResponse, ApiError, CreateActivityRequest, LotteryPreDrawRule, LotteryPreDrawRuleType, UpdateActivityRequest } from '../api/types';
import AccountHeader from '../components/AccountHeader.vue';

type ManageableType = 'LOTTERY' | 'REDEMPTION';
type PrizeValueDraft = { prizeId: string; value: number };
type PointsTierDraft = { minimumPoints: number; multipliers: PrizeValueDraft[] };
type RuleDraft = {
  type: LotteryPreDrawRuleType;
  enabled: boolean;
  userIds: string;
  unlocks: PrizeValueDraft[];
  tiers: PointsTierDraft[];
};
type Draft = {
  id?: number; code: string; name: string; type: ManageableType; status: ActivityStatus;
  startsAt: string; endsAt: string; pointsCost: number; dailyLimit: number | null;
  luckyPrizeId: number | null | ''; preDrawRules: RuleDraft[];
};

const ruleOptions: Array<{ type: LotteryPreDrawRuleType; label: string }> = [
  { type: 'USER_LIST', label: '名单规则' },
  { type: 'PRIZE_UNLOCK', label: '奖品解锁' },
  { type: 'POINTS_WEIGHT', label: '积分权重' }
];
const activities = ref<AdminActivityResponse[]>([]);
const loading = ref(true);
const saving = ref(false);
const error = ref('');
const formError = ref('');
const keyword = ref('');
const typeFilter = ref<'ALL' | ManageableType>('ALL');
const statusFilter = ref<'ALL' | ActivityStatus>('ALL');
const drawerOpen = ref(false);
const selectedRuleType = ref<LotteryPreDrawRuleType>('USER_LIST');
const draft = reactive<Draft>(blankDraft());

const filteredActivities = computed(() => activities.value.filter(item => {
  const search = keyword.value.trim().toLowerCase();
  return (!search || item.name.toLowerCase().includes(search) || item.code.toLowerCase().includes(search))
    && (typeFilter.value === 'ALL' || item.type === typeFilter.value)
    && (statusFilter.value === 'ALL' || item.status === statusFilter.value);
}));
const activeCount = computed(() => activities.value.filter(item => item.status === 'ACTIVE').length);
const scheduledCount = computed(() => activities.value.filter(item => new Date(item.startsAt) > new Date()).length);
const availableRuleOptions = computed(() => ruleOptions.filter(option =>
  !draft.preDrawRules.some(rule => rule.type === option.type)));

function blankDraft(): Draft {
  const now = new Date();
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  return { code: '', name: '', type: 'LOTTERY', status: 'DRAFT', startsAt: now.toISOString().slice(0, 16),
    endsAt: '', pointsCost: 0, dailyLimit: null, luckyPrizeId: null, preDrawRules: [] };
}
function blankRule(type: LotteryPreDrawRuleType): RuleDraft {
  return {
    type, enabled: true, userIds: '',
    unlocks: type === 'PRIZE_UNLOCK' ? [{ prizeId: '', value: 1 }] : [],
    tiers: type === 'POINTS_WEIGHT'
      ? [{ minimumPoints: 0, multipliers: [{ prizeId: '', value: 1 }] }]
      : []
  };
}
function ruleFromApi(rule: LotteryPreDrawRule): RuleDraft {
  const result = blankRule(rule.type);
  result.enabled = rule.enabled;
  if (rule.type === 'USER_LIST') {
    result.userIds = rule.userIds.join('\n');
  } else if (rule.type === 'PRIZE_UNLOCK') {
    result.unlocks = Object.entries(rule.prizeMinimumDrawCounts)
      .map(([prizeId, value]) => ({ prizeId, value: Number(value) }));
  } else {
    result.tiers = rule.pointsTiers.map(tier => {
      return { minimumPoints: Number(tier.minimumPoints), multipliers: Object.entries(tier.weightMultipliers)
        .map(([prizeId, multiplier]) => ({ prizeId, value: Number(multiplier) })) };
    });
  }
  return result;
}
function message(value: unknown, fallback: string) {
  if (axios.isAxiosError(value)) return (value.response?.data as ApiError | undefined)?.message || fallback;
  return fallback;
}
function typeName(type: string) { return type === 'LOTTERY' ? '抽奖活动' : '兑换活动'; }
function ruleName(type: LotteryPreDrawRuleType) { return ruleOptions.find(item => item.type === type)?.label || type; }
function statusName(status: ActivityStatus) { return ({ DRAFT: '草稿', ACTIVE: '进行中', PAUSED: '已暂停', ENDED: '已结束' })[status]; }
function formatDate(value: string | null) {
  if (!value) return '长期有效';
  return new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(new Date(value));
}
function toLocalDate(value: string | null) {
  if (!value) return '';
  const date = new Date(value); date.setMinutes(date.getMinutes() - date.getTimezoneOffset());
  return date.toISOString().slice(0, 16);
}
function resetFilters() { keyword.value = ''; typeFilter.value = 'ALL'; statusFilter.value = 'ALL'; }
function openCreate() { Object.assign(draft, blankDraft()); formError.value = ''; drawerOpen.value = true; }
function openEdit(item: AdminActivityResponse) {
  Object.assign(draft, { id: item.id, code: item.code, name: item.name, type: item.type,
    status: item.status, startsAt: toLocalDate(item.startsAt), endsAt: toLocalDate(item.endsAt),
    pointsCost: item.pointsCost, dailyLimit: item.dailyLimit, luckyPrizeId: item.luckyPrizeId,
    preDrawRules: item.preDrawRules.map(ruleFromApi) });
  formError.value = ''; drawerOpen.value = true;
}
function addRule() {
  if (!availableRuleOptions.value.length) return;
  const type = availableRuleOptions.value.some(option => option.type === selectedRuleType.value)
    ? selectedRuleType.value : availableRuleOptions.value[0].type;
  draft.preDrawRules.push(blankRule(type));
  selectedRuleType.value = availableRuleOptions.value[0]?.type || 'USER_LIST';
}
function removeRule(index: number) { draft.preDrawRules.splice(index, 1); }
function moveRule(index: number, direction: -1 | 1) {
  const target = index + direction;
  if (target < 0 || target >= draft.preDrawRules.length) return;
  const [rule] = draft.preDrawRules.splice(index, 1);
  draft.preDrawRules.splice(target, 0, rule);
}
function addUnlock(rule: RuleDraft) { rule.unlocks.push({ prizeId: '', value: 1 }); }
function addTier(rule: RuleDraft) { rule.tiers.push({ minimumPoints: 0, multipliers: [{ prizeId: '', value: 1 }] }); }
function addMultiplier(tier: PointsTierDraft) { tier.multipliers.push({ prizeId: '', value: 1 }); }
async function loadActivities() {
  loading.value = true; error.value = '';
  try { activities.value = (await http.get<AdminActivityResponse[]>('/activities/admin')).data; }
  catch (cause) { error.value = message(cause, '暂时无法获取活动列表，请稍后重试。'); }
  finally { loading.value = false; }
}
function parsedUserIds(value: string) {
  return value.split(/[\s,，]+/).map(item => item.trim()).filter(Boolean).map(Number);
}
function validatePrizeValues(values: PrizeValueDraft[], valueName: string) {
  if (!values.length) return `请至少配置一项${valueName}。`;
  const ids = new Set<string>();
  for (const item of values) {
    if (!/^\d+$/.test(item.prizeId) || Number(item.prizeId) <= 0) return '奖品 ID 必须是正整数。';
    if (ids.has(item.prizeId)) return `奖品 ID ${item.prizeId} 不能重复。`;
    ids.add(item.prizeId);
    if (!Number.isFinite(item.value) || item.value <= 0) return `${valueName}必须大于 0。`;
  }
  return '';
}
function validate() {
  if (!draft.name.trim() || (!draft.id && !draft.code.trim())) return '请填写活动名称与唯一编码。';
  if (!draft.startsAt) return '请选择活动开始时间。';
  if (draft.endsAt && new Date(draft.endsAt) <= new Date(draft.startsAt)) return '结束时间必须晚于开始时间。';
  if (draft.pointsCost < 0) return '积分成本不能小于 0。';
  if (draft.dailyLimit !== null && draft.dailyLimit < 1) return '每日参与上限至少为 1，留空表示不限。';
  if (draft.type !== 'LOTTERY') return '';
  if (draft.luckyPrizeId !== null && draft.luckyPrizeId !== '' && draft.luckyPrizeId <= 0) return '幸运奖奖品 ID 必须是正整数。';
  for (const rule of draft.preDrawRules) {
    if (rule.type === 'USER_LIST') {
      const ids = parsedUserIds(rule.userIds);
      if (!ids.length || ids.some(id => !Number.isSafeInteger(id) || id <= 0)) return '名单规则需要填写有效的用户 ID。';
    } else if (rule.type === 'PRIZE_UNLOCK') {
      const issue = validatePrizeValues(rule.unlocks, '解锁次数'); if (issue) return issue;
      if (rule.unlocks.some(item => !Number.isInteger(item.value))) return '解锁次数必须是整数。';
    } else {
      if (!rule.tiers.length) return '积分权重规则至少需要一个积分档位。';
      const minimums = new Set<number>();
      for (const tier of rule.tiers) {
        if (!Number.isInteger(tier.minimumPoints) || tier.minimumPoints < 0) return '积分门槛必须是非负整数。';
        if (minimums.has(tier.minimumPoints)) return `积分门槛 ${tier.minimumPoints} 不能重复。`;
        minimums.add(tier.minimumPoints);
        const issue = validatePrizeValues(tier.multipliers, '权重倍数'); if (issue) return issue;
      }
    }
  }
  return '';
}
function serializeRule(rule: RuleDraft): LotteryPreDrawRule {
  const serialized: LotteryPreDrawRule = {
    type: rule.type,
    enabled: rule.enabled,
    userIds: [],
    prizeMinimumDrawCounts: {},
    pointsTiers: []
  };
  if (rule.type === 'USER_LIST') {
    serialized.userIds = parsedUserIds(rule.userIds);
  } else if (rule.type === 'PRIZE_UNLOCK') {
    serialized.prizeMinimumDrawCounts = Object.fromEntries(
      rule.unlocks.map(item => [item.prizeId, Number(item.value)]));
  } else {
    serialized.pointsTiers = rule.tiers.map(tier => ({ minimumPoints: Number(tier.minimumPoints),
      weightMultipliers: Object.fromEntries(
        tier.multipliers.map(item => [item.prizeId, Number(item.value)])) }));
  }
  return serialized;
}
async function save() {
  if (saving.value) return;
  formError.value = validate(); if (formError.value) return;
  saving.value = true;
  const lotteryRules = draft.type === 'LOTTERY' ? draft.preDrawRules.map(serializeRule) : [];
  const common = { name: draft.name.trim(), startsAt: new Date(draft.startsAt).toISOString(),
    endsAt: draft.endsAt ? new Date(draft.endsAt).toISOString() : null, pointsCost: Number(draft.pointsCost),
    dailyLimit: draft.dailyLimit === null ? null : Number(draft.dailyLimit),
    luckyPrizeId: draft.type === 'LOTTERY' && draft.luckyPrizeId !== '' ? draft.luckyPrizeId : null,
    preDrawRules: lotteryRules };
  try {
    if (draft.id) {
      const body: UpdateActivityRequest = { ...common, status: draft.status };
      await http.put(`/activities/admin/${draft.id}`, body);
    } else {
      const body: CreateActivityRequest = { ...common, code: draft.code.trim().toUpperCase(), type: draft.type };
      await http.post('/activities/admin', body);
    }
    drawerOpen.value = false; await loadActivities();
  } catch (cause) { formError.value = message(cause, '保存失败，请检查填写内容后重试。'); }
  finally { saving.value = false; }
}
async function setStatus(item: AdminActivityResponse, status: ActivityStatus) {
  try {
    const body: UpdateActivityRequest = { name: item.name, status, startsAt: item.startsAt, endsAt: item.endsAt,
      pointsCost: item.pointsCost, dailyLimit: item.dailyLimit, luckyPrizeId: item.luckyPrizeId,
      preDrawRules: item.preDrawRules.map(rule => ({
        type: rule.type, enabled: rule.enabled, userIds: rule.userIds,
        prizeMinimumDrawCounts: rule.prizeMinimumDrawCounts,
        pointsTiers: rule.pointsTiers })) };
    await http.put(`/activities/admin/${item.id}`, body); await loadActivities();
  } catch (cause) { error.value = message(cause, '状态更新失败，请稍后重试。'); }
}
onMounted(loadActivities);
</script>

<template>
  <div class="admin-page">
    <AccountHeader active="admin-activities" />
    <main class="admin-workspace">
      <header class="page-head">
        <div><p class="crumb">运营管理 <span></span> 活动管理</p><h1>活动管理</h1><p>配置抽奖与兑换活动的周期、参与成本和每日限额。</p></div>
        <button class="primary-button" type="button" @click="openCreate">
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14M5 12h14" /></svg>新建活动
        </button>
      </header>

      <section class="overview" aria-label="活动概览">
        <div><span>全部活动</span><strong>{{ activities.length }}</strong><small>抽奖与兑换活动</small></div>
        <div><span>正在进行</span><strong>{{ activeCount }}</strong><small>状态已设为进行中</small></div>
        <div><span>等待开始</span><strong>{{ scheduledCount }}</strong><small>开始时间尚未到达</small></div>
        <p>活动进入“进行中”后，仍需处于设定周期内，用户端才会展示并允许参与。</p>
      </section>

      <section class="filter-bar" aria-label="活动筛选">
        <label class="search"><svg viewBox="0 0 24 24"><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 4 4"/></svg><input v-model="keyword" aria-label="搜索活动" placeholder="搜索活动名称 / 编码" /></label>
        <select v-model="typeFilter" aria-label="活动类型"><option value="ALL">全部活动类型</option><option value="LOTTERY">抽奖活动</option><option value="REDEMPTION">兑换活动</option></select>
        <select v-model="statusFilter" aria-label="活动状态"><option value="ALL">全部状态</option><option value="DRAFT">草稿</option><option value="ACTIVE">进行中</option><option value="PAUSED">已暂停</option><option value="ENDED">已结束</option></select>
        <button class="secondary-button" type="button" @click="resetFilters">重置筛选</button>
      </section>

      <p v-if="error" class="notice" role="alert">{{ error }} <button type="button" @click="loadActivities">重新加载</button></p>
      <section v-if="!error" class="table-panel" aria-live="polite">
        <div v-if="loading" class="state">正在同步活动数据…</div>
        <div v-else-if="!filteredActivities.length" class="state"><strong>没有符合条件的活动</strong><span>调整筛选条件，或创建一个新活动。</span></div>
        <table v-else>
          <thead><tr><th>活动信息</th><th>活动周期</th><th>参与规则</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead>
          <tbody><tr v-for="item in filteredActivities" :key="item.id">
            <td><div class="activity-info"><span :class="['type-mark', item.type.toLowerCase()]"><svg v-if="item.type === 'LOTTERY'" viewBox="0 0 24 24"><path d="M4 10h16v10H4zM3 7h18v3H3zM12 7v13M7.5 7C5 7 5 3.5 7.5 3.5c2 0 4.5 3.5 4.5 3.5s2.5-3.5 4.5-3.5C19 3.5 19 7 16.5 7"/></svg><svg v-else viewBox="0 0 24 24"><path d="M5 8h14l-1 12H6L5 8Z"/><path d="M9 9V6a3 3 0 0 1 6 0v3"/></svg></span><div><strong>{{ item.name }}</strong><small>{{ item.code }} · {{ typeName(item.type) }}</small></div></div></td>
            <td class="period"><strong>{{ formatDate(item.startsAt) }}</strong><small>至 {{ formatDate(item.endsAt) }}</small></td>
            <td><div class="rule"><strong>{{ item.type === 'LOTTERY' ? `${item.pointsCost} 积分 / 次` : '按商品定价' }}</strong><small>{{ item.dailyLimit ? `每日最多 ${item.dailyLimit} 次` : '每日不限次数' }} · v{{ item.ruleVersion }}</small></div></td>
            <td><span :class="['status', item.status.toLowerCase()]">{{ statusName(item.status) }}</span></td>
            <td class="date">{{ formatDate(item.updatedAt) }}</td>
            <td class="actions"><button type="button" @click="openEdit(item)">编辑</button><button v-if="item.status !== 'ACTIVE'" type="button" @click="setStatus(item, 'ACTIVE')">启用</button><button v-else type="button" @click="setStatus(item, 'PAUSED')">暂停</button></td>
          </tr></tbody>
        </table>
        <footer v-if="!loading && filteredActivities.length"><span>共 {{ filteredActivities.length }} 项</span><span>参与规则发生变化时自动保留新版本</span></footer>
      </section>
    </main>

    <div v-if="drawerOpen" class="drawer-mask" @click.self="drawerOpen = false">
      <aside class="drawer" role="dialog" aria-modal="true" aria-labelledby="activity-drawer-title">
        <header><div><span>{{ draft.id ? '编辑活动' : '新建活动' }}</span><h2 id="activity-drawer-title">{{ draft.id ? draft.name : '创建活动与首版规则' }}</h2></div><button type="button" aria-label="关闭" @click="drawerOpen = false"><svg viewBox="0 0 24 24"><path d="m6 6 12 12M18 6 6 18"/></svg></button></header>
        <div class="drawer-body">
          <label><span>活动名称 <b>*</b></span><input v-model="draft.name" maxlength="100" placeholder="例如：夏日幸运抽奖" /></label>
          <div class="field-row"><label><span>活动编码 <b>*</b></span><input v-model="draft.code" :disabled="!!draft.id" maxlength="64" placeholder="SUMMER_DRAW" /><small>仅支持大写字母、数字与下划线</small></label><label><span>活动类型</span><select v-model="draft.type" :disabled="!!draft.id"><option value="LOTTERY">抽奖活动</option><option value="REDEMPTION">兑换活动</option></select></label></div>
          <div class="field-row"><label><span>开始时间 <b>*</b></span><input v-model="draft.startsAt" type="datetime-local" /></label><label><span>结束时间</span><input v-model="draft.endsAt" type="datetime-local" /><small>留空表示长期有效</small></label></div>
          <label v-if="draft.id"><span>活动状态</span><select v-model="draft.status"><option value="DRAFT">草稿</option><option value="ACTIVE">进行中</option><option value="PAUSED">已暂停</option><option value="ENDED">已结束</option></select></label>
          <fieldset><legend>参与规则</legend><p>规则字段修改后将创建新版本，已有参与记录继续关联原版本。</p><div class="field-row"><label><span>单次积分成本</span><input v-model.number="draft.pointsCost" type="number" min="0" :disabled="draft.type === 'REDEMPTION'" /><small>{{ draft.type === 'REDEMPTION' ? '兑换积分由具体商品决定' : '设为 0 表示免费参与' }}</small></label><label><span>每日参与上限</span><input v-model.number="draft.dailyLimit" type="number" min="1" placeholder="不限" /><small>留空表示不限次数</small></label></div>
            <div v-if="draft.type === 'LOTTERY'" class="rule-builder">
              <label><span>幸运奖奖品 ID</span><input v-model.number="draft.luckyPrizeId" type="number" min="1" placeholder="默认使用 NONE 类型奖品" /><small>所有候选奖品均被过滤时使用；留空则自动选择 NONE 类型奖品。</small></label>
              <div class="rule-builder-head"><div><strong>前置责任链</strong><small>从上到下执行，名单命中后会立即结束前置处理。</small></div><div><select v-model="selectedRuleType" :disabled="!availableRuleOptions.length" aria-label="待添加的规则类型"><option v-for="option in availableRuleOptions" :key="option.type" :value="option.type">{{ option.label }}</option></select><button class="add-rule" type="button" :disabled="!availableRuleOptions.length" @click="addRule">添加规则</button></div></div>
              <div v-if="!draft.preDrawRules.length" class="rule-empty"><strong>当前使用基础奖池</strong><span>按需要加入名单、解锁或积分权重规则。</span></div>
              <section v-for="(rule, ruleIndex) in draft.preDrawRules" :key="rule.type" class="rule-editor">
                <header><div><strong>{{ ruleName(rule.type) }}</strong><small>执行顺序 {{ ruleIndex + 1 }}</small></div><label class="rule-switch"><input v-model="rule.enabled" type="checkbox" /><span>启用</span></label><div class="rule-actions"><button type="button" :disabled="ruleIndex === 0" aria-label="上移规则" @click="moveRule(ruleIndex, -1)">上移</button><button type="button" :disabled="ruleIndex === draft.preDrawRules.length - 1" aria-label="下移规则" @click="moveRule(ruleIndex, 1)">下移</button><button type="button" @click="removeRule(ruleIndex)">移除</button></div></header>
                <label v-if="rule.type === 'USER_LIST'"><span>名单用户 ID</span><textarea v-model="rule.userIds" rows="4" placeholder="每行一个用户 ID，也支持逗号分隔"></textarea><small>名单中的用户直接获得当前基础权重最大的候选奖品。</small></label>
                <div v-else-if="rule.type === 'PRIZE_UNLOCK'" class="rule-values"><div class="value-head"><span>奖品解锁条件</span><button type="button" @click="addUnlock(rule)">添加条件</button></div><div v-for="(item, itemIndex) in rule.unlocks" :key="itemIndex" class="value-row"><label><span>奖品 ID</span><input v-model="item.prizeId" inputmode="numeric" placeholder="例如 101" /></label><label><span>第几次抽奖解锁</span><input v-model.number="item.value" type="number" min="1" /></label><button type="button" aria-label="删除解锁条件" @click="rule.unlocks.splice(itemIndex, 1)">删除</button></div></div>
                <div v-else class="points-tiers"><div class="value-head"><span>积分档位</span><button type="button" @click="addTier(rule)">添加档位</button></div><section v-for="(tier, tierIndex) in rule.tiers" :key="tierIndex" class="tier"><header><label><span>最低消耗积分</span><input v-model.number="tier.minimumPoints" type="number" min="0" /></label><button type="button" @click="rule.tiers.splice(tierIndex, 1)">删除档位</button></header><div class="value-head"><span>奖品权重倍数</span><button type="button" @click="addMultiplier(tier)">添加奖品</button></div><div v-for="(item, itemIndex) in tier.multipliers" :key="itemIndex" class="value-row"><label><span>奖品 ID</span><input v-model="item.prizeId" inputmode="numeric" placeholder="例如 101" /></label><label><span>权重倍数</span><input v-model.number="item.value" type="number" min="0.01" step="0.01" /></label><button type="button" aria-label="删除权重配置" @click="tier.multipliers.splice(itemIndex, 1)">删除</button></div></section></div>
              </section>
            </div>
          </fieldset>
          <p v-if="formError" class="notice" role="alert">{{ formError }}</p>
        </div>
        <footer><button class="secondary-button" type="button" :disabled="saving" @click="drawerOpen = false">取消</button><button class="primary-button" type="button" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存活动' }}</button></footer>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.admin-page{--blue:#174fa7;--navy:#102f65;--yellow:#f1c84a;--paper:#f8f7f2;--ink:#17213a;--muted:#697287;--line:#d7dde6;min-width:1180px;min-height:100vh;padding-top:74px;color:var(--ink);background:#e8edf3;font-size:13px}.admin-page :deep(.account-header){position:fixed;inset:0 0 auto;z-index:8}.admin-workspace{width:min(1370px,calc(100% - 96px));margin:auto;padding:38px 0}.page-head{display:flex;align-items:flex-end;justify-content:space-between;padding-bottom:24px;border-bottom:1px solid var(--line)}.page-head h1{margin:9px 0 4px;font-size:34px;letter-spacing:-.025em}.page-head p{margin:0;color:var(--muted)}.crumb{font-size:12px}.crumb span{display:inline-block;width:4px;height:4px;margin:0 8px 2px;background:#a6b0bf;border-radius:50%}button,input,select,textarea{font:inherit}button{cursor:pointer}.primary-button,.secondary-button{display:inline-flex;min-height:42px;align-items:center;justify-content:center;gap:8px;padding:0 17px;border:0;border-radius:9px;font-weight:800}.primary-button{color:#fff;background:var(--blue);box-shadow:0 8px 18px rgb(23 79 167 / 18%)}.primary-button svg{width:17px;fill:none;stroke:currentColor;stroke-width:2;stroke-linecap:round}.secondary-button{color:#536079;background:#e9edf2}.overview{display:grid;grid-template-columns:repeat(3,170px) 1fr;gap:0;margin:22px 0 16px;padding:18px 22px;background:var(--navy);border-radius:16px;box-shadow:0 12px 30px rgb(31 54 94 / 14%)}.overview>div{display:grid;padding:0 22px;border-right:1px solid rgb(255 255 255 / 16%)}.overview>div:first-child{padding-left:0}.overview span,.overview small{color:#aebfdb}.overview strong{color:#fff;font-size:26px;line-height:1.3;font-variant-numeric:tabular-nums}.overview small{font-size:10px}.overview p{align-self:center;max-width:54ch;margin:0 0 0 28px;color:#d5e0f1;line-height:1.7}.filter-bar{display:flex;align-items:center;gap:10px;padding:14px 18px;background:var(--paper);border-radius:12px;box-shadow:0 7px 20px rgb(38 50 75 / 6%)}.search{display:flex;height:40px;align-items:center;gap:8px;flex:1;max-width:390px;padding:0 12px;background:#fff;border:1px solid var(--line);border-radius:8px}.search svg{width:17px;fill:none;stroke:#768297;stroke-width:1.8;stroke-linecap:round}.search input{width:100%;border:0;outline:0;color:var(--ink)}select,input,textarea{color:var(--ink);background:#fff;border:1px solid var(--line);border-radius:8px}select{height:40px;padding:0 32px 0 11px}.table-panel{margin-top:16px;overflow:hidden;background:var(--paper);border-radius:16px;box-shadow:0 10px 28px rgb(38 50 75 / 9%)}table{width:100%;border-collapse:collapse}th,td{padding:15px 13px;text-align:left;border-bottom:1px solid #e4e8ee}th{color:var(--muted);background:#f2f4f7;font-size:11px}th:first-child,td:first-child{padding-left:20px}.activity-info{display:flex;align-items:center;gap:11px}.type-mark{display:grid;width:38px;height:38px;place-items:center;color:#fff;background:var(--blue);border-radius:9px 9px 4px 9px}.type-mark.redemption{color:var(--navy);background:var(--yellow)}.type-mark svg{width:20px;fill:none;stroke:currentColor;stroke-width:1.6;stroke-linecap:round;stroke-linejoin:round}.activity-info strong,.activity-info small,.period strong,.period small,.rule strong,.rule small{display:block}.activity-info small,.period small,.rule small{margin-top:3px;color:var(--muted);font-size:10px}.period strong,.rule strong{font-size:12px}.status{display:inline-block;padding:4px 8px;border-radius:5px;font-size:11px;font-weight:700}.status.active{color:#315e3f;background:#e2efe4}.status.draft{color:#835f0c;background:#fff2c5}.status.paused{color:#7c4b19;background:#f7e8d8}.status.ended{color:#687286;background:#e6e9ee}.date{color:var(--muted);font-size:11px}.actions{white-space:nowrap}.actions button{padding:7px 10px;color:var(--blue);background:#e2ebfa;border:0;border-radius:6px;font-size:12px;font-weight:700}.actions button+button{margin-left:7px;color:#536079;background:#edf0f4}.table-panel footer{display:flex;justify-content:space-between;padding:14px 20px;color:var(--muted);font-size:11px}.state{display:grid;min-height:280px;place-content:center;gap:6px;color:var(--muted);text-align:center}.state strong{color:var(--ink);font-size:16px}.notice{margin:14px 0;padding:10px 12px;color:#a23843;background:#f8e9eb;border-radius:8px}.notice button{margin-left:7px;color:inherit;background:transparent;border:0;text-decoration:underline}.drawer-mask{position:fixed;inset:0;z-index:10;background:rgb(16 31 56 / 42%)}.drawer{position:absolute;inset:0 0 0 auto;display:flex;width:min(700px,calc(100vw - 80px));flex-direction:column;background:var(--paper);box-shadow:-18px 0 40px rgb(16 31 56 / 18%)}.drawer>header{display:flex;align-items:start;justify-content:space-between;padding:26px 30px 20px;border-bottom:1px solid var(--line)}.drawer>header span{color:var(--blue);font-size:12px;font-weight:800}.drawer h2{margin:5px 0 0;font-size:22px}.drawer>header button{display:grid;width:34px;height:34px;place-items:center;color:var(--muted);background:transparent;border:0;border-radius:6px}.drawer>header svg{width:22px;fill:none;stroke:currentColor;stroke-width:1.8;stroke-linecap:round}.drawer-body{display:grid;gap:17px;overflow:auto;padding:24px 30px}.drawer label{display:grid;gap:7px}.drawer label>span{font-size:12px;font-weight:800}.drawer label b{color:#a23843}.drawer input,.drawer textarea,.drawer select{width:100%;padding:0 11px}.drawer input{height:40px}.drawer textarea{padding:10px 11px;line-height:1.5;resize:vertical}.drawer small{color:var(--muted);font-size:10px;line-height:1.5}.field-row{display:grid;grid-template-columns:1fr 1fr;gap:14px}.drawer fieldset{display:grid;gap:14px;margin:0;padding:17px;border:1px solid var(--line);border-radius:12px}.drawer legend{padding:0 6px;color:var(--blue);font-size:12px;font-weight:800}.drawer fieldset>p{margin:0;color:var(--muted);font-size:11px;line-height:1.6}.rule-builder{display:grid;gap:16px;padding-top:4px;border-top:1px solid var(--line)}.rule-builder-head,.rule-editor>header,.value-head,.tier>header{display:flex;align-items:center;justify-content:space-between;gap:12px}.rule-builder-head>div:first-child{display:grid;gap:3px}.rule-builder-head>div:last-child{display:flex;align-items:center;gap:8px}.rule-builder-head select{min-width:150px}.add-rule,.rule-actions button,.value-head button,.value-row>button,.tier>header>button{min-height:34px;padding:0 11px;color:var(--blue);background:#e2ebfa;border:0;border-radius:6px;font-size:11px;font-weight:800}.rule-empty{display:grid;gap:4px;padding:18px;color:var(--muted);text-align:center;background:#eef1f5;border-radius:9px}.rule-empty strong{color:var(--ink)}.rule-editor{display:grid;gap:14px;padding:15px 0;border-top:1px solid var(--line)}.rule-editor>header>div:first-child{display:grid;min-width:115px;gap:2px}.rule-editor>header>div:first-child small{font-variant-numeric:tabular-nums}.rule-switch{display:flex!important;align-items:center;gap:7px;margin-left:auto}.rule-switch input{width:16px!important;height:16px!important;accent-color:var(--blue)}.rule-switch span{font-size:11px!important}.rule-actions{display:flex;gap:5px}.rule-actions button:disabled,.add-rule:disabled{opacity:.45;cursor:not-allowed}.rule-actions button:last-child,.value-row>button,.tier>header>button{color:#8b3942;background:#f4e5e7}.rule-values,.points-tiers,.tier{display:grid;gap:11px}.value-head>span{font-size:11px;font-weight:800}.value-row{display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr) auto;align-items:end;gap:9px}.value-row>button{height:40px}.tier{padding:13px;background:#eef1f5;border-radius:9px}.tier>header label{grid-template-columns:130px 150px;align-items:center}.tier>header label span{font-size:11px}.tier>header label input{height:36px}.drawer>footer{display:flex;justify-content:flex-end;gap:10px;margin-top:auto;padding:17px 30px;border-top:1px solid var(--line)}.drawer>footer .primary-button{min-width:112px}button:focus-visible,input:focus-visible,select:focus-visible,textarea:focus-visible{outline:3px solid rgb(23 79 167 / 18%);outline-offset:2px}.primary-button:disabled,.secondary-button:disabled{opacity:.65;cursor:not-allowed}@media(prefers-reduced-motion:reduce){*{transition:none!important}}
</style>
