<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { http } from '../api/http';
import type {
  ActivityDetailResponse,
  ActivitySummaryResponse,
  ApiError,
  PointBalanceResponse,
  RedemptionItemResponse,
  RedemptionResponse,
} from '../api/types';
import AccountHeader from '../components/AccountHeader.vue';

const router = useRouter();
const activities = ref<ActivityDetailResponse[]>([]);
const activeCode = ref('');
const balance = ref(0);
const loading = ref(true);
const loadError = ref('');
const current = ref<RedemptionItemResponse | null>(null);
const submitting = ref(false);
const success = ref(false);
const submitError = ref('');
const result = ref<RedemptionResponse | null>(null);
const activity = computed(() => activities.value.find(item => item.code === activeCode.value) ?? null);
const items = computed(() => activity.value?.items ?? []);
const formattedBalance = computed(() => new Intl.NumberFormat('zh-CN').format(balance.value));

function getErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as ApiError | undefined;
    return data?.message || fallback;
  }
  return fallback;
}

function category(item: RedemptionItemResponse) {
  return item.type === 'POINTS' ? '积分奖励' : '虚拟权益';
}

function accent(item: RedemptionItemResponse) {
  if (item.type === 'POINTS') return 'yellow';
  return item.displayOrder % 2 === 0 ? 'navy' : 'blue';
}

async function loadActivities() {
  loading.value = true;
  loadError.value = '';
  try {
    const userId = sessionStorage.getItem('currentUserId');
    if (!userId) {
      await router.replace('/login');
      return;
    }
    const [activityResponse, balanceResponse] = await Promise.all([
      http.get<ActivitySummaryResponse[]>('/activities/active'),
      http.get<PointBalanceResponse>(`/points/users/${userId}/balance`),
    ]);
    const redemptionActivities = activityResponse.data.filter(item => item.type === 'REDEMPTION');
    activities.value = await Promise.all(redemptionActivities.map(async item =>
      (await http.get<ActivityDetailResponse>(`/activities/${item.code}`)).data));
    balance.value = balanceResponse.data.balance;
    if (!activities.value.some(item => item.code === activeCode.value)) {
      activeCode.value = activities.value[0]?.code ?? '';
    }
  } catch (error) {
    loadError.value = getErrorMessage(error, '暂时无法获取兑换活动，请稍后重试');
  } finally {
    loading.value = false;
  }
}

function start(item: RedemptionItemResponse) {
  current.value = item;
  success.value = false;
  submitError.value = '';
  result.value = null;
}

async function confirm() {
  if (!current.value || !activity.value || submitting.value) return;
  const userId = sessionStorage.getItem('currentUserId');
  if (!userId) {
    await router.replace('/login');
    return;
  }
  submitting.value = true;
  submitError.value = '';
  try {
    const response = await http.post<RedemptionResponse>(
      `/activities/redemptions/${activity.value.code}/items/${current.value.id}/users/${userId}`,
    );
    result.value = response.data;
    balance.value = response.data.balanceAfter;
    success.value = true;
  } catch (error) {
    submitError.value = getErrorMessage(error, '兑换未完成，请核对积分后重试');
  } finally {
    submitting.value = false;
  }
}

function close() {
  current.value = null;
  success.value = false;
  submitError.value = '';
  result.value = null;
}

onMounted(loadActivities);
</script>

<template>
  <!--
  THESIS: 兑换商城以货架式选择配合一张确认单，先呈现成本与交付状态，再让用户确认扣分。
  OWN-WORLD: 深海军蓝导航、纸白活动货架、钴蓝兑换操作与暖黄仅标记积分权益。
  STORY: 用户浏览可兑换的演示项目，清楚看到积分成本与待发奖状态，在确认单内完成兑换决定。
  FIRST VIEWPORT: 标题和积分余额横向对齐，四个可兑换项目组成单层货架，右侧固定显示本期交付边界。
  FORM: Operate 模式，活动参与凭证台的兑换分支，candidate 4，seed 4c1571b6。
  FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, and DESIGN.md
  -->
  <div class="redemption-page">
    <svg class="redemption-defs" aria-hidden="true"><symbol id="redeem-bag" viewBox="0 0 24 24"><path d="M5 8h14l-1 12H6L5 8Z"/><path d="M9 9V6a3 3 0 0 1 6 0v3"/></symbol><symbol id="redeem-coin" viewBox="0 0 24 24"><circle cx="12" cy="12" r="8"/><path d="M9 9.5h4.2a2 2 0 0 1 0 4H10.8a2 2 0 0 0 0 4H15M12 7v2.5M12 17.5V20"/></symbol><symbol id="redeem-check" viewBox="0 0 24 24"><path d="m5 12 4 4L19 6"/></symbol><symbol id="redeem-arrow" viewBox="0 0 24 24"><path d="M5 12h14M14 7l5 5-5 5"/></symbol></svg>
    <AccountHeader active="redemption" />
    <main class="redemption-workspace" aria-live="polite">
      <section v-if="loading" class="page-state"><span class="page-loader" aria-hidden="true"></span><strong>正在同步兑换活动</strong><p>请稍候，正在获取商品、价格与积分余额。</p></section>
      <section v-else-if="loadError" class="page-state error-state"><strong>兑换活动暂不可用</strong><p>{{ loadError }}</p><button type="button" @click="loadActivities">重新加载</button></section>
      <section v-else-if="!activity" class="page-state"><strong>当前没有进行中的兑换活动</strong><p>活动发布后会显示在这里，请稍后再来看看。</p><button type="button" @click="loadActivities">刷新活动</button></section>

      <template v-else>
        <div class="redeem-kicker"><span>活动服务</span><i></i><span>实时商品配置</span></div>
        <header class="redeem-heading">
          <div><p>进行中的兑换活动</p><h1>{{ activity.name }}</h1><span>兑换确认后立即扣除积分，奖励随后进入待发奖状态。</span></div>
          <div class="redeem-balance"><svg><use href="#redeem-coin"/></svg><div><small>当前可用积分</small><strong>{{ formattedBalance }}</strong></div></div>
        </header>
        <label v-if="activities.length > 1" class="activity-select"><span>当前活动</span><select v-model="activeCode" @change="close"><option v-for="option in activities" :key="option.code" :value="option.code">{{ option.name }}</option></select></label>
        <div class="redeem-layout">
          <section>
            <div class="shelf-heading"><div><span class="shelf-number">01</span><h2>可兑换项目</h2></div><span>当前共 {{ items.length }} 项</span></div>
            <div v-if="items.length" class="item-grid">
              <article v-for="item in items" :key="item.id" class="redeem-item">
                <div :class="['item-visual', accent(item)]"><svg><use href="#redeem-bag"/></svg><span>{{ category(item) }}</span></div>
                <div class="item-copy"><div><h2>{{ item.name }}</h2><span>可兑换</span></div><p>{{ item.campaignQuota == null ? '活动投放名额不限，以服务端校验为准' : `活动投放名额 ${item.campaignQuota} 份，以服务端剩余量为准` }}</p><footer><strong>{{ item.pointsPrice }} <small>积分</small></strong><button type="button" :disabled="balance < item.pointsPrice" @click="start(item)">{{ balance < item.pointsPrice ? '积分不足' : '立即兑换' }} <svg v-if="balance >= item.pointsPrice"><use href="#redeem-arrow"/></svg></button></footer></div>
              </article>
            </div>
            <div v-else class="empty-shelf"><strong>当前活动暂无可兑换商品</strong><span>商品上架后会显示在这里。</span></div>
          </section>
          <aside class="delivery-panel"><div class="delivery-title"><span class="shelf-number">02</span><h2>兑换说明</h2></div><ol><li><span>01</span><p><strong>确认资格与价格</strong>活动状态、积分余额及项目规则以服务端校验为准。</p></li><li><span>02</span><p><strong>确认后立即扣分</strong>兑换参与成功即会写入积分流水。</p></li><li><span>03</span><p><strong>生成待发奖记录</strong>一期暂不处理真实库存和实际发放。</p></li></ol><div class="delivery-note"><svg><use href="#redeem-check"/></svg><span>商品名称、价格和活动名额均来自当前活动配置。</span></div></aside>
        </div>
      </template>
    </main>
    <div v-if="current" class="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="confirm-title" @click.self="close">
      <section v-if="!success"><button class="close" type="button" aria-label="关闭确认" @click="close">×</button><span class="dialog-icon"><svg><use href="#redeem-bag"/></svg></span><p>确认兑换</p><h2 id="confirm-title">{{ current.name }}</h2><dl><div><dt>兑换积分</dt><dd>{{ current.pointsPrice }} 积分</dd></div><div><dt>参与确认后</dt><dd>立即扣除积分</dd></div><div><dt>奖励状态</dt><dd>待发奖</dd></div></dl><p class="confirm-tip">请确认本次兑换；一期暂不支持取消、退款或积分回退。</p><p v-if="submitError" class="submit-error" role="alert">{{ submitError }}</p><div class="dialog-actions"><button type="button" class="cancel" :disabled="submitting" @click="close">暂不兑换</button><button type="button" class="submit" :disabled="submitting || balance < current.pointsPrice" @click="confirm">{{ submitting ? '正在提交…' : '确认并扣除积分' }}</button></div></section>
      <section v-else class="success-card"><span class="dialog-icon"><svg><use href="#redeem-check"/></svg></span><p>兑换已提交</p><h2>{{ result?.prizeName || current.name }}</h2><span>{{ result?.pendingAwardCreated ? '已进入待发奖状态' : '兑换记录已创建' }}</span><small v-if="result">当前积分余额 {{ formattedBalance }}</small><button type="button" class="submit" @click="close">完成</button></section>
    </div>
  </div>
</template>

<style scoped>
.redemption-page{--blue:#174fa7;--navy:#102f65;--yellow:#f1c84a;--paper:#f8f7f2;--ink:#17213a;--muted:#697287;--line:#d7dde6;min-width:1180px;min-height:100vh;color:var(--ink);background:#e8edf3}.redemption-page svg{width:20px;height:20px;fill:none;stroke:currentColor;stroke-width:1.7;stroke-linecap:round;stroke-linejoin:round}.redemption-defs{position:absolute;width:0!important;height:0!important;overflow:hidden}.redemption-workspace{width:min(1180px,calc(100% - 96px));margin:0 auto;padding:37px 0 48px}.redeem-kicker{display:flex;align-items:center;gap:9px;color:var(--muted);font-size:12px}.redeem-kicker span:first-child{color:var(--blue);font-weight:800}.redeem-kicker i{width:4px;height:4px;background:#a6b0bf;border-radius:50%}.redeem-heading{display:flex;align-items:end;justify-content:space-between;margin:14px 0 24px;padding-bottom:24px;border-bottom:1px solid var(--line)}.redeem-heading p{margin:0 0 5px;color:var(--blue);font-size:13px;font-weight:700}.redeem-heading h1{margin:0;font-size:34px;letter-spacing:-.025em}.redeem-heading>div>span{display:block;margin-top:8px;color:var(--muted);font-size:13px}.redeem-balance{display:flex;align-items:center;gap:12px;padding:12px 16px;background:var(--paper);border:1px solid #dfe4eb;border-radius:12px}.redeem-balance svg{color:var(--blue)}.redeem-balance small,.redeem-balance strong{display:block}.redeem-balance small{color:var(--muted);font-size:11px}.redeem-balance strong{margin-top:2px;font-size:20px;font-variant-numeric:tabular-nums}.redeem-layout{display:grid;grid-template-columns:minmax(0,1fr) 300px;gap:22px}.shelf-heading,.delivery-title{display:flex;align-items:center;justify-content:space-between;margin-bottom:15px}.shelf-heading>div,.delivery-title{display:flex;align-items:center;gap:11px}.shelf-heading h2,.delivery-title h2{margin:0;font-size:21px;letter-spacing:-.015em}.shelf-heading>span{color:var(--muted);font-size:12px}.shelf-number{display:grid;width:27px;height:27px;place-items:center;color:var(--navy);background:var(--yellow);border-radius:8px 8px 3px 8px;font-size:11px;font-weight:800}.item-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.redeem-item{display:grid;grid-template-columns:112px 1fr;min-height:190px;overflow:hidden;background:var(--paper);border-radius:16px;box-shadow:0 10px 28px rgb(38 50 75 / 9%)}.item-visual{display:flex;align-items:center;justify-content:center;flex-direction:column;gap:9px;color:#fff;background:var(--blue)}.item-visual.navy{background:var(--navy)}.item-visual.yellow{color:var(--navy);background:var(--yellow)}.item-visual svg{width:30px;height:30px}.item-visual span{font-size:10px;font-weight:700}.item-copy{display:flex;min-width:0;padding:18px 17px 15px;flex-direction:column}.item-copy>div{display:flex;align-items:start;justify-content:space-between;gap:8px}.item-copy h2{margin:0;font-size:16px;line-height:1.4}.item-copy>div span{padding:4px 6px;color:#315e3f;background:#e2efe4;border-radius:5px;font-size:10px;white-space:nowrap}.item-copy p{margin:8px 0;color:var(--muted);font-size:11px;line-height:1.65}.item-copy footer{display:flex;align-items:center;justify-content:space-between;margin-top:auto}.item-copy footer strong{color:var(--blue);font-size:18px;font-variant-numeric:tabular-nums}.item-copy footer small{font-size:10px}.item-copy button{display:flex;align-items:center;gap:4px;color:var(--blue);background:transparent;border:0;font-size:11px;font-weight:800}.item-copy button svg{width:15px}.delivery-panel{align-self:start;padding:26px;background:var(--paper);border-radius:16px;box-shadow:0 10px 28px rgb(38 50 75 / 9%)}.delivery-panel ol{display:grid;gap:17px;margin:23px 0;padding:0;list-style:none}.delivery-panel li{display:grid;grid-template-columns:25px 1fr;gap:10px}.delivery-panel li>span{display:grid;width:23px;height:23px;place-items:center;color:var(--blue);background:#e2ebfa;border-radius:6px;font-size:10px;font-weight:800}.delivery-panel li p{margin:0;color:var(--muted);font-size:11px;line-height:1.65}.delivery-panel li strong{display:block;color:var(--ink);font-size:12px}.delivery-note{display:flex;gap:8px;padding:11px;color:#536079;background:#e9eef5;border-radius:9px;font-size:11px;line-height:1.6}.delivery-note svg{width:16px;flex:0 0 auto;color:var(--blue)}.confirm-dialog{position:fixed;inset:0;z-index:10;display:grid;place-items:center;background:rgb(16 31 56 / 45%);padding:24px}.confirm-dialog section{position:relative;width:410px;padding:34px;background:var(--paper);border-radius:16px;box-shadow:0 22px 52px rgb(9 29 64 / 28%);text-align:center}.close{position:absolute;top:12px;right:16px;border:0;color:var(--muted);background:transparent;font-size:26px}.dialog-icon{display:grid;width:52px;height:52px;margin:0 auto 14px;place-items:center;color:var(--navy);background:var(--yellow);border-radius:14px 14px 4px 14px}.dialog-icon svg{width:25px}.confirm-dialog p{margin:0;color:var(--muted);font-size:12px}.confirm-dialog h2{margin:7px 0 2px;font-size:24px}.confirm-dialog dl{margin:20px 0;text-align:left;border-top:1px solid var(--line)}.confirm-dialog dl>div{display:flex;justify-content:space-between;padding:11px 0;border-bottom:1px solid var(--line);font-size:12px}.confirm-dialog dt{color:var(--muted)}.confirm-dialog dd{margin:0;font-weight:700}.confirm-tip{padding:10px;text-align:left;background:#e9eef5;border-radius:8px;line-height:1.6}.dialog-actions{display:grid;grid-template-columns:1fr 1.4fr;gap:10px;margin-top:18px}.dialog-actions button,.success-card .submit{min-height:44px;border:0;border-radius:9px;font-size:13px;font-weight:800}.cancel{color:#536079;background:#e9edf2}.submit{color:#fff;background:var(--blue);box-shadow:0 8px 18px rgb(23 79 167 / 22%)}.submit:disabled{opacity:.72;cursor:wait}.success-card .dialog-icon{color:#315e3f;background:#e2efe4}.success-card>span:not(.dialog-icon){display:block;color:#315e3f;font-size:12px;font-weight:700}.success-card .submit{width:100%;margin-top:23px}@media(prefers-reduced-motion:reduce){.item-copy button{transition:none}}
.page-state{display:grid;min-height:520px;place-content:center;justify-items:center;gap:10px;text-align:center}.page-state strong{font-size:21px}.page-state p{max-width:420px;margin:0;color:var(--muted);font-size:14px}.page-state button{margin-top:8px;padding:11px 18px;color:#fff;background:var(--blue);border:0;border-radius:9px;font-weight:700}.error-state strong,.submit-error{color:#a23843}.page-loader{width:30px;height:30px;border:3px solid #c7d5ed;border-top-color:var(--blue);border-radius:50%;animation:spin .7s linear infinite}.activity-select{display:flex;align-items:center;justify-content:flex-end;gap:10px;margin:-8px 0 18px;color:var(--muted);font-size:12px}.activity-select select{min-width:220px;height:38px;padding:0 34px 0 11px;color:var(--ink);background:#fff;border:1px solid var(--line);border-radius:9px}.activity-select select:focus-visible{outline:3px solid rgb(23 79 167 / 18%);outline-offset:2px}.empty-shelf{display:grid;min-height:300px;place-content:center;gap:6px;color:var(--muted);text-align:center;background:var(--paper);border-radius:16px}.empty-shelf strong{color:var(--ink);font-size:16px}.empty-shelf span{font-size:12px}.item-copy button:disabled{color:#8992a2;cursor:not-allowed}.submit-error{margin-top:12px!important;font-weight:600}.success-card small{display:block;margin-top:8px;color:var(--muted);font-size:11px;font-variant-numeric:tabular-nums}@keyframes spin{to{transform:rotate(360deg)}}@media(prefers-reduced-motion:reduce){.page-loader{animation:none}}
</style>
