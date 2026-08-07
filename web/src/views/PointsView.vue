<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { http } from '../api/http';

type Profile = { points?: number | string; pointBalance?: number | string };
type RecordKind = 'earn' | 'spend';
type PointRecord = { id: number; date: string; time: string; title: string; detail: string; amount: number; kind: RecordKind };

const router = useRouter();
const loading = ref(true);
const error = ref('');
const balance = ref(0);
const activeFilter = ref<'all' | RecordKind>('all');

// 积分流水将在对应接口接入后写入此列表。
const records = ref<PointRecord[]>([]);

const filteredRecords = computed(() => activeFilter.value === 'all' ? records.value : records.value.filter(record => record.kind === activeFilter.value));
const groupedRecords = computed(() => {
  const groups = new Map<string, PointRecord[]>();
  filteredRecords.value.forEach(record => groups.set(record.date, [...(groups.get(record.date) ?? []), record]));
  return [...groups.entries()].map(([date, entries]) => ({ date, entries }));
});
const formattedBalance = computed(() => new Intl.NumberFormat('zh-CN').format(balance.value));

function getErrorMessage(requestError: unknown) {
  if (axios.isAxiosError(requestError)) {
    const data = requestError.response?.data as { message?: string; error?: string } | undefined;
    return data?.message || data?.error || '暂时无法获取积分余额，请稍后重试';
  }
  return '暂时无法获取积分余额，请稍后重试';
}

async function loadPoints() {
  loading.value = true;
  error.value = '';
  try {
    const userId = sessionStorage.getItem('currentUserId');
    if (!userId) { await router.replace('/login'); return; }
    const response = await http.get(`/users/${userId}`);
    const profile = (response.data?.data ?? response.data) as Profile;
    balance.value = Number(profile?.points ?? profile?.pointBalance ?? 0) || 0;
  } catch (requestError) {
    if (axios.isAxiosError(requestError) && requestError.response?.status === 401) { await router.replace('/login'); return; }
    error.value = getErrorMessage(requestError);
  } finally { loading.value = false; }
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
      <symbol id="points-arrow" viewBox="0 0 24 24"><path d="M5 12h14M14 7l5 5-5 5"/></symbol>
      <symbol id="points-plus" viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></symbol>
      <symbol id="points-minus" viewBox="0 0 24 24"><path d="M6 12h12"/></symbol>
    </svg>

    <header class="top-nav">
      <RouterLink class="brand" to="/profile"><span class="brand-mark">偶</span><span>偶得</span></RouterLink>
      <nav aria-label="主导航">
        <RouterLink to="/profile"><svg><use href="#points-user" /></svg><span>用户信息</span></RouterLink>
        <RouterLink to="/points" class="active"><svg><use href="#points-coin" /></svg><span>积分明细</span></RouterLink>
        <a href="#" @click.prevent><svg><use href="#points-gift" /></svg><span>幸运抽奖</span></a>
        <a href="#" @click.prevent><svg><use href="#points-bag" /></svg><span>兑换商城</span></a>
      </nav>
      <div class="top-support"><span>服务中心</span><strong>400 · 888 · 2608</strong></div>
    </header>

    <main class="workspace" aria-live="polite">
      <section v-if="loading" class="state-panel"><span class="loader" aria-hidden="true"></span><strong>正在同步积分信息</strong><p>请稍候，正在获取你的当前积分余额。</p></section>
      <section v-else-if="error" class="state-panel error-state"><strong>积分信息暂不可用</strong><p>{{ error }}</p><button type="button" @click="loadPoints">重新加载</button></section>

      <template v-else>
        <div class="page-heading"><div><h1>积分明细</h1><p>清楚查看每一笔积分的获得与使用。</p></div><RouterLink to="/profile">返回账户信息 <svg><use href="#points-arrow" /></svg></RouterLink></div>

        <section class="balance-panel" aria-labelledby="balance-title">
          <div class="balance-copy"><span id="balance-title">当前可用积分</span><strong>{{ formattedBalance }}</strong><p>积分可用于参与活动或兑换商品，以实际规则为准。</p></div>
          <div class="balance-actions"><a href="#" @click.prevent><svg><use href="#points-gift" /></svg><span>去抽奖</span></a><a href="#" @click.prevent><svg><use href="#points-bag" /></svg><span>去兑换</span></a></div>
        </section>

        <section class="ledger-panel" aria-labelledby="ledger-title">
          <header class="ledger-header"><div><h2 id="ledger-title">积分流水</h2><p>查看每一笔积分的获得与使用。</p></div><div class="filters" role="group" aria-label="积分流水筛选"><button :class="{ selected: activeFilter === 'all' }" type="button" @click="activeFilter = 'all'">全部</button><button :class="{ selected: activeFilter === 'earn' }" type="button" @click="activeFilter = 'earn'">获得</button><button :class="{ selected: activeFilter === 'spend' }" type="button" @click="activeFilter = 'spend'">使用</button></div></header>

          <div v-if="groupedRecords.length" class="ledger-list">
            <section v-for="group in groupedRecords" :key="group.date" class="date-group">
              <h3>{{ group.date }}</h3>
              <ul>
                <li v-for="record in group.entries" :key="record.id">
                  <span class="record-icon" :class="record.kind"><svg><use :href="record.kind === 'earn' ? '#points-plus' : '#points-minus'" /></svg></span>
                  <div class="record-copy"><strong>{{ record.title }}</strong><span>{{ record.detail }}</span></div>
                  <time>{{ record.time }}</time>
                  <b :class="record.kind">{{ record.amount > 0 ? '+' : '' }}{{ record.amount }}</b>
                </li>
              </ul>
            </section>
          </div>
          <div v-else class="empty-state"><strong>暂无积分记录</strong><p>后续获得或使用积分后，记录会显示在这里。</p></div>
        </section>
        <footer><span>积分规则</span><span>兑换帮助</span><span>隐私说明</span><em>© 偶得 · 账户中心</em></footer>
      </template>
    </main>
  </div>
</template>

<style scoped>
.points-page{--blue:#174fa7;--navy:#102f65;--ink:#17213a;--paper:#f8f7f2;--line:#d7dde6;--yellow:#f1c84a;min-width:1180px;min-height:100vh;background:#e8edf3;color:var(--ink)}.points-page svg{width:20px;height:20px;fill:none;stroke:currentColor;stroke-width:1.7;stroke-linecap:round;stroke-linejoin:round}.points-defs{position:absolute;width:0!important;height:0!important;overflow:hidden}.top-nav{display:flex;height:74px;align-items:center;padding:0 48px;color:#eef4ff;background:var(--navy);box-shadow:0 2px 8px rgb(16 47 101 / 14%)}.brand{display:flex;align-items:center;gap:12px;flex:0 0 180px;color:#fff;font-size:18px;font-weight:700;text-decoration:none}.brand-mark{display:grid;width:34px;height:34px;place-items:center;color:var(--navy);background:var(--yellow);border-radius:9px 9px 4px 9px;font-size:17px;font-weight:800}.top-nav nav{display:flex;align-items:center;gap:6px;height:100%}.top-nav nav a{display:flex;align-items:center;gap:10px;height:42px;padding:0 15px;color:#afc0dc;border-radius:10px;font-size:14px;text-decoration:none;transition:.18s ease}.top-nav nav a:hover,.top-nav nav a:focus-visible{color:#fff;background:#183f7b;outline:none}.top-nav nav a.active{color:var(--navy);background:var(--yellow);font-weight:700}.top-nav nav a svg{width:18px}.top-support{display:flex;align-items:baseline;gap:10px;margin-left:auto;color:#9eb1ce;font-size:12px}.top-support strong{color:#f4f7fd;font-size:14px;font-variant-numeric:tabular-nums}.workspace{max-width:1340px;min-height:calc(100vh - 74px);margin:0 auto;padding:34px 48px 28px}.page-heading{display:flex;align-items:end;justify-content:space-between;margin:0 0 20px}.page-heading h1{margin:0 0 6px;font-size:34px;letter-spacing:-.025em}.page-heading p{margin:0;color:#697287;font-size:14px}.page-heading>a{display:flex;align-items:center;gap:8px;color:var(--blue);font-size:13px;font-weight:700;text-decoration:none}.page-heading>a svg{width:17px;transition:transform .18s ease}.page-heading>a:hover svg{transform:translateX(3px)}.balance-panel{display:flex;align-items:center;min-height:182px;padding:30px 38px;color:#fff;background:var(--blue);border-radius:16px;box-shadow:0 18px 40px rgb(31 54 94 / 16%)}.balance-copy{flex:1}.balance-copy>span{color:#c9d9f3;font-size:13px}.balance-copy strong{display:block;margin:3px 0 5px;font-size:48px;line-height:1.05;letter-spacing:-.035em;font-variant-numeric:tabular-nums}.balance-copy p{margin:0;color:#c9d9f3;font-size:13px}.balance-actions{display:flex;gap:10px;padding-left:42px;border-left:1px solid rgb(255 255 255 / 22%)}.balance-actions a{display:flex;min-width:126px;min-height:88px;flex-direction:column;align-items:center;justify-content:center;gap:9px;color:#fff;background:rgb(7 39 91 / 28%);border-radius:12px;font-size:13px;font-weight:700;text-decoration:none;transition:background .18s ease,transform .18s ease}.balance-actions a:hover{background:rgb(7 39 91 / 48%);transform:translateY(-1px)}.balance-actions svg{width:23px}.ledger-panel{margin-top:22px;background:var(--paper);border-radius:16px;box-shadow:0 10px 28px rgb(38 50 75 / 9%)}.ledger-header{display:flex;align-items:center;justify-content:space-between;padding:27px 32px 22px;border-bottom:1px solid var(--line)}.ledger-header h2{margin:0 0 5px;font-size:21px;letter-spacing:-.015em}.ledger-header p{margin:0;color:#697287;font-size:13px;line-height:1.7}.filters{display:flex;gap:4px;padding:4px;background:#e8edf3;border-radius:9px}.filters button{min-width:58px;height:32px;color:#647087;background:transparent;border:0;border-radius:6px;font:inherit;font-size:12px;font-weight:700;transition:.18s ease}.filters button:hover{color:var(--blue)}.filters button.selected{color:var(--navy);background:var(--yellow)}.filters button:focus-visible,.balance-actions a:focus-visible,.page-heading>a:focus-visible{outline:3px solid rgb(23 79 167 / 28%);outline-offset:3px}.ledger-list{padding:6px 32px 18px}.date-group h3{margin:24px 0 8px;color:#697287;font-size:12px;font-weight:700}.date-group ul{margin:0;padding:0;list-style:none}.date-group li{display:grid;grid-template-columns:42px minmax(0,1fr) 72px 82px;align-items:center;min-height:72px;border-bottom:1px solid #e4e8ee}.date-group li:last-child{border-bottom:0}.record-icon{display:grid;width:30px;height:30px;place-items:center;border-radius:9px}.record-icon svg{width:16px}.record-icon.earn{color:#315e3f;background:#e2efe4}.record-icon.spend{color:#7a5c1b;background:#f9edbf}.record-copy{display:grid;gap:3px}.record-copy strong{font-size:14px}.record-copy span{color:#697287;font-size:12px}.date-group time{color:#697287;font-size:12px;font-variant-numeric:tabular-nums}.date-group b{font-size:15px;text-align:right;font-variant-numeric:tabular-nums}.date-group b.earn{color:#237346}.date-group b.spend{color:#9a6c16}.empty-state{padding:68px 32px;color:#697287;text-align:center}.empty-state strong{display:block;margin-bottom:6px;color:var(--ink);font-size:16px}.empty-state p{margin:0;font-size:13px}footer{display:flex;gap:24px;align-items:center;padding:24px 4px 0;color:#747d90;font-size:11px}footer em{margin-left:auto;font-style:normal}.state-panel{display:grid;min-height:360px;place-content:center;justify-items:center;gap:10px;color:#17213a;text-align:center}.state-panel strong{font-size:21px}.state-panel p{max-width:360px;margin:0;color:#697287;font-size:14px;line-height:1.7}.state-panel button{margin-top:8px;padding:11px 18px;color:#fff;background:var(--blue);border:0;border-radius:9px;font-size:13px;font-weight:700}.error-state strong{color:#a23843}.loader{width:28px;height:28px;border:3px solid #c7d5ed;border-top-color:var(--blue);border-radius:50%;animation:spin .7s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}@media(prefers-reduced-motion:reduce){.loader{animation:none}.top-nav nav a,.balance-actions a,.filters button,.page-heading>a svg{transition:none}}
</style>
