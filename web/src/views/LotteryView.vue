<script setup lang="ts">
import { computed, ref } from 'vue';
import AccountHeader from '../components/AccountHeader.vue';

type Prize = { name: string; kind: string; tone: 'blue' | 'yellow' | 'quiet' };
type Activity = { code: string; name: string; label: string; description: string; cost: number; limit: string; prizes: Prize[] };

const activities: Activity[] = [
  {
    code: 'weekly-lucky', name: '幸运抽奖', label: '日常参与', description: '每日参与，解锁随机收获', cost: 100, limit: '今日剩余 3 次',
    prizes: [
      { name: '会员权益体验', kind: '虚拟权益', tone: 'blue' }, { name: '20 积分奖励', kind: '积分奖励', tone: 'yellow' }, { name: '谢谢参与', kind: '未中奖', tone: 'quiet' },
      { name: '优惠券权益', kind: '虚拟权益', tone: 'blue' }, { name: '50 积分奖励', kind: '积分奖励', tone: 'yellow' }, { name: '专属服务礼包', kind: '虚拟权益', tone: 'blue' },
    ],
  },
  {
    code: 'newcomer-gift', name: '新人见面礼', label: '新用户专享', description: '首次参与，开启偶得权益', cost: 50, limit: '还可参与 1 次',
    prizes: [
      { name: '新人专属礼包', kind: '虚拟权益', tone: 'blue' }, { name: '10 积分奖励', kind: '积分奖励', tone: 'yellow' }, { name: '谢谢参与', kind: '未中奖', tone: 'quiet' },
      { name: '服务体验券', kind: '虚拟权益', tone: 'blue' }, { name: '30 积分奖励', kind: '积分奖励', tone: 'yellow' }, { name: '会员试用权益', kind: '虚拟权益', tone: 'blue' },
    ],
  },
  {
    code: 'member-weekend', name: '周末会员礼', label: '会员专享', description: '限时开放的会员权益抽取', cost: 200, limit: '今日剩余 2 次',
    prizes: [
      { name: '会员服务礼包', kind: '虚拟权益', tone: 'blue' }, { name: '100 积分奖励', kind: '积分奖励', tone: 'yellow' }, { name: '谢谢参与', kind: '未中奖', tone: 'quiet' },
      { name: '专属优惠权益', kind: '虚拟权益', tone: 'blue' }, { name: '60 积分奖励', kind: '积分奖励', tone: 'yellow' }, { name: '会员延展体验', kind: '虚拟权益', tone: 'blue' },
    ],
  },
];

const activeCode = ref(activities[0].code);
const drawing = ref(false);
const resultOpen = ref(false);
const activePrize = ref(0);
const activity = computed(() => activities.find(item => item.code === activeCode.value) ?? activities[0]);
const selectedPrize = computed(() => activity.value.prizes[activePrize.value]);

function selectActivity(code: string) {
  if (drawing.value || code === activeCode.value) return;
  activeCode.value = code;
  activePrize.value = 0;
}

function draw() {
  if (drawing.value) return;
  drawing.value = true;
  window.setTimeout(() => {
    activePrize.value = Math.floor(Math.random() * activity.value.prizes.length);
    drawing.value = false;
    resultOpen.value = true;
  }, 650);
}
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

    <main class="activity-workspace">
      <div class="activity-kicker"><span>活动服务</span><i></i><span>演示数据 · 以实际活动配置为准</span></div>
      <header class="activity-heading"><div><p>进行中的抽奖活动</p><h1>{{ activity.name }}</h1><span>{{ activity.description }}；参与确认后将立即扣除积分。</span></div><div class="balance-chip"><svg><use href="#lottery-coin"/></svg><div><small>当前可用积分</small><strong>2,680</strong></div></div></header>

      <div class="lottery-layout">
        <section class="draw-counter" aria-labelledby="draw-title">
          <div class="counter-top"><div><h2 id="draw-title">本期收获窗</h2><span>{{ activity.label }} · {{ activity.limit }}</span></div><span class="live-state"><i></i>活动进行中</span></div>
          <div class="prize-grid" :class="{ drawing }">
            <article v-for="(prize, index) in activity.prizes" :key="prize.name" :class="['prize-cell', prize.tone, { selected: drawing && index === activePrize }]">
              <svg><use href="#lottery-gift"/></svg><strong>{{ prize.name }}</strong><small>{{ prize.kind }}</small>
            </article>
          </div>
          <div class="draw-action"><div><span>单次参与</span><strong>{{ activity.cost }} <small>积分</small></strong></div><button type="button" :disabled="drawing" @click="draw"><span v-if="drawing" class="button-loader"></span><span>{{ drawing ? '正在抽取结果…' : `使用 ${activity.cost} 积分参与` }}</span><svg v-if="!drawing"><use href="#lottery-arrow"/></svg></button></div>
          <p class="counter-caption">结果依据活动配置的权重产生；当前画面仅展示交互样例。</p>
        </section>

        <aside class="activity-catalog" aria-label="活动列表">
          <div class="catalog-heading"><div><h2>选择活动</h2><span>点击切换当前奖池</span></div><strong>{{ activities.length }} 项</strong></div>
          <button v-for="item in activities" :key="item.code" type="button" :class="['activity-card', { active: item.code === activeCode }]" :aria-pressed="item.code === activeCode" @click="selectActivity(item.code)">
            <span class="activity-card-mark"><svg><use href="#lottery-gift"/></svg></span>
            <span class="activity-card-copy"><small>{{ item.label }}</small><strong>{{ item.name }}</strong><em>{{ item.description }}</em></span>
            <span class="activity-card-meta"><b>{{ item.cost }} 积分</b><i>{{ item.limit }}</i></span>
          </button>
          <p class="catalog-note"><svg><use href="#lottery-check"/></svg><span>各活动的奖项、次数与积分成本，由活动配置分别决定。</span></p>
        </aside>
      </div>
      <section class="activity-footnote"><span>活动规则说明</span><p>一期仅校验活动状态、参与积分及每日参与次数。虚拟权益与积分奖励会创建待发奖记录；奖品库存与实际发放将在后续阶段接入。</p></section>
    </main>

    <div v-if="resultOpen" class="result-dialog" role="dialog" aria-modal="true" aria-labelledby="result-title" @click.self="resultOpen = false"><section><button class="dialog-close" type="button" aria-label="关闭结果" @click="resultOpen = false">×</button><span class="result-mark"><svg><use href="#lottery-gift"/></svg></span><p>{{ activity.name }} · 本次参与结果</p><h2 id="result-title">{{ selectedPrize.name }}</h2><span class="result-kind">{{ selectedPrize.kind }}</span><div class="result-note"><svg><use href="#lottery-check"/></svg><span>{{ selectedPrize.kind === '未中奖' ? '参与结果已记录，未创建待发奖任务。' : '结果已记录，奖励将以待发奖状态处理。' }}</span></div><button class="dialog-confirm" type="button" @click="resultOpen = false">我知道了</button></section></div>
  </div>
</template>

<style scoped>
.activity-page{--blue:#174fa7;--navy:#102f65;--yellow:#f1c84a;--paper:#f8f7f2;--ink:#17213a;--muted:#697287;--line:#d7dde6;min-width:1180px;min-height:100vh;background:#e8edf3;color:var(--ink)}.activity-page svg{width:20px;height:20px;fill:none;stroke:currentColor;stroke-width:1.7;stroke-linecap:round;stroke-linejoin:round}.activity-defs{position:absolute;width:0!important;height:0!important;overflow:hidden}.activity-workspace{width:min(1180px,calc(100% - 96px));margin:0 auto;padding:37px 0 48px}.activity-kicker{display:flex;align-items:center;gap:9px;color:#697287;font-size:12px}.activity-kicker span:first-child{color:var(--blue);font-weight:800}.activity-kicker i{width:4px;height:4px;background:#a6b0bf;border-radius:50%}.activity-heading{display:flex;align-items:end;justify-content:space-between;margin:14px 0 24px;padding-bottom:24px;border-bottom:1px solid var(--line)}.activity-heading p{margin:0 0 5px;color:var(--blue);font-size:13px;font-weight:700}.activity-heading h1{margin:0;font-size:34px;letter-spacing:-.025em}.activity-heading>div>span{display:block;margin-top:8px;color:var(--muted);font-size:13px}.balance-chip{display:flex;align-items:center;gap:12px;padding:12px 16px;background:var(--paper);border:1px solid #dfe4eb;border-radius:12px}.balance-chip svg{color:var(--blue)}.balance-chip small,.balance-chip strong{display:block}.balance-chip small{color:var(--muted);font-size:11px}.balance-chip strong{margin-top:2px;font-size:20px;font-variant-numeric:tabular-nums}.lottery-layout{display:grid;grid-template-columns:minmax(0,1fr) 330px;gap:22px}.draw-counter,.activity-catalog{background:var(--paper);border-radius:16px;box-shadow:0 10px 28px rgb(38 50 75 / 9%)}.draw-counter{padding:28px}.counter-top{display:flex;align-items:flex-start;justify-content:space-between}.counter-top h2,.catalog-heading h2{margin:0;font-size:21px;letter-spacing:-.015em}.counter-top>div>span,.catalog-heading span{display:block;margin-top:5px;color:var(--muted);font-size:11px}.live-state{display:flex;align-items:center;gap:6px;padding:5px 9px;color:#315e3f;background:#e2efe4;border-radius:6px;font-size:11px;font-weight:700}.live-state i{width:6px;height:6px;background:#4da572;border-radius:50%}.prize-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;margin:25px 0}.prize-cell{display:flex;min-height:116px;flex-direction:column;justify-content:center;padding:16px;border:1px solid #dfe5ee;border-radius:12px;background:#fff;transition:border-color .18s ease,transform .18s ease}.prize-cell svg{margin-bottom:10px;color:var(--blue)}.prize-cell strong{font-size:14px}.prize-cell small{margin-top:4px;color:var(--muted);font-size:11px}.prize-cell.yellow{background:#fffdf4}.prize-cell.yellow svg{color:#b07b08}.prize-cell.quiet{background:#f3f5f7}.prize-cell.quiet svg{color:#7d8798}.prize-grid.drawing .prize-cell{opacity:.6}.prize-grid.drawing .prize-cell.selected{opacity:1;border-color:var(--yellow);transform:translateY(-2px);box-shadow:0 7px 17px rgb(176 123 8 / 14%)}.draw-action{display:flex;align-items:center;justify-content:space-between;padding:17px 18px;background:var(--blue);border-radius:12px;color:#fff}.draw-action>div span,.draw-action>div strong{display:block}.draw-action>div span{color:#cbdaf3;font-size:11px}.draw-action>div strong{margin-top:1px;font-size:22px;font-variant-numeric:tabular-nums}.draw-action>div small{font-size:11px}.draw-action button,.dialog-confirm{display:flex;min-height:44px;align-items:center;justify-content:center;gap:8px;padding:0 17px;color:var(--navy);background:var(--yellow);border:0;border-radius:9px;font-size:13px;font-weight:800;transition:transform .18s ease}.draw-action button:hover:not(:disabled),.dialog-confirm:hover{transform:translateY(-1px)}.draw-action button:disabled{opacity:.8;cursor:wait}.draw-action button svg{width:17px}.button-loader{width:15px;height:15px;border:2px solid rgb(16 47 101 / 28%);border-top-color:var(--navy);border-radius:50%;animation:spin .7s linear infinite}.counter-caption{margin:14px 0 0;color:#788195;font-size:11px}.activity-catalog{padding:20px}.catalog-heading{display:flex;align-items:flex-start;justify-content:space-between;padding:4px 4px 15px;border-bottom:1px solid var(--line)}.catalog-heading strong{display:grid;min-width:38px;height:24px;place-items:center;color:var(--blue);background:#e2ebfa;border-radius:6px;font-size:11px}.activity-card{display:grid;grid-template-columns:32px minmax(0,1fr);gap:10px;width:100%;padding:15px 5px;color:var(--ink);text-align:left;background:transparent;border:0;border-bottom:1px solid var(--line);cursor:pointer;transition:background .18s ease,transform .18s ease}.activity-card:last-of-type{border-bottom:0}.activity-card:hover{background:#f0f3f8}.activity-card:focus-visible{outline:3px solid rgb(23 79 167 / 28%);outline-offset:2px}.activity-card.active{margin:10px 0;padding:14px 10px;background:#e8effa;border:1px solid #b8caea;border-radius:12px}.activity-card.active+.activity-card{border-top:1px solid var(--line)}.activity-card-mark{display:grid;width:30px;height:30px;place-items:center;color:var(--blue);background:#e2ebfa;border-radius:8px 8px 3px 8px}.activity-card.active .activity-card-mark{color:var(--navy);background:var(--yellow)}.activity-card-mark svg{width:16px}.activity-card-copy{min-width:0}.activity-card-copy small,.activity-card-copy strong,.activity-card-copy em{display:block}.activity-card-copy small{color:var(--blue);font-size:10px;font-weight:700}.activity-card-copy strong{margin:2px 0;color:var(--ink);font-size:13px}.activity-card-copy em{overflow:hidden;color:var(--muted);font-size:10px;font-style:normal;line-height:1.45;text-overflow:ellipsis;white-space:nowrap}.activity-card-meta{grid-column:2;display:flex;align-items:center;justify-content:space-between;margin-top:-4px}.activity-card-meta b{color:#536079;font-size:10px}.activity-card-meta i{color:#697287;font-size:10px;font-style:normal}.activity-card.active .activity-card-meta b{color:var(--blue)}.catalog-note{display:flex;gap:7px;margin:16px 3px 2px;color:#697287;font-size:10px;line-height:1.6}.catalog-note svg{width:15px;flex:0 0 auto;color:var(--blue)}.activity-footnote{display:flex;gap:22px;margin-top:22px;padding:18px 22px;color:#697287;border-top:1px solid var(--line);font-size:12px}.activity-footnote span{flex:0 0 98px;color:#17213a;font-weight:700}.activity-footnote p{margin:0;line-height:1.7}.result-dialog{position:fixed;inset:0;z-index:10;display:grid;place-items:center;background:rgb(16 31 56 / 45%);padding:24px}.result-dialog section{position:relative;width:390px;padding:34px;text-align:center;background:var(--paper);border-radius:16px;box-shadow:0 22px 52px rgb(9 29 64 / 28%)}.dialog-close{position:absolute;top:12px;right:16px;color:#697287;background:transparent;border:0;font-size:26px;line-height:1}.result-mark{display:grid;width:55px;height:55px;margin:0 auto 16px;place-items:center;color:var(--navy);background:var(--yellow);border-radius:15px 15px 5px 15px}.result-mark svg{width:27px}.result-dialog p{margin:0;color:var(--muted);font-size:12px}.result-dialog h2{margin:7px 0 4px;font-size:25px}.result-kind{color:var(--blue);font-size:12px;font-weight:700}.result-note{display:flex;gap:8px;margin:20px 0;padding:11px;text-align:left;color:#536079;background:#e9eef5;border-radius:9px;font-size:12px;line-height:1.65}.result-note svg{width:17px;flex:0 0 auto;color:var(--blue)}.dialog-confirm{width:100%;color:#fff;background:var(--blue)}@keyframes spin{to{transform:rotate(360deg)}}@media(prefers-reduced-motion:reduce){.prize-cell,.draw-action button,.activity-card,.dialog-confirm{transition:none}.button-loader{animation:none}}
</style>
