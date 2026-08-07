<script setup lang="ts">
import axios from 'axios';
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { http } from '../api/http';
import type { ApiError, PointBalanceResponse, UserResponse } from '../api/types';

const router = useRouter();
const profile = ref<UserResponse | null>(null);
const pointBalance = ref(0);
const loading = ref(true);
const error = ref('');
const loggingOut = ref(false);
const editing = ref(false);
const saving = ref(false);
const formError = ref('');
const saveFeedback = ref('');
const draft = ref({ nickname: '', phone: '' });
let feedbackTimer: ReturnType<typeof setTimeout> | undefined;

const nickname = computed(() => profile.value?.nickname || '未设置昵称');
const phone = computed(() => profile.value?.phone || '未绑定手机号');
const memberLevel = computed(() => '普通会员');
const accountStatus = computed(() => '账户状态正常');
const avatarUrl = computed(() => '');
const avatarText = computed(() => nickname.value.slice(0, 1).toUpperCase());
const formattedPoints = computed(() => new Intl.NumberFormat('zh-CN').format(pointBalance.value));

function getErrorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as ApiError | undefined;
    return data?.message || '暂时无法获取账户信息，请稍后重试';
  }
  return '暂时无法获取账户信息，请稍后重试';
}

async function loadProfile() {
  loading.value = true;
  error.value = '';
  try {
    const userId = sessionStorage.getItem('currentUserId');
    if (!userId) {
      await router.replace('/login');
      return;
    }
    const [profileResponse, balanceResponse] = await Promise.all([
      http.get<UserResponse>(`/users/${userId}`),
      http.get<PointBalanceResponse>(`/points/users/${userId}/balance`),
    ]);
    profile.value = profileResponse.data;
    pointBalance.value = balanceResponse.data.balance;
  } catch (requestError) {
    if (axios.isAxiosError(requestError) && requestError.response?.status === 401) {
      await router.replace('/login');
      return;
    }
    error.value = getErrorMessage(requestError);
  } finally {
    loading.value = false;
  }
}

function startEditing() {
  if (!profile.value) return;
  draft.value = { nickname: profile.value.nickname, phone: profile.value.phone };
  formError.value = '';
  clearFeedback();
  editing.value = true;
}

function clearFeedback() {
  if (feedbackTimer) clearTimeout(feedbackTimer);
  feedbackTimer = undefined;
  saveFeedback.value = '';
}

function showSaveFeedback() {
  clearFeedback();
  saveFeedback.value = '资料已更新';
  feedbackTimer = setTimeout(clearFeedback, 3000);
}

function cancelEditing() {
  editing.value = false;
  formError.value = '';
}

async function saveProfile() {
  const userId = sessionStorage.getItem('currentUserId');
  const nextNickname = draft.value.nickname.trim();
  const nextPhone = draft.value.phone.trim();
  if (!userId) {
    await router.replace('/login');
    return;
  }
  if (!nextNickname || nextNickname.length > 15) {
    formError.value = '昵称需填写，且不能超过 15 个字。';
    return;
  }
  if (!/^1\d{10}$/.test(nextPhone)) {
    formError.value = '请输入 11 位中国大陆手机号。';
    return;
  }

  saving.value = true;
  formError.value = '';
  try {
    const response = await http.put<UserResponse>(`/users/${userId}`, { nickname: nextNickname, phone: nextPhone });
    profile.value = response.data;
    editing.value = false;
    showSaveFeedback();
  } catch (requestError) {
    formError.value = getErrorMessage(requestError);
  } finally {
    saving.value = false;
  }
}

async function logout() {
  if (loggingOut.value) return;
  loggingOut.value = true;
  sessionStorage.removeItem('currentUserId');
  loggingOut.value = false;
  await router.replace('/login');
}

onMounted(loadProfile);
onUnmounted(clearFeedback);
</script>

<template>
  <!--
  THESIS: 账户档案将身份、积分和状态置于同一条清晰服务记录中，拒绝泛化的卡片仪表盘。
  OWN-WORLD: 深海军蓝顶部导航、钴蓝身份摘要、纸白单层资料面板、暖黄权益标识与细分隔线。
  STORY: 登录用户先确认身份、积分与账户状态，再按需进入资料编辑或退出当前账户。
  FIRST VIEWPORT: 顶部横向导航固定品牌与当前页；首屏先呈现身份和积分摘要，资料与操作区紧随其后。
  FORM: Operate 模式，会员服务档案，approved comp user-profile-a.png。
  FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, and DESIGN.md
  -->
  <div class="profile-page">
    <svg class="profile-defs" aria-hidden="true">
      <symbol id="profile-user" viewBox="0 0 24 24"><circle cx="12" cy="8" r="3.5"/><path d="M5 20c.5-4 3-6 7-6s6.5 2 7 6"/></symbol>
      <symbol id="profile-points" viewBox="0 0 24 24"><circle cx="12" cy="12" r="8"/><path d="M9 9.5h4.2a2 2 0 0 1 0 4H10.8a2 2 0 0 0 0 4H15M12 7v2.5M12 17.5V20"/></symbol>
      <symbol id="profile-gift" viewBox="0 0 24 24"><path d="M4 10h16v10H4zM3 7h18v3H3zM12 7v13M7.5 7C5 7 5 3.5 7.5 3.5c2 0 4.5 3.5 4.5 3.5s2.5-3.5 4.5-3.5C19 3.5 19 7 16.5 7"/></symbol>
      <symbol id="profile-bag" viewBox="0 0 24 24"><path d="M5 8h14l-1 12H6L5 8Z"/><path d="M9 9V6a3 3 0 0 1 6 0v3"/></symbol>
      <symbol id="profile-phone" viewBox="0 0 24 24"><path d="M7 4 5 6c0 7.2 5.8 13 13 13l2-2-4-3-2 2c-2.5-.8-5.2-3.5-6-6l2-2-3-4Z"/></symbol>
      <symbol id="profile-edit" viewBox="0 0 24 24"><path d="m5 16-1 4 4-1L19 8l-3-3L5 16Z"/><path d="m14.5 6.5 3 3"/></symbol>
      <symbol id="profile-logout" viewBox="0 0 24 24"><path d="M10 5H5v14h5M14 8l4 4-4 4M8 12h10"/></symbol>
    </svg>

    <header class="top-nav">
      <div class="brand"><span class="brand-mark">得</span><span>偶得</span></div>
      <nav aria-label="主导航">
        <RouterLink to="/profile" class="active"><svg><use href="#profile-user" /></svg><span>用户信息</span></RouterLink>
        <RouterLink to="/points"><svg><use href="#profile-points" /></svg><span>积分明细</span></RouterLink>
        <a href="#" @click.prevent><svg><use href="#profile-gift" /></svg><span>幸运抽奖</span></a>
        <a href="#" @click.prevent><svg><use href="#profile-bag" /></svg><span>兑换商城</span></a>
      </nav>
      <div class="top-support"><span>服务中心</span><strong>400 · 888 · 2608</strong></div>
    </header>

    <main class="workspace" aria-live="polite">
      <div v-if="loading" class="state-panel"><span class="loader" aria-hidden="true"></span><strong>正在加载账户信息</strong><p>请稍候，正在同步你的账户状态与积分</p></div>
      <section v-else-if="error" class="state-panel error-state"><strong>账户信息暂不可用</strong><p>{{ error }}</p><button type="button" @click="loadProfile">重新加载</button></section>
      <section v-else-if="!profile" class="state-panel"><strong>暂未找到账户信息</strong><p>当前账户没有可展示的资料，请稍后重试。</p><button type="button" @click="loadProfile">重新加载</button></section>

      <template v-else>
        <div class="sample-note">账户中心 · 信息实时同步</div>
        <section class="identity-panel" aria-labelledby="profile-name">
          <div class="avatar" :aria-label="`${nickname} 的头像`"><img v-if="avatarUrl" :src="avatarUrl" alt="" /><span v-else>{{ avatarText }}</span></div>
          <div class="identity-copy"><p>欢迎回来</p><h1 id="profile-name">{{ nickname }}</h1><div class="member-line"><span>{{ memberLevel }}</span><span>{{ accountStatus }}</span></div></div>
          <div class="points-block"><span>当前可用积分</span><strong>{{ formattedPoints }}</strong><RouterLink to="/points">查看积分明细</RouterLink></div>
        </section>

        <div class="content-grid">
          <section class="details-panel" :class="{ editing }" aria-labelledby="details-title">
            <header class="section-heading"><div><h2 id="details-title">{{ editing ? '编辑个人资料' : '个人资料' }}</h2><p>{{ editing ? '请核对后保存，手机号将用于账户通知。' : '用于账户识别与权益通知' }}</p></div><span>{{ editing ? '编辑中' : '账户信息完整' }}</span></header>
            <Transition name="feedback"><p v-if="saveFeedback" class="save-feedback" role="status">{{ saveFeedback }}</p></Transition>
            <form v-if="editing" class="profile-form" @submit.prevent="saveProfile">
              <label class="profile-field"><span><svg><use href="#profile-user" /></svg>昵称</span><input v-model="draft.nickname" maxlength="15" autocomplete="nickname" /><small>{{ draft.nickname.length }}/15</small></label>
              <label class="profile-field"><span><svg><use href="#profile-phone" /></svg>手机号</span><input v-model.trim="draft.phone" inputmode="numeric" autocomplete="tel" maxlength="11" /></label>
              <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>
              <div class="form-actions"><button class="text-button" type="button" :disabled="saving" @click="cancelEditing">取消</button><button class="save-button" type="submit" :disabled="saving">{{ saving ? '正在保存…' : '保存修改' }}</button></div>
            </form>
            <dl v-else class="details-list">
              <div><dt><svg><use href="#profile-user" /></svg>昵称</dt><dd>{{ nickname }}</dd></div>
              <div><dt><svg><use href="#profile-phone" /></svg>手机号</dt><dd>{{ phone }}</dd></div>
              <div><dt><svg><use href="#profile-points" /></svg>会员等级</dt><dd><span class="level-tag">{{ memberLevel }}</span></dd></div>
              <div><dt><svg><use href="#profile-user" /></svg>账户状态</dt><dd><span class="status-tag">{{ accountStatus }}</span></dd></div>
            </dl>
          </section>

          <aside class="action-panel"><div class="action-copy"><span>账户管理</span><h2>{{ editing ? '正在编辑资料' : '资料有变化？' }}</h2><p>{{ editing ? '保存后将立即回到资料展示状态。' : '及时更新个人资料，确保账户权益信息准确。' }}</p></div><button class="primary" type="button" :disabled="editing" @click="startEditing"><svg><use href="#profile-edit" /></svg>{{ editing ? '请在左侧保存' : '修改个人资料' }}</button><button class="secondary" type="button" :disabled="loggingOut || saving" @click="logout"><svg><use href="#profile-logout" /></svg>{{ loggingOut ? '正在退出…' : '退出当前账户' }}</button></aside>
        </div>
        <footer><span>积分规则</span><span>兑换帮助</span><span>隐私说明</span><em>© 偶得 · 账户中心</em></footer>
      </template>
    </main>
  </div>
</template>

<style scoped>
.profile-page{--blue:#174fa7;--navy:#102f65;--ink:#17213a;--paper:#f8f7f2;--line:#d7dde6;--yellow:#f1c84a;min-width:1180px;min-height:100vh;background:#e8edf3}.profile-page svg{width:20px;height:20px;fill:none;stroke:currentColor;stroke-width:1.7;stroke-linecap:round;stroke-linejoin:round}.profile-defs{position:absolute;width:0!important;height:0!important;overflow:hidden}.top-nav{display:flex;height:74px;align-items:center;padding:0 48px;color:#eef4ff;background:var(--navy);box-shadow:0 2px 8px rgb(16 47 101 / 14%)}.brand{display:flex;align-items:center;gap:12px;flex:0 0 180px;font-size:18px;font-weight:700}.brand-mark{display:grid;width:34px;height:34px;place-items:center;color:var(--navy);background:var(--yellow);border-radius:9px 9px 4px 9px;font-size:17px;font-weight:800}.top-nav nav{display:flex;align-items:center;gap:6px;height:100%}.top-nav nav a{display:flex;align-items:center;gap:10px;height:42px;padding:0 15px;color:#afc0dc;border-radius:10px;font-size:14px;transition:.18s ease}.top-nav nav a:hover,.top-nav nav a:focus-visible{color:#fff;background:#183f7b;outline:none}.top-nav nav a.active{color:var(--navy);background:var(--yellow);font-weight:700}.top-nav nav a svg{width:18px}.top-support{display:flex;align-items:baseline;gap:10px;margin-left:auto;color:#9eb1ce;font-size:12px}.top-support strong{color:#f4f7fd;font-size:14px;font-variant-numeric:tabular-nums}.workspace{max-width:1340px;min-height:calc(100vh - 74px);margin:0 auto;padding:34px 48px 28px}.sample-note{color:#667087;font-size:12px;letter-spacing:.04em}.identity-panel{display:grid;grid-template-columns:auto 1fr 330px;align-items:center;min-height:206px;margin-top:16px;padding:34px 40px;color:#fff;background:var(--blue);border-radius:16px;box-shadow:0 18px 40px rgb(31 54 94 / 16%)}.avatar{display:grid;width:96px;height:96px;margin-right:26px;overflow:hidden;place-items:center;color:var(--navy);background:var(--yellow);border:5px solid rgb(255 255 255 / 18%);border-radius:26px 26px 10px 26px;font-size:42px;font-weight:800}.avatar img{width:100%;height:100%;object-fit:cover}.identity-copy p{margin:0 0 3px;color:#c9d9f3;font-size:13px}.identity-copy h1{margin:0 0 13px;font-size:34px;line-height:1.25;letter-spacing:-.02em}.member-line{display:flex;gap:10px}.member-line span{padding:5px 10px;color:#e8effb;background:rgb(7 39 91 / 32%);border-radius:6px;font-size:12px}.member-line span:first-child{color:var(--navy);background:var(--yellow);font-weight:700}.points-block{align-self:stretch;display:grid;align-content:center;padding-left:38px;border-left:1px solid rgb(255 255 255 / 22%)}.points-block>span{color:#c9d9f3;font-size:13px}.points-block strong{margin:1px 0 8px;font-size:45px;line-height:1.1;letter-spacing:-.03em;font-variant-numeric:tabular-nums}.points-block a{width:max-content;color:#fff;font-size:13px;font-weight:600;text-decoration:underline;text-decoration-color:rgb(255 255 255 / 45%);text-underline-offset:4px}.content-grid{display:grid;grid-template-columns:minmax(0,1.7fr) minmax(320px,.8fr);gap:22px;margin-top:22px}.details-panel,.action-panel{background:var(--paper);border-radius:16px;box-shadow:0 10px 28px rgb(38 50 75 / 9%)}.details-panel{padding:28px 32px 16px}.section-heading{display:flex;align-items:flex-start;justify-content:space-between;padding-bottom:20px;border-bottom:1px solid var(--line)}.section-heading h2,.action-copy h2{margin:0 0 5px;font-size:21px;letter-spacing:-.015em}.section-heading p,.action-copy p{margin:0;color:#697287;font-size:13px;line-height:1.7}.section-heading>span,.status-tag{padding:5px 9px;color:#315e3f;background:#e2efe4;border-radius:6px;font-size:11px;font-weight:600}.details-list{margin:0}.details-list>div{display:grid;grid-template-columns:190px 1fr;align-items:center;min-height:68px;border-bottom:1px solid #e4e8ee}.details-list>div:last-child{border-bottom:0}.details-list dt{display:flex;align-items:center;gap:10px;color:#697287;font-size:13px}.details-list dt svg{width:17px;color:var(--blue)}.details-list dd{margin:0;color:#17213a;font-size:14px;font-weight:600}.level-tag{display:inline-flex;padding:5px 9px;color:var(--navy);background:#f8e9a8;border-radius:6px;font-size:12px}.action-panel{display:flex;flex-direction:column;padding:28px}.action-copy>span{display:block;margin-bottom:10px;color:var(--blue);font-size:12px;font-weight:700}.action-copy{margin-bottom:22px}.action-panel button{display:flex;min-height:46px;align-items:center;justify-content:center;gap:9px;border:0;border-radius:9px;font-size:13px;font-weight:700;transition:transform .18s ease,box-shadow .18s ease}.action-panel button:hover:not(:disabled){transform:translateY(-1px)}.action-panel button:focus-visible,.state-panel button:focus-visible{outline:3px solid rgb(23 79 167 / 28%);outline-offset:3px}.action-panel button:disabled{opacity:.72;cursor:wait}.action-panel button svg{width:17px}.primary{color:#fff;background:var(--blue);box-shadow:0 8px 18px rgb(23 79 167 / 22%)}.secondary{margin-top:10px;color:#536079;background:#e9edf2}footer{display:flex;gap:24px;align-items:center;padding:24px 4px 0;color:#747d90;font-size:11px}footer em{margin-left:auto;font-style:normal}.state-panel{display:grid;min-height:360px;place-content:center;justify-items:center;gap:10px;color:#17213a;text-align:center}.state-panel strong{font-size:21px}.state-panel p{max-width:360px;margin:0;color:#697287;font-size:14px;line-height:1.7}.state-panel button{margin-top:8px;padding:11px 18px;color:#fff;background:var(--blue);border:0;border-radius:9px;font-size:13px;font-weight:700}.error-state strong{color:#a23843}.loader{width:28px;height:28px;border:3px solid #c7d5ed;border-top-color:var(--blue);border-radius:50%;animation:spin .7s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}@media(prefers-reduced-motion:reduce){.loader{animation:none}.top-nav nav a,.action-panel button{transition:none}}
.details-panel.editing{padding-bottom:28px}.profile-form{display:grid;gap:18px;padding-top:20px}.profile-field{display:grid;grid-template-columns:190px minmax(0,1fr) auto;gap:12px;align-items:center;min-height:60px}.profile-field>span{display:flex;align-items:center;gap:10px;color:#697287;font-size:13px}.profile-field span svg{width:17px;color:var(--blue)}.profile-field input{height:46px;min-width:0;padding:0 13px;color:var(--ink);background:#fff;border:1px solid #cfd6e1;border-radius:9px;font:inherit;font-size:14px;font-weight:600;caret-color:var(--blue);transition:border-color .18s ease,box-shadow .18s ease}.profile-field input:focus{border-color:var(--blue);box-shadow:0 0 0 3px rgb(23 79 167 / 14%);outline:0}.profile-field small{min-width:36px;color:#697287;font-size:11px;text-align:right}.form-error{margin:0;padding:9px 12px;color:#a23843;background:#f8e7e9;border-radius:6px;font-size:12px}.form-actions{display:flex;justify-content:flex-end;gap:10px;padding-top:4px}.profile-form button{min-height:42px;padding:0 18px;border:0;border-radius:9px;font-size:13px;font-weight:700;transition:.18s ease}.text-button{color:#536079;background:#e9edf2}.save-button{min-width:110px;color:#fff;background:var(--blue);box-shadow:0 8px 18px rgb(23 79 167 / 22%)}.save-button:hover:not(:disabled){transform:translateY(-1px)}.profile-form button:focus-visible,.profile-field input:focus-visible{outline:3px solid rgb(23 79 167 / 28%);outline-offset:3px}.profile-form button:disabled{opacity:.72;cursor:wait}.save-feedback{margin:14px 0 -4px;padding:8px 10px;color:#315e3f;background:#e2efe4;border-radius:6px;font-size:12px;font-weight:600}.feedback-enter-active,.feedback-leave-active{transition:opacity .28s ease,transform .28s ease}.feedback-enter-from,.feedback-leave-to{opacity:0;transform:translateY(-4px)}@media(prefers-reduced-motion:reduce){.profile-field input,.profile-form button,.feedback-enter-active,.feedback-leave-active{transition:none}}
</style>
