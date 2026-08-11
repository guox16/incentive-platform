<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { http } from '../api/http';
import AccountHeader from '../components/AccountHeader.vue';
import type {
  ApiError,
  DailyCheckInResponse,
  PointBalanceResponse,
  PointTransactionPageResponse,
  PointTransactionResponse,
} from '../api/types';

type ContentTab = 'activities' | 'transactions';
type RecordKind = 'earn' | 'spend';
type PointRecord = { id: number; date: string; time: string; title: string; detail: string; amount: number; kind: RecordKind };
type CalendarDay = { iso: string; label: number; inMonth: boolean; today: boolean; signed: boolean };

const router = useRouter();
const loading = ref(true);
const error = ref('');
const balance = ref(0);
const activeTab = ref<ContentTab>('activities');
const activeFilter = ref<'all' | RecordKind>('all');
const checkIn = ref<DailyCheckInResponse | null>(null);
const checkingIn = ref(false);
const checkInError = ref('');
const checkInFeedback = ref('');
const records = ref<PointRecord[]>([]);

const filteredRecords = computed(() => activeFilter.value === 'all'
  ? records.value
  : records.value.filter(record => record.kind === activeFilter.value));
const groupedRecords = computed(() => {
  const groups = new Map<string, PointRecord[]>();
  filteredRecords.value.forEach(record => groups.set(record.date, [...(groups.get(record.date) ?? []), record]));
  return [...groups.entries()].map(([date, entries]) => ({ date, entries }));
});
const formattedBalance = computed(() => new Intl.NumberFormat('zh-CN').format(balance.value));
const checkedInToday = computed(() => checkIn.value?.checkedInToday ?? false);
const monthLabel = computed(() => {
  const date = parseBusinessDate(checkIn.value?.businessDate);
  return `${date.getFullYear()}年${date.getMonth() + 1}月`;
});
const calendarDays = computed<CalendarDay[]>(() => {
  const current = parseBusinessDate(checkIn.value?.businessDate);
  const year = current.getFullYear();
  const month = current.getMonth();
  const monthStart = new Date(year, month, 1);
  const mondayOffset = (monthStart.getDay() + 6) % 7;
  const gridStart = new Date(year, month, 1 - mondayOffset);
  const signed = new Set(checkIn.value?.signedDates ?? []);
  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(gridStart);
    date.setDate(gridStart.getDate() + index);
    const iso = toLocalIsoDate(date);
    return {
      iso,
      label: date.getDate(),
      inMonth: date.getMonth() === month,
      today: iso === checkIn.value?.businessDate,
      signed: signed.has(iso),
    };
  });
});

function parseBusinessDate(value?: string) {
  const fallback = new Date();
  if (!value) return fallback;
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function toLocalIsoDate(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function getErrorMessage(requestError: unknown, fallback = '暂时无法获取积分信息，请稍后重试') {
  if (axios.isAxiosError(requestError)) {
    const data = requestError.response?.data as ApiError | undefined;
    return data?.message || fallback;
  }
  return fallback;
}

async function loadPoints() {
  loading.value = true;
  error.value = '';
  try {
    const userId = sessionStorage.getItem('currentUserId');
    if (!userId) { await router.replace('/login'); return; }
    const [balanceResponse, transactionsResponse, checkInResponse] = await Promise.all([
      http.get<PointBalanceResponse>(`/points/users/${userId}/balance`),
      http.get<PointTransactionPageResponse>(`/points/users/${userId}/transactions`, { params: { page: 0, size: 100 } }),
      http.get<DailyCheckInResponse>(`/activities/check-ins/users/${userId}`),
    ]);
    balance.value = balanceResponse.data.balance;
    records.value = transactionsResponse.data.items.map(toPointRecord);
    checkIn.value = checkInResponse.data;
  } catch (requestError) {
    if (axios.isAxiosError(requestError) && requestError.response?.status === 401) { await router.replace('/login'); return; }
    error.value = getErrorMessage(requestError);
  } finally {
    loading.value = false;
  }
}

async function performCheckIn() {
  if (checkingIn.value || checkedInToday.value) return;
  const userId = sessionStorage.getItem('currentUserId');
  if (!userId) { await router.replace('/login'); return; }
  checkingIn.value = true;
  checkInError.value = '';
  checkInFeedback.value = '';
  try {
    const response = await http.post<DailyCheckInResponse>(`/activities/check-ins/users/${userId}`);
    checkIn.value = response.data;
    if (response.data.balanceAfter !== null) balance.value = response.data.balanceAfter;
    const [balanceResponse, transactionsResponse] = await Promise.all([
      http.get<PointBalanceResponse>(`/points/users/${userId}/balance`),
      http.get<PointTransactionPageResponse>(`/points/users/${userId}/transactions`, { params: { page: 0, size: 100 } }),
    ]);
    balance.value = balanceResponse.data.balance;
    records.value = transactionsResponse.data.items.map(toPointRecord);
    checkInFeedback.value = `签到成功，获得 ${response.data.rewardPoints} 积分`;
  } catch (requestError) {
    checkInError.value = getErrorMessage(requestError, '签到未完成，请稍后重试');
  } finally {
    checkingIn.value = false;
  }
}

function toPointRecord(transaction: PointTransactionResponse): PointRecord {
  const createdAt = new Date(transaction.createdAt);
  const kind: RecordKind = transaction.type === 'CREDIT' ? 'earn' : 'spend';
  return {
    id: transaction.transactionId,
    date: new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }).format(createdAt),
    time: new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false }).format(createdAt),
    title: transaction.source === 'CHECK_IN' ? '每日签到' : transaction.source,
    detail: transaction.remark || `变动后余额 ${transaction.balanceAfter}`,
    amount: kind === 'earn' ? transaction.amount : -transaction.amount,
    kind,
  };
}

onMounted(loadPoints);
</script>

<template>
  <div class="points-page">
    <svg class="points-defs" aria-hidden="true">
      <symbol id="points-user" viewBox="0 0 24 24"><circle cx="12" cy="8" r="3.5"/><path d="M5 20c.5-4 3-6 7-6s6.5 2 7 6"/></symbol>
      <symbol id="points-coin" viewBox="0 0 24 24"><circle cx="12" cy="12" r="8"/><path d="M9 9.5h4.2a2 2 0 0 1 0 4H10.8a2 2 0 0 0 0 4H15M12 7v2.5M12 17.5V20"/></symbol>
      <symbol id="points-gift" viewBox="0 0 24 24"><path d="M4 10h16v10H4zM3 7h18v3H3zM12 7v13M7.5 7C5 7 5 3.5 7.5 3.5c2 0 4.5 3.5 4.5 3.5s2.5-3.5 4.5-3.5C19 3.5 19 7 16.5 7"/></symbol>
      <symbol id="points-bag" viewBox="0 0 24 24"><path d="M5 8h14l-1 12H6L5 8Z"/><path d="M9 9V6a3 3 0 0 1 6 0v3"/></symbol>
      <symbol id="points-calendar" viewBox="0 0 24 24"><rect x="3.5" y="5" width="17" height="16" rx="2"/><path d="M8 3v4M16 3v4M3.5 10h17M8 15l2.5 2.5L16 12"/></symbol>
      <symbol id="points-arrow" viewBox="0 0 24 24"><path d="M5 12h14M14 7l5 5-5 5"/></symbol>
      <symbol id="points-plus" viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></symbol>
      <symbol id="points-minus" viewBox="0 0 24 24"><path d="M6 12h12"/></symbol>
    </svg>

    <AccountHeader active="points" />

    <main class="workspace" aria-live="polite">
      <section v-if="loading" class="state-panel"><span class="loader" aria-hidden="true"></span><strong>正在同步积分权益</strong><p>请稍候，正在获取积分、签到与活动信息。</p></section>
      <section v-else-if="error" class="state-panel error-state"><strong>积分权益暂不可用</strong><p>{{ error }}</p><button type="button" @click="loadPoints">重新加载</button></section>

      <template v-else>
        <section class="balance-panel" aria-labelledby="balance-title">
          <div class="balance-copy"><span id="balance-title">当前可用积分</span><strong>{{ formattedBalance }}</strong><p>积分可用于参与活动或兑换商品，以实际规则为准。</p></div>
          <div class="check-in-area">
            <div class="check-in-heading"><svg><use href="#points-calendar" /></svg><div><h1>每日签到</h1><p>每天签到，积累下一次收获</p></div></div>
            <button class="check-in-button" type="button" :disabled="checkingIn || checkedInToday" @click="performCheckIn">
              <span v-if="checkingIn" class="button-spinner" aria-hidden="true"></span>
              <span>{{ checkingIn ? '签到处理中' : checkedInToday ? '今日已签到' : '今日签到' }}</span>
              <b v-if="!checkingIn">+{{ checkIn?.rewardPoints ?? 10 }}</b>
            </button>
            <p class="streak-copy">连续签到 <strong>{{ checkIn?.currentStreak ?? 0 }}</strong> 天</p>
            <p v-if="checkInFeedback" class="check-in-message success" role="status">{{ checkInFeedback }}</p>
            <p v-if="checkInError" class="check-in-message error" role="alert">{{ checkInError }}</p>
          </div>
        </section>

        <section id="activities" class="content-panel">
          <header class="content-header">
            <div><h2>{{ activeTab === 'activities' ? '我的活动' : '积分明细' }}</h2><p>{{ activeTab === 'activities' ? '查看签到进度，发现可参与的积分活动。' : '清楚查看每一笔积分的获得与使用。' }}</p></div>
            <div class="content-tabs" role="tablist" aria-label="积分内容切换">
              <button role="tab" type="button" :aria-selected="activeTab === 'activities'" :class="{ selected: activeTab === 'activities' }" @click="activeTab = 'activities'">活动</button>
              <button role="tab" type="button" :aria-selected="activeTab === 'transactions'" :class="{ selected: activeTab === 'transactions' }" @click="activeTab = 'transactions'">积分明细</button>
            </div>
          </header>

          <div v-if="activeTab === 'activities'" class="activities-layout" role="tabpanel">
            <section class="calendar-panel" aria-labelledby="calendar-title">
              <div class="calendar-heading"><h3 id="calendar-title">签到日历</h3><strong>{{ monthLabel }}</strong></div>
              <div class="weekdays" aria-hidden="true"><span>一</span><span>二</span><span>三</span><span>四</span><span>五</span><span>六</span><span>日</span></div>
              <div class="calendar-grid">
                <time v-for="day in calendarDays" :key="day.iso" :datetime="day.iso" :class="{ muted: !day.inMonth, today: day.today, signed: day.signed }">
                  <span>{{ day.label }}</span><i v-if="day.signed" aria-label="已签到">✓</i>
                </time>
              </div>
              <div class="calendar-summary"><svg><use href="#points-calendar" /></svg><span>本月已签到 <strong>{{ checkIn?.signedDates.length ?? 0 }}</strong> 天</span></div>
            </section>

            <div class="activity-cards">
              <article class="activity-card">
                <div class="activity-icon"><svg><use href="#points-gift" /></svg></div>
                <div><h3>幸运抽奖</h3><p>使用积分，抽取惊喜好礼</p><button type="button" disabled>去抽奖 · 即将开放</button></div>
              </article>
              <article class="activity-card">
                <div class="activity-icon"><svg><use href="#points-bag" /></svg></div>
                <div><h3>积分兑换</h3><p>用积分兑换心仪好物</p><button type="button" disabled>去兑换 · 即将开放</button></div>
              </article>
            </div>
          </div>

          <div v-else class="ledger-content" role="tabpanel">
            <div class="ledger-toolbar"><span>共 {{ records.length }} 条记录</span><div class="filters" role="group" aria-label="积分流水筛选"><button :class="{ selected: activeFilter === 'all' }" type="button" @click="activeFilter = 'all'">全部</button><button :class="{ selected: activeFilter === 'earn' }" type="button" @click="activeFilter = 'earn'">获得</button><button :class="{ selected: activeFilter === 'spend' }" type="button" @click="activeFilter = 'spend'">使用</button></div></div>
            <div v-if="groupedRecords.length" class="ledger-list">
              <section v-for="group in groupedRecords" :key="group.date" class="date-group">
                <h3>{{ group.date }}</h3>
                <ul><li v-for="record in group.entries" :key="record.id"><span class="record-icon" :class="record.kind"><svg><use :href="record.kind === 'earn' ? '#points-plus' : '#points-minus'" /></svg></span><div class="record-copy"><strong>{{ record.title }}</strong><span>{{ record.detail }}</span></div><time>{{ record.time }}</time><b :class="record.kind">{{ record.amount > 0 ? '+' : '' }}{{ record.amount }}</b></li></ul>
              </section>
            </div>
            <div v-else class="empty-state"><strong>暂无积分记录</strong><p>完成签到或使用积分后，记录会显示在这里。</p></div>
          </div>
        </section>
        <footer><span>积分规则</span><span>活动说明</span><span>隐私说明</span><em>© 偶得 · 账户中心</em></footer>
      </template>
    </main>
  </div>
</template>

<style scoped>
.points-page{--blue:#174fa7;--navy:#102f65;--ink:#17213a;--paper:#f8f7f2;--line:#d7dde6;--yellow:#f1c84a;--muted:#697287;min-width:1180px;min-height:100vh;background:#e8edf3;color:var(--ink)}
.points-page ::selection{color:#fff;background:#174fa7}.points-page svg{width:20px;height:20px;fill:none;stroke:currentColor;stroke-width:1.7;stroke-linecap:round;stroke-linejoin:round}.points-defs{position:absolute;width:0!important;height:0!important;overflow:hidden}
.top-nav{display:flex;height:74px;align-items:center;padding:0 48px;color:#eef4ff;background:var(--navy);box-shadow:0 2px 8px rgb(16 47 101 / 14%)}.brand{display:flex;align-items:center;gap:12px;flex:0 0 180px;color:#fff;font-size:18px;font-weight:700;text-decoration:none}.brand-mark{display:grid;width:34px;height:34px;place-items:center;color:var(--navy);background:var(--yellow);border-radius:9px 9px 4px 9px;font-size:17px;font-weight:800}.top-nav nav{display:flex;align-items:center;gap:6px;height:100%}.top-nav nav a{display:flex;align-items:center;gap:10px;height:42px;padding:0 15px;color:#afc0dc;border-radius:10px;font-size:14px;text-decoration:none;transition:color .18s ease,background .18s ease}.top-nav nav a:hover{color:#fff;background:#183f7b}.top-nav nav a:focus-visible,.brand:focus-visible{outline:3px solid rgb(241 200 74 / 55%);outline-offset:3px}.top-nav nav a.active{color:var(--navy);background:var(--yellow);font-weight:700}.top-nav nav a svg{width:18px}.top-support{display:flex;align-items:baseline;gap:10px;margin-left:auto;color:#9eb1ce;font-size:12px}.top-support strong{color:#f4f7fd;font-size:14px;font-variant-numeric:tabular-nums}
.workspace{max-width:1340px;min-height:calc(100vh - 74px);margin:0 auto;padding:34px 48px 28px}.balance-panel{display:grid;grid-template-columns:minmax(0,1.2fr) minmax(390px,.8fr);min-height:220px;color:#fff;background:var(--blue);border-radius:16px;box-shadow:0 18px 40px rgb(31 54 94 / 16%)}.balance-copy{display:grid;align-content:center;padding:36px 54px}.balance-copy>span{color:#c9d9f3;font-size:13px;font-weight:600}.balance-copy>strong{margin:5px 0;font-size:64px;line-height:1.05;letter-spacing:-.035em;font-variant-numeric:tabular-nums}.balance-copy p{margin:0;color:#c9d9f3;font-size:13px}.check-in-area{display:grid;align-content:center;padding:30px 50px;border-left:1px solid rgb(255 255 255 / 22%)}.check-in-heading{display:flex;align-items:center;gap:15px}.check-in-heading>svg{width:42px;height:42px;color:#c9d9f3}.check-in-heading h1{margin:0 0 3px;font-size:21px;letter-spacing:-.015em}.check-in-heading p{margin:0;color:#c9d9f3;font-size:12px}.check-in-button{display:flex;height:50px;align-items:center;justify-content:center;gap:12px;margin-top:17px;color:var(--blue);background:#fff;border:0;border-radius:9px;box-shadow:0 9px 20px rgb(7 39 91 / 22%);font-size:14px;font-weight:700;transition:transform .18s ease,box-shadow .18s ease}.check-in-button:hover:not(:disabled){transform:translateY(-1px);box-shadow:0 12px 25px rgb(7 39 91 / 28%)}.check-in-button:focus-visible,.content-tabs button:focus-visible,.filters button:focus-visible{outline:3px solid rgb(241 200 74 / 65%);outline-offset:3px}.check-in-button:disabled{color:#526e9c;background:#eaf0f9;box-shadow:none;cursor:default}.check-in-button b{color:var(--blue);font-size:16px}.streak-copy{margin:11px 0 0;color:#c9d9f3;font-size:13px}.streak-copy strong{color:var(--yellow);font-size:16px}.check-in-message{margin:6px 0 0;font-size:12px}.check-in-message.success{color:#d8f0df}.check-in-message.error{color:#ffe1e4}.button-spinner{width:16px;height:16px;border:2px solid rgb(23 79 167 / 25%);border-top-color:var(--blue);border-radius:50%;animation:spin .7s linear infinite}
.content-panel{margin-top:22px;overflow:hidden;background:var(--paper);border-radius:16px;box-shadow:0 10px 28px rgb(38 50 75 / 9%)}.content-header{display:flex;align-items:center;justify-content:space-between;padding:24px 32px;border-bottom:1px solid var(--line)}.content-header h2{margin:0 0 4px;font-size:24px;letter-spacing:-.02em}.content-header p{margin:0;color:var(--muted);font-size:13px}.content-tabs,.filters{display:flex;gap:4px;padding:4px;background:#e8edf3;border-radius:9px}.content-tabs button{min-width:106px;height:38px;color:#647087;background:transparent;border:0;border-radius:6px;font:inherit;font-size:13px;font-weight:700}.content-tabs button.selected,.filters button.selected{color:var(--navy);background:var(--yellow)}
.activities-layout{display:grid;grid-template-columns:minmax(0,1.65fr) minmax(330px,.85fr);gap:22px;padding:24px 32px 30px}.calendar-panel{border:1px solid var(--line);border-radius:12px}.calendar-heading{display:flex;align-items:center;justify-content:space-between;padding:21px 24px 15px}.calendar-heading h3,.activity-card h3{margin:0;font-size:18px}.calendar-heading>strong{font-size:18px;font-variant-numeric:tabular-nums}.weekdays,.calendar-grid{display:grid;grid-template-columns:repeat(7,1fr);text-align:center}.weekdays{padding:0 18px;color:#68758b;font-size:12px;font-weight:700}.weekdays span{padding:8px 0}.calendar-grid{padding:0 18px 14px}.calendar-grid time{position:relative;display:grid;height:48px;place-items:center;color:#25314b;font-size:13px;font-style:normal;font-variant-numeric:tabular-nums}.calendar-grid time.muted{color:#aab3c3}.calendar-grid time.today span{display:grid;width:31px;height:31px;place-items:center;color:var(--navy);background:var(--yellow);border-radius:50%;font-weight:800}.calendar-grid time.signed:not(.today) span{font-weight:700}.calendar-grid time i{position:absolute;right:16%;bottom:5px;display:grid;width:16px;height:16px;place-items:center;color:#fff;background:var(--blue);border:2px solid var(--paper);border-radius:50%;font-size:9px;font-style:normal}.calendar-summary{display:flex;min-height:54px;align-items:center;justify-content:center;gap:9px;color:var(--muted);border-top:1px solid var(--line);font-size:13px}.calendar-summary svg{width:18px;color:var(--blue)}.calendar-summary strong{color:var(--blue);font-size:16px}.activity-cards{display:grid;grid-template-rows:1fr 1fr;gap:16px}.activity-card{display:grid;grid-template-columns:92px 1fr;align-items:center;gap:20px;padding:24px;border:1px solid var(--line);border-radius:12px;background:#fff}.activity-icon{display:grid;width:84px;height:84px;place-items:center;color:var(--blue);background:#edf3fb;border-radius:16px 16px 6px 16px}.activity-icon svg{width:43px;height:43px}.activity-card p{margin:7px 0 15px;color:var(--muted);font-size:13px}.activity-card button{height:36px;padding:0 13px;color:#647087;background:#e8edf3;border:0;border-radius:7px;font-size:12px;font-weight:700;cursor:not-allowed}
.ledger-toolbar{display:flex;align-items:center;justify-content:space-between;padding:18px 32px 6px;color:var(--muted);font-size:12px}.filters button{min-width:58px;height:32px;color:#647087;background:transparent;border:0;border-radius:6px;font:inherit;font-size:12px;font-weight:700}.ledger-list{padding:0 32px 18px}.date-group h3{margin:22px 0 8px;color:var(--muted);font-size:12px}.date-group ul{margin:0;padding:0;list-style:none}.date-group li{display:grid;grid-template-columns:42px minmax(0,1fr) 72px 82px;align-items:center;min-height:72px;border-bottom:1px solid #e4e8ee}.record-icon{display:grid;width:30px;height:30px;place-items:center;border-radius:9px}.record-icon svg{width:16px}.record-icon.earn{color:#315e3f;background:#e2efe4}.record-icon.spend{color:#7a5c1b;background:#f9edbf}.record-copy{display:grid;gap:3px}.record-copy strong{font-size:14px}.record-copy span,.date-group time{color:var(--muted);font-size:12px}.date-group b{font-size:15px;text-align:right;font-variant-numeric:tabular-nums}.date-group b.earn{color:#237346}.date-group b.spend{color:#9a6c16}.empty-state{padding:82px 32px;color:var(--muted);text-align:center}.empty-state strong{display:block;margin-bottom:6px;color:var(--ink);font-size:16px}.empty-state p{margin:0;font-size:13px}
footer{display:flex;gap:24px;align-items:center;padding:24px 4px 0;color:#747d90;font-size:11px}footer em{margin-left:auto;font-style:normal}.state-panel{display:grid;min-height:420px;place-content:center;justify-items:center;gap:10px;text-align:center}.state-panel strong{font-size:21px}.state-panel p{max-width:380px;margin:0;color:var(--muted);font-size:14px}.state-panel button{margin-top:8px;padding:11px 18px;color:#fff;background:var(--blue);border:0;border-radius:9px;font-weight:700}.error-state strong{color:#a23843}.loader{width:28px;height:28px;border:3px solid #c7d5ed;border-top-color:var(--blue);border-radius:50%;animation:spin .7s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}
@media(prefers-reduced-motion:reduce){.loader,.button-spinner{animation:none}.top-nav nav a,.check-in-button{transition:none}}
</style>
