<script setup lang="ts">
import axios from 'axios';
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { http } from '../api/http';
import type {
  ActivityDetailResponse,
  ActivitySummaryResponse,
  ApiError,
  LotteryDrawRequest,
  LotteryDrawResponse,
  LotteryRecordResponse,
  LotteryRecordStatus,
  LotteryPrizeResponse,
  PointBalanceResponse,
  PrizeType,
} from '../api/types';
import AccountHeader from '../components/AccountHeader.vue';

const activities = ref<ActivityDetailResponse[]>([]);
const activeCode = ref('');
const balance = ref(0);
const loading = ref(true);
const loadError = ref('');
const drawing = ref(false);
const drawError = ref('');
const drawNotice = ref('');
const confirmingResult = ref(false);
const resultOpen = ref(false);
const activePrize = ref(0);
const drawResult = ref<LotteryDrawResponse | null>(null);
const records = ref<LotteryRecordResponse[]>([]);
const recordsLoading = ref(true);
const recordsError = ref('');
const recordsSection = ref<HTMLElement | null>(null);
const activity = computed(() => activities.value.find(item => item.code === activeCode.value) ?? null);
const formattedBalance = computed(() => new Intl.NumberFormat('zh-CN').format(balance.value));
const pendingRequestKey = (activityCode: string) => `lottery:pending-request:${activityCode}`;
const DRAW_ANIMATION_MS = 3600;
const DRAW_REQUEST_TIMEOUT_MS = 15_000;
const HIGHLIGHT_INTERVAL_MS = 115;
const RECORD_POLL_MS = 5000;
let highlightTimer: number | undefined;
let recordPollTimer: number | undefined;

type DrawOutcome =
  | { kind: 'success'; data: LotteryDrawResponse }
  | { kind: 'error'; error: unknown }
  | { kind: 'deadline' };

function getOrCreateRequestId(activityCode: string) {
  const storageKey = pendingRequestKey(activityCode);
  const pendingRequestId = sessionStorage.getItem(storageKey);
  if (pendingRequestId) return pendingRequestId;

  const requestId = crypto.randomUUID();
  sessionStorage.setItem(storageKey, requestId);
  return requestId;
}

function clearRequestId(activityCode: string) {
  sessionStorage.removeItem(pendingRequestKey(activityCode));
}

function wait(milliseconds: number) {
  return new Promise<void>(resolve => window.setTimeout(resolve, milliseconds));
}

function startPrizeHighlight(prizeCount: number) {
  window.clearInterval(highlightTimer);
  if (prizeCount <= 0 || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  activePrize.value = 0;
  highlightTimer = window.setInterval(() => {
    activePrize.value = (activePrize.value + 1) % prizeCount;
  }, HIGHLIGHT_INTERVAL_MS);
}

function stopPrizeHighlight(targetIndex?: number) {
  window.clearInterval(highlightTimer);
  highlightTimer = undefined;
  if (targetIndex != null && targetIndex >= 0) activePrize.value = targetIndex;
}

function getErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as ApiError | undefined;
    return data?.message || fallback;
  }
  return fallback;
}

function prizeKind(type: PrizeType) {
  return type === 'VIRTUAL' ? '虚拟权益' : type === 'POINTS' ? '积分奖励' : '未中奖';
}

function prizeTone(prize: LotteryPrizeResponse) {
  return prize.type === 'POINTS' ? 'yellow' : prize.type === 'NONE' ? 'quiet' : 'blue';
}

function recordStatusText(status: LotteryRecordStatus) {
  return status === 'SUCCESS' ? '已完成' : status === 'FAILED' ? '未完成' : '处理中';
}

function recordResultText(record: LotteryRecordResponse) {
  if (record.status === 'SUCCESS') return record.prizeName ?? '抽奖结果已确认';
  if (record.status === 'FAILED') return '抽奖未完成';
  return '结果确认中';
}

function recordDetailText(record: LotteryRecordResponse) {
  if (record.status === 'SUCCESS') {
    return record.prizeType ? prizeKind(record.prizeType) : '结果已记录';
  }
  if (record.status === 'FAILED') return '积分未扣除或已退回';
  return '完成后将在这里自动更新';
}

function formatRecordTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false,
  }).format(new Date(value));
}

function dailyLimitText(item: ActivityDetailResponse) {
  return item.dailyLimit == null ? '每日次数不限' : `每日最多 ${item.dailyLimit} 次`;
}

async function loadActivities() {
  loading.value = true;
  loadError.value = '';
  try {
    const [activityResponse, balanceResponse] = await Promise.all([
      http.get<ActivitySummaryResponse[]>('/activities/active'),
      http.get<PointBalanceResponse>('/points/me/balance'),
    ]);
    const lotteryActivities = activityResponse.data.filter(item => item.type === 'LOTTERY');
    activities.value = await Promise.all(lotteryActivities.map(async item =>
      (await http.get<ActivityDetailResponse>(`/activities/${item.code}`)).data));
    balance.value = balanceResponse.data.balance;
    if (!activities.value.some(item => item.code === activeCode.value)) {
      activeCode.value = activities.value[0]?.code ?? '';
    }
  } catch (error) {
    loadError.value = getErrorMessage(error, '暂时无法获取抽奖活动，请稍后重试');
  } finally {
    loading.value = false;
  }
}

async function loadRecords(showLoading = false) {
  window.clearTimeout(recordPollTimer);
  recordPollTimer = undefined;
  if (showLoading) recordsLoading.value = true;
  recordsError.value = '';
  try {
    const previouslyProcessing = new Set(
      records.value.filter(record => record.status === 'PROCESSING').map(record => record.orderId));
    const response = await http.get<LotteryRecordResponse[]>('/activities/lotteries/orders/me');
    records.value = response.data;
    if (response.data.some(record =>
      previouslyProcessing.has(record.orderId) && record.status !== 'PROCESSING')) {
      try {
        const balanceResponse = await http.get<PointBalanceResponse>('/points/me/balance');
        balance.value = balanceResponse.data.balance;
      } catch {
        // 记录结果已经确定，余额会在下次页面加载时重新同步。
      }
    }
  } catch (error) {
    recordsError.value = getErrorMessage(error, '暂时无法获取抽奖记录');
  } finally {
    recordsLoading.value = false;
    if (records.value.some(record => record.status === 'PROCESSING')) {
      recordPollTimer = window.setTimeout(() => void loadRecords(), RECORD_POLL_MS);
    }
  }
}

function viewRecords() {
  recordsSection.value?.scrollIntoView({
    behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
    block: 'start',
  });
}

function selectActivity(code: string) {
  if (drawing.value || code === activeCode.value) return;
  activeCode.value = code;
  activePrize.value = 0;
  drawError.value = '';
  drawNotice.value = '';
  drawResult.value = null;
}

function isPendingError(error: unknown) {
  const data = axios.isAxiosError(error) ? error.response?.data as ApiError | undefined : undefined;
  return data?.code === 'LOTTERY_RETRY_SCHEDULED';
}

function outcomeUnknown(error: unknown) {
  return axios.isAxiosError(error) && !error.response;
}

function applyDrawSuccess(
  result: LotteryDrawResponse, selectedActivity: ActivityDetailResponse,
) {
  clearRequestId(selectedActivity.code);
  drawResult.value = result;
  balance.value = result.balanceAfter;
  drawNotice.value = '';
  const prizeIndex = selectedActivity.prizes.findIndex(
    prize => prize.prizeId === result.prizeId);
  stopPrizeHighlight(prizeIndex < 0 ? 0 : prizeIndex);
  resultOpen.value = true;
}

async function draw() {
  if (drawing.value || !activity.value) return;
  const startedAt = Date.now();
  drawing.value = true;
  drawError.value = '';
  drawNotice.value = '';
  confirmingResult.value = false;
  drawResult.value = null;
  resultOpen.value = false;
  const selectedActivity = activity.value;
  const requestId = getOrCreateRequestId(selectedActivity.code);
  startPrizeHighlight(selectedActivity.prizes.length);

  const requestOutcome: Promise<Exclude<DrawOutcome, { kind: 'deadline' }>> = http
    .post<LotteryDrawResponse>(
      `/activities/lotteries/${selectedActivity.code}/draw`,
      { requestId } satisfies LotteryDrawRequest,
      { timeout: DRAW_REQUEST_TIMEOUT_MS },
    )
    .then(response => ({ kind: 'success' as const, data: response.data }))
    .catch(error => ({ kind: 'error' as const, error }));
  let outcome = await Promise.race<DrawOutcome>([
    requestOutcome,
    wait(DRAW_ANIMATION_MS).then(() => ({ kind: 'deadline' as const })),
  ]);
  let delayed = false;

  if (outcome.kind === 'deadline') {
    delayed = true;
    stopPrizeHighlight();
    confirmingResult.value = true;
    drawNotice.value = '抽奖请求已提交，正在确认最终结果…';
    outcome = await requestOutcome;
  } else {
    await wait(Math.max(0, DRAW_ANIMATION_MS - (Date.now() - startedAt)));
  }

  if (outcome.kind === 'success') {
    applyDrawSuccess(outcome.data, selectedActivity);
  } else {
    stopPrizeHighlight();
    drawNotice.value = '';
    if (isPendingError(outcome.error)) {
      drawNotice.value = '本次结果正在处理中，请稍后在抽奖记录中查看。';
    } else if (outcomeUnknown(outcome.error)) {
      if (delayed) {
        drawNotice.value = '暂时未收到最终结果，请稍后在抽奖记录中查看。';
      } else {
        drawError.value = '网络开了个小差，请再试一次。';
      }
    } else {
      clearRequestId(selectedActivity.code);
      drawError.value = getErrorMessage(outcome.error, '本次抽奖未完成，请稍后重试');
    }
  }
  confirmingResult.value = false;
  drawing.value = false;
  void loadRecords();
}

onMounted(() => {
  void loadActivities();
  void loadRecords(true);
});

onBeforeUnmount(() => {
  window.clearInterval(highlightTimer);
  window.clearTimeout(recordPollTimer);
});
</script>

<template>
  <!--
  THESIS: 多活动抽奖以可扫描的活动清单替代规则说明；选择和参与成本始终留在同一个工作台内。
  OWN-WORLD: 深海军蓝导航、钴蓝参与柜台、纸白活动目录与少量暖黄权益信号。
  STORY: 用户从右侧活动目录选择一项进行中的活动，核对奖池、成本与次数后发起参与并收到结果状态。
  FIRST VIEWPORT: 左侧是当前活动的奖项窗和主行动；右侧是可点击的活动卡片列表，当前项以暖黄信号清晰标识。
  FORM: Operate 模式，活动参与凭证台，candidate 4，seed 4c1571b6。
  FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, and DESIGN.md
  -->
  <div class="activity-page lottery-page">
    <svg class="activity-defs" aria-hidden="true">
      <symbol id="lottery-gift" viewBox="0 0 24 24"><path d="M4 10h16v10H4zM3 7h18v3H3zM12 7v13M7.5 7C5 7 5 3.5 7.5 3.5c2 0 4.5 3.5 4.5 3.5s2.5-3.5 4.5-3.5C19 3.5 19 7 16.5 7"/></symbol>
      <symbol id="lottery-coin" viewBox="0 0 24 24"><circle cx="12" cy="12" r="8"/><path d="M9 9.5h4.2a2 2 0 0 1 0 4H10.8a2 2 0 0 0 0 4H15M12 7v2.5M12 17.5V20"/></symbol>
      <symbol id="lottery-arrow" viewBox="0 0 24 24"><path d="M5 12h14M14 7l5 5-5 5"/></symbol>
      <symbol id="lottery-check" viewBox="0 0 24 24"><path d="m5 12 4 4L19 6"/></symbol>
    </svg>
    <AccountHeader active="lottery" />

    <main class="activity-workspace" aria-live="polite">
      <section v-if="loading" class="page-state"><span class="page-loader" aria-hidden="true"></span><strong>正在同步抽奖活动</strong><p>请稍候，正在获取活动、奖池与积分余额。</p></section>
      <section v-else-if="loadError" class="page-state error-state"><strong>抽奖活动暂不可用</strong><p>{{ loadError }}</p><button type="button" @click="loadActivities">重新加载</button></section>
      <section v-else-if="!activity" class="page-state"><strong>当前没有进行中的抽奖活动</strong><p>活动发布后会显示在这里，请稍后再来看看。</p><button type="button" @click="loadActivities">刷新活动</button></section>

      <template v-else>
      <header class="activity-heading"><div><p>进行中的抽奖活动</p><h1>{{ activity.name }}</h1><span>奖项、参与积分和次数限制以本页展示为准。</span></div><div class="balance-chip"><svg><use href="#lottery-coin"/></svg><div><small>当前可用积分</small><strong>{{ formattedBalance }}</strong></div></div></header>

      <div class="lottery-layout">
        <section class="draw-counter" aria-labelledby="draw-title">
          <div class="counter-top"><div><h2 id="draw-title">本期收获窗</h2><span>{{ dailyLimitText(activity) }}</span></div><span class="live-state"><i></i>活动进行中</span></div>
          <div v-if="activity.prizes.length" class="prize-grid" :class="{ drawing }">
            <article v-for="(prize, index) in activity.prizes" :key="prize.id" :class="['prize-cell', prizeTone(prize), { selected: !drawing && Boolean(drawResult) && index === activePrize, highlighted: drawing && index === activePrize }]">
              <svg><use href="#lottery-gift"/></svg><strong>{{ prize.name }}</strong><small>{{ prizeKind(prize.type) }}</small>
            </article>
          </div>
          <div v-else class="empty-pool"><strong>奖池尚未配置</strong><span>请等待活动方完成奖品配置后再参与。</span></div>
          <div class="draw-action"><div><span>单次参与</span><strong>{{ activity.pointsCost }} <small>积分</small></strong></div><button type="button" :disabled="drawing || balance < activity.pointsCost || !activity.prizes.length" @click="draw"><span v-if="drawing" class="button-loader"></span><span>{{ drawing ? confirmingResult ? '正在确认结果…' : '正在抽取结果…' : !activity.prizes.length ? '奖池尚未开放' : balance < activity.pointsCost ? '当前积分不足' : `使用 ${activity.pointsCost} 积分参与` }}</span><svg v-if="!drawing && balance >= activity.pointsCost && activity.prizes.length"><use href="#lottery-arrow"/></svg></button></div>
          <p v-if="drawError" class="action-error" role="alert">{{ drawError }}</p>
          <div v-if="drawNotice" class="action-notice" role="status"><span>{{ drawNotice }}</span><button type="button" @click="viewRecords">查看抽奖记录</button></div>
          <p class="counter-caption">参与成功后立即扣除积分，抽奖结果可在下方记录中查看。</p>
        </section>

        <aside class="activity-catalog" aria-label="活动列表">
          <div class="catalog-heading"><div><h2>选择活动</h2><span>点击切换当前奖池</span></div><strong>{{ activities.length }} 项</strong></div>
          <button v-for="item in activities" :key="item.code" type="button" :class="['activity-card', { active: item.code === activeCode }]" :aria-pressed="item.code === activeCode" @click="selectActivity(item.code)">
            <span class="activity-card-mark"><svg><use href="#lottery-gift"/></svg></span>
            <span class="activity-card-copy"><small>抽奖活动</small><strong>{{ item.name }}</strong><em>奖池共 {{ item.prizes.length }} 项</em></span>
            <span class="activity-card-meta"><b>{{ item.pointsCost }} 积分</b><i>{{ dailyLimitText(item) }}</i></span>
          </button>
          <p class="catalog-note"><svg><use href="#lottery-check"/></svg><span>各活动的奖项、次数与积分成本，由活动配置分别决定。</span></p>
        </aside>
      </div>
      <section id="lottery-records" ref="recordsSection" class="lottery-records" aria-labelledby="records-title">
        <header class="records-heading"><div><h2 id="records-title">我的抽奖记录</h2><p>处理中记录会自动刷新，成功后才会展示奖品结果。</p></div><button type="button" :disabled="recordsLoading" @click="loadRecords(true)">{{ recordsLoading ? '正在刷新' : '刷新记录' }}</button></header>
        <div v-if="recordsLoading && !records.length" class="records-state"><span class="page-loader" aria-hidden="true"></span><span>正在获取抽奖记录</span></div>
        <div v-else-if="recordsError && !records.length" class="records-state records-error"><span>{{ recordsError }}</span><button type="button" @click="loadRecords(true)">重新加载</button></div>
        <div v-else-if="!records.length" class="records-state"><strong>还没有抽奖记录</strong><span>完成首次参与后，结果会保存在这里。</span></div>
        <div v-else class="records-list">
          <article v-for="record in records" :key="record.orderId" class="record-row">
            <span :class="['record-status', record.status.toLowerCase()]" aria-hidden="true"><i></i></span>
            <div class="record-activity"><strong>{{ record.activityName }}</strong><span>{{ formatRecordTime(record.createdAt) }} · {{ record.pointsCost }} 积分</span></div>
            <div class="record-result"><strong>{{ recordResultText(record) }}</strong><span>{{ recordDetailText(record) }}</span></div>
            <span :class="['record-badge', record.status.toLowerCase()]">{{ recordStatusText(record.status) }}</span>
          </article>
        </div>
      </section>
      <section class="activity-footnote"><span>活动说明</span><p>参与前请确认所需积分和每日参与次数；抽奖完成后，可在“我的抽奖记录”中查看结果。</p></section>
      </template>
    </main>

    <div v-if="resultOpen && drawResult && activity" class="result-dialog" role="dialog" aria-modal="true" aria-labelledby="result-title" @click.self="resultOpen = false"><section><button class="dialog-close" type="button" aria-label="关闭结果" @click="resultOpen = false">×</button><span class="result-mark"><svg><use href="#lottery-gift"/></svg></span><p>{{ activity.name }} · 本次参与结果</p><h2 id="result-title">{{ drawResult.prizeName }}</h2><span class="result-kind">{{ prizeKind(drawResult.prizeType) }}</span><div class="result-note"><svg><use href="#lottery-check"/></svg><span>本次抽奖结果已记录。</span></div><button class="dialog-confirm" type="button" @click="resultOpen = false">我知道了</button></section></div>
  </div>
</template>

<style scoped>
.activity-page{--blue:#174fa7;--navy:#102f65;--yellow:#f1c84a;--paper:#f8f7f2;--ink:#17213a;--muted:#697287;--line:#d7dde6;min-width:1180px;min-height:100vh;background:#e8edf3;color:var(--ink)}.activity-page svg{width:20px;height:20px;fill:none;stroke:currentColor;stroke-width:1.7;stroke-linecap:round;stroke-linejoin:round}.activity-defs{position:absolute;width:0!important;height:0!important;overflow:hidden}.activity-workspace{width:min(1180px,calc(100% - 96px));margin:0 auto;padding:37px 0 48px}.activity-kicker{display:flex;align-items:center;gap:9px;color:#697287;font-size:12px}.activity-kicker span:first-child{color:var(--blue);font-weight:800}.activity-kicker i{width:4px;height:4px;background:#a6b0bf;border-radius:50%}.activity-heading{display:flex;align-items:end;justify-content:space-between;margin:14px 0 24px;padding-bottom:24px;border-bottom:1px solid var(--line)}.activity-heading p{margin:0 0 5px;color:var(--blue);font-size:13px;font-weight:700}.activity-heading h1{margin:0;font-size:34px;letter-spacing:-.025em}.activity-heading>div>span{display:block;margin-top:8px;color:var(--muted);font-size:13px}.balance-chip{display:flex;align-items:center;gap:12px;padding:12px 16px;background:var(--paper);border:1px solid #dfe4eb;border-radius:12px}.balance-chip svg{color:var(--blue)}.balance-chip small,.balance-chip strong{display:block}.balance-chip small{color:var(--muted);font-size:11px}.balance-chip strong{margin-top:2px;font-size:20px;font-variant-numeric:tabular-nums}.lottery-layout{display:grid;grid-template-columns:minmax(0,1fr) 330px;gap:22px}.draw-counter,.activity-catalog{background:var(--paper);border-radius:16px;box-shadow:0 10px 28px rgb(38 50 75 / 9%)}.draw-counter{padding:28px}.counter-top{display:flex;align-items:flex-start;justify-content:space-between}.counter-top h2,.catalog-heading h2{margin:0;font-size:21px;letter-spacing:-.015em}.counter-top>div>span,.catalog-heading span{display:block;margin-top:5px;color:var(--muted);font-size:11px}.live-state{display:flex;align-items:center;gap:6px;padding:5px 9px;color:#315e3f;background:#e2efe4;border-radius:6px;font-size:11px;font-weight:700}.live-state i{width:6px;height:6px;background:#4da572;border-radius:50%}.prize-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;margin:25px 0}.prize-cell{display:flex;min-height:116px;flex-direction:column;justify-content:center;padding:16px;border:1px solid #dfe5ee;border-radius:12px;background:#fff;transition:border-color .18s ease,transform .18s ease}.prize-cell svg{margin-bottom:10px;color:var(--blue)}.prize-cell strong{font-size:14px}.prize-cell small{margin-top:4px;color:var(--muted);font-size:11px}.prize-cell.yellow{background:#fffdf4}.prize-cell.yellow svg{color:#b07b08}.prize-cell.quiet{background:#f3f5f7}.prize-cell.quiet svg{color:#7d8798}.prize-grid.drawing .prize-cell{opacity:.6}.prize-grid.drawing .prize-cell.selected{opacity:1;border-color:var(--yellow);transform:translateY(-2px);box-shadow:0 7px 17px rgb(176 123 8 / 14%)}.draw-action{display:flex;align-items:center;justify-content:space-between;padding:17px 18px;background:var(--blue);border-radius:12px;color:#fff}.draw-action>div span,.draw-action>div strong{display:block}.draw-action>div span{color:#cbdaf3;font-size:11px}.draw-action>div strong{margin-top:1px;font-size:22px;font-variant-numeric:tabular-nums}.draw-action>div small{font-size:11px}.draw-action button,.dialog-confirm{display:flex;min-height:44px;align-items:center;justify-content:center;gap:8px;padding:0 17px;color:var(--navy);background:var(--yellow);border:0;border-radius:9px;font-size:13px;font-weight:800;transition:transform .18s ease}.draw-action button:hover:not(:disabled),.dialog-confirm:hover{transform:translateY(-1px)}.draw-action button:disabled{opacity:.8;cursor:wait}.draw-action button svg{width:17px}.button-loader{width:15px;height:15px;border:2px solid rgb(16 47 101 / 28%);border-top-color:var(--navy);border-radius:50%;animation:spin .7s linear infinite}.counter-caption{margin:14px 0 0;color:#788195;font-size:11px}.activity-catalog{padding:20px}.catalog-heading{display:flex;align-items:flex-start;justify-content:space-between;padding:4px 4px 15px;border-bottom:1px solid var(--line)}.catalog-heading strong{display:grid;min-width:38px;height:24px;place-items:center;color:var(--blue);background:#e2ebfa;border-radius:6px;font-size:11px}.activity-card{display:grid;grid-template-columns:32px minmax(0,1fr);gap:10px;width:100%;padding:15px 5px;color:var(--ink);text-align:left;background:transparent;border:0;border-bottom:1px solid var(--line);cursor:pointer;transition:background .18s ease,transform .18s ease}.activity-card:last-of-type{border-bottom:0}.activity-card:hover{background:#f0f3f8}.activity-card:focus-visible{outline:3px solid rgb(23 79 167 / 28%);outline-offset:2px}.activity-card.active{margin:10px 0;padding:14px 10px;background:#e8effa;border:1px solid #b8caea;border-radius:12px}.activity-card.active+.activity-card{border-top:1px solid var(--line)}.activity-card-mark{display:grid;width:30px;height:30px;place-items:center;color:var(--blue);background:#e2ebfa;border-radius:8px 8px 3px 8px}.activity-card.active .activity-card-mark{color:var(--navy);background:var(--yellow)}.activity-card-mark svg{width:16px}.activity-card-copy{min-width:0}.activity-card-copy small,.activity-card-copy strong,.activity-card-copy em{display:block}.activity-card-copy small{color:var(--blue);font-size:10px;font-weight:700}.activity-card-copy strong{margin:2px 0;color:var(--ink);font-size:13px}.activity-card-copy em{overflow:hidden;color:var(--muted);font-size:10px;font-style:normal;line-height:1.45;text-overflow:ellipsis;white-space:nowrap}.activity-card-meta{grid-column:2;display:flex;align-items:center;justify-content:space-between;margin-top:-4px}.activity-card-meta b{color:#536079;font-size:10px}.activity-card-meta i{color:#697287;font-size:10px;font-style:normal}.activity-card.active .activity-card-meta b{color:var(--blue)}.catalog-note{display:flex;gap:7px;margin:16px 3px 2px;color:#697287;font-size:10px;line-height:1.6}.catalog-note svg{width:15px;flex:0 0 auto;color:var(--blue)}.activity-footnote{display:flex;gap:22px;margin-top:22px;padding:18px 22px;color:#697287;border-top:1px solid var(--line);font-size:12px}.activity-footnote span{flex:0 0 98px;color:#17213a;font-weight:700}.activity-footnote p{margin:0;line-height:1.7}.result-dialog{position:fixed;inset:0;z-index:10;display:grid;place-items:center;background:rgb(16 31 56 / 45%);padding:24px}.result-dialog section{position:relative;width:390px;padding:34px;text-align:center;background:var(--paper);border-radius:16px;box-shadow:0 22px 52px rgb(9 29 64 / 28%)}.dialog-close{position:absolute;top:12px;right:16px;color:#697287;background:transparent;border:0;font-size:26px;line-height:1}.result-mark{display:grid;width:55px;height:55px;margin:0 auto 16px;place-items:center;color:var(--navy);background:var(--yellow);border-radius:15px 15px 5px 15px}.result-mark svg{width:27px}.result-dialog p{margin:0;color:var(--muted);font-size:12px}.result-dialog h2{margin:7px 0 4px;font-size:25px}.result-kind{color:var(--blue);font-size:12px;font-weight:700}.result-note{display:flex;gap:8px;margin:20px 0;padding:11px;text-align:left;color:#536079;background:#e9eef5;border-radius:9px;font-size:12px;line-height:1.65}.result-note svg{width:17px;flex:0 0 auto;color:var(--blue)}.dialog-confirm{width:100%;color:#fff;background:var(--blue)}@keyframes spin{to{transform:rotate(360deg)}}@media(prefers-reduced-motion:reduce){.prize-cell,.draw-action button,.activity-card,.dialog-confirm{transition:none}.button-loader{animation:none}}
.page-state{display:grid;min-height:520px;place-content:center;justify-items:center;gap:10px;text-align:center}.page-state strong{font-size:21px}.page-state p{max-width:420px;margin:0;color:var(--muted);font-size:14px}.page-state button{margin-top:8px;padding:11px 18px;color:#fff;background:var(--blue);border:0;border-radius:9px;font-weight:700}.error-state strong,.action-error{color:#a23843}.page-loader{width:30px;height:30px;border:3px solid #c7d5ed;border-top-color:var(--blue);border-radius:50%;animation:spin .7s linear infinite}.empty-pool{display:grid;min-height:242px;margin:25px 0;place-content:center;gap:5px;color:var(--muted);text-align:center;background:#f1f4f8;border-radius:12px}.empty-pool strong{color:var(--ink);font-size:16px}.empty-pool span{font-size:12px}.action-error{margin:10px 0 0;font-size:12px;font-weight:600}.draw-action button:disabled{cursor:not-allowed}.prize-cell.selected{border-color:var(--yellow);box-shadow:0 7px 17px rgb(176 123 8 / 14%)}@media(prefers-reduced-motion:reduce){.page-loader{animation:none}}
</style>

<style scoped>
.prize-grid.drawing .prize-cell {
  opacity: .48;
  transition-duration: .09s;
  will-change: transform, opacity;
}

.prize-grid.drawing .prize-cell.highlighted {
  opacity: 1;
  border-color: #d6a91f;
  background: #fff8d9;
  transform: translateY(-3px);
  box-shadow: 0 9px 20px rgb(176 123 8 / 20%);
}

.action-notice {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 12px;
  padding: 10px 12px;
  color: #536079;
  background: #e9eef5;
  border-radius: 9px;
  font-size: 12px;
}

.action-notice button,
.records-heading button,
.records-state button {
  padding: 0;
  color: var(--blue);
  background: transparent;
  border: 0;
  font-size: 12px;
  font-weight: 700;
  text-underline-offset: 3px;
  cursor: pointer;
}

.action-notice button:hover,
.records-heading button:hover,
.records-state button:hover {
  text-decoration: underline;
}

.action-notice button:focus-visible,
.records-heading button:focus-visible,
.records-state button:focus-visible {
  outline: 3px solid rgb(23 79 167 / 22%);
  outline-offset: 3px;
  border-radius: 4px;
}

.lottery-records {
  scroll-margin-top: 24px;
  margin-top: 22px;
  padding: 24px 28px;
  background: var(--paper);
  border-radius: 16px;
  box-shadow: 0 10px 28px rgb(38 50 75 / 9%);
}

.records-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--line);
}

.records-heading h2 {
  margin: 0;
  font-size: 21px;
  letter-spacing: -.015em;
}

.records-heading p {
  margin: 5px 0 0;
  color: var(--muted);
  font-size: 11px;
}

.records-heading button {
  min-height: 34px;
  padding: 0 11px;
  background: #e2ebfa;
  border-radius: 9px;
  text-decoration: none;
}

.records-heading button:disabled {
  opacity: .6;
  cursor: wait;
}

.records-list {
  display: grid;
}

.record-row {
  display: grid;
  grid-template-columns: 22px minmax(220px, 1fr) minmax(260px, 1.15fr) 68px;
  align-items: center;
  gap: 14px;
  min-height: 70px;
  border-bottom: 1px solid var(--line);
}

.record-row:last-child {
  border-bottom: 0;
}

.record-status {
  display: grid;
  width: 18px;
  height: 18px;
  place-items: center;
  background: #e2ebfa;
  border-radius: 6px 6px 2px 6px;
}

.record-status i {
  width: 6px;
  height: 6px;
  background: var(--blue);
  border-radius: 50%;
}

.record-status.success {
  background: #e2efe4;
}

.record-status.success i {
  background: #4d8a5e;
}

.record-status.failed {
  background: #f5e5e7;
}

.record-status.failed i {
  background: #a23843;
}

.record-status.processing i {
  animation: record-pulse 1.2s ease-in-out infinite;
}

.record-activity strong,
.record-activity span,
.record-result strong,
.record-result span {
  display: block;
}

.record-activity strong,
.record-result strong {
  color: var(--ink);
  font-size: 13px;
}

.record-activity span,
.record-result span {
  margin-top: 3px;
  color: var(--muted);
  font-size: 10px;
}

.record-badge {
  justify-self: end;
  padding: 6px 8px;
  color: var(--blue);
  background: #e2ebfa;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
}

.record-badge.success {
  color: #315e3f;
  background: #e2efe4;
}

.record-badge.failed {
  color: #8b3039;
  background: #f5e5e7;
}

.records-state {
  display: grid;
  min-height: 140px;
  place-content: center;
  justify-items: center;
  gap: 8px;
  color: var(--muted);
  font-size: 12px;
  text-align: center;
}

.records-state .page-loader {
  width: 22px;
  height: 22px;
  border-width: 2px;
}

.records-error {
  color: #a23843;
}

@keyframes record-pulse {
  50% { opacity: .35; transform: scale(.72); }
}

@media (prefers-reduced-motion: reduce) {
  .prize-grid.drawing .prize-cell { will-change: auto; }
  .record-status.processing i { animation: none; }
}
</style>
