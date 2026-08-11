<script setup lang="ts">
import axios from 'axios';
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { http } from '../api/http';
import type { ApiError, UserResponse } from '../api/types';

type AuthMode = 'login' | 'register';

const router = useRouter();
const mode = ref<AuthMode>('login');
const showPassword = ref(false);
const loading = ref(false);
const submitError = ref('');
const form = reactive({ identifier: '', username: '', phone: '', nickname: '', password: '', confirmPassword: '' });
const errors = reactive({ identifier: '', username: '', phone: '', nickname: '', password: '', confirmPassword: '' });

const isRegister = computed(() => mode.value === 'register');
const title = computed(() => (isRegister.value ? '注册账户' : '账户登录'));
const subtitle = computed(() => (isRegister.value ? '填写资料，创建偶得账户' : '请输入用户名或手机号与密码'));

function clearFeedback() {
  errors.identifier = '';
  errors.username = '';
  errors.phone = '';
  errors.nickname = '';
  errors.password = '';
  errors.confirmPassword = '';
  submitError.value = '';
}

function switchMode(nextMode: AuthMode) {
  mode.value = nextMode;
  form.confirmPassword = '';
  showPassword.value = false;
  clearFeedback();
}

function validate() {
  clearFeedback();
  if (isRegister.value) {
    if (!form.username) errors.username = '请输入用户名';
    else if (!/^[A-Za-z0-9_]{3,32}$/.test(form.username)) errors.username = '用户名为 3–32 位字母、数字或下划线';
    if (!form.phone) errors.phone = '请输入手机号';
    else if (!/^1\d{10}$/.test(form.phone)) errors.phone = '请输入 11 位中国大陆手机号';
    if (!form.nickname) errors.nickname = '请输入昵称';
    else if (form.nickname.length > 50) errors.nickname = '昵称不能超过 50 个字符';
  } else if (!form.identifier) {
    errors.identifier = '请输入用户名或手机号';
  }

  if (!form.password) errors.password = '请输入密码';
  else if (form.password.length < 6) errors.password = '密码至少需要 6 位';

  if (isRegister.value) {
    if (!form.confirmPassword) errors.confirmPassword = '请再次输入密码';
    else if (form.confirmPassword !== form.password) errors.confirmPassword = '两次输入的密码不一致';
  }

  return !errors.identifier && !errors.username && !errors.phone && !errors.nickname && !errors.password && !errors.confirmPassword;
}

function getRequestError(error: unknown) {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as ApiError | undefined;
    return data?.message || '服务暂时不可用，请稍后再试';
  }
  return '服务暂时不可用，请稍后再试';
}

async function submit() {
  if (!validate() || loading.value) return;

  loading.value = true;
  try {
    if (isRegister.value) {
      await http.post('/auth/register', { username: form.username, phone: form.phone, nickname: form.nickname, password: form.password });
      switchMode('login');
      submitError.value = '注册成功，请使用新账户登录';
      return;
    }

    const response = await http.post<UserResponse>('/auth/login', { identifier: form.identifier, password: form.password });
    const user = response.data;
    if (!user?.id) throw new Error('登录响应缺少用户标识');
    sessionStorage.setItem('currentUserId', String(user.id));
    await router.push('/profile');
  } catch (error) {
    submitError.value = getRequestError(error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <!--
  THESIS: 登录是进入偶得权益服务的清晰入口，拒绝悬浮在空背景中央的通用登录卡片。
  OWN-WORLD: 420px 深蓝服务栏、暖黄识别块、纸白操作区、单层表单与克制分隔线。
  STORY: 用户先理解偶得能带来的持续收获，再输入手机号和密码进入个人中心，或切换注册。
  FIRST VIEWPORT: 左侧品牌承诺固定两行并列出三项权益；右侧表单与注册说明同屏出现。
  FORM: Operate 模式，入口服务厅结构，surface seed ae3d6760，approved comp login-final.png。
  FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, and DESIGN.md
  -->
  <div class="login-page">
    <svg class="login-defs" aria-hidden="true">
      <symbol id="login-phone" viewBox="0 0 24 24"><path d="M7 4 5 6c0 7.2 5.8 13 13 13l2-2-4-3-2 2c-2.5-.8-5.2-3.5-6-6l2-2-3-4Z" /></symbol>
      <symbol id="login-lock" viewBox="0 0 24 24"><rect x="5" y="10" width="14" height="10" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v2" /></symbol>
      <symbol id="login-eye" viewBox="0 0 24 24"><path d="M3 12s3-5 9-5 9 5 9 5-3 5-9 5-9-5-9-5Z" /><circle cx="12" cy="12" r="2" /></symbol>
      <symbol id="login-eye-off" viewBox="0 0 24 24"><path d="m4 4 16 16M10.6 7.2A9.8 9.8 0 0 1 12 7c6 0 9 5 9 5a14 14 0 0 1-2.2 2.7M6.2 6.3C4.1 7.8 3 10 3 12c0 0 3 5 9 5 1 0 1.9-.1 2.7-.4M10.5 10.5a2.1 2.1 0 0 0 3 3" /></symbol>
      <symbol id="login-arrow" viewBox="0 0 24 24"><path d="M5 12h14M14 7l5 5-5 5" /></symbol>
      <symbol id="login-check" viewBox="0 0 24 24"><path d="m5 12 4 4L19 6" /></symbol>
    </svg>

    <aside class="brand-rail">
      <div class="login-brand"><span class="login-mark">得</span><strong>偶得</strong></div>
      <div class="brand-message">
        <h1>每一次偶遇，<br />都有新的收获。</h1>
        <p>登录偶得，查看你的积分、活动与专属奖励<br />把每一次参与，都沉淀成值得期待的下一次</p>
      </div>
      <ul class="benefit-list">
        <li><svg><use href="#login-check" /></svg><span>积分明细随时可查</span></li>
        <li><svg><use href="#login-check" /></svg><span>专属活动与奖励提醒</span></li>
        <li><svg><use href="#login-check" /></svg><span>账户权益安全完整</span></li>
      </ul>
      <small>偶得账户服务 · 演示界面</small>
    </aside>

    <main class="login-hall">
      <section class="auth-counter" aria-labelledby="auth-title">
        <header class="auth-heading">
          <div><h2 id="auth-title">{{ title }}</h2><p>{{ subtitle }}</p></div>
          <span class="security-state"><i></i>安全连接正常</span>
        </header>

        <div class="auth-layout">
          <form class="auth-form" novalidate @submit.prevent="submit">
            <div v-if="!isRegister" class="form-field">
              <label for="identifier">用户名或手机号</label>
              <div class="field-control" :class="{ invalid: errors.identifier }">
                <svg><use href="#login-phone" /></svg>
                <input id="identifier" v-model.trim="form.identifier" autocomplete="username" maxlength="32" placeholder="请输入用户名或手机号" :aria-invalid="Boolean(errors.identifier)" aria-describedby="identifier-error" />
              </div>
              <p v-if="errors.identifier" id="identifier-error" class="field-error">{{ errors.identifier }}</p>
            </div>

            <template v-else>
              <div class="form-field"><label for="username">用户名</label><div class="field-control" :class="{ invalid: errors.username }"><svg><use href="#login-phone" /></svg><input id="username" v-model.trim="form.username" autocomplete="username" maxlength="32" placeholder="3–32 位字母、数字或下划线" :aria-invalid="Boolean(errors.username)" /></div><p v-if="errors.username" class="field-error">{{ errors.username }}</p></div>
              <div class="form-field"><label for="phone">手机号</label><div class="field-control" :class="{ invalid: errors.phone }"><svg><use href="#login-phone" /></svg><input id="phone" v-model.trim="form.phone" inputmode="numeric" autocomplete="tel" maxlength="11" placeholder="请输入手机号" :aria-invalid="Boolean(errors.phone)" /></div><p v-if="errors.phone" class="field-error">{{ errors.phone }}</p></div>
              <div class="form-field"><label for="nickname">昵称</label><div class="field-control" :class="{ invalid: errors.nickname }"><svg><use href="#login-phone" /></svg><input id="nickname" v-model.trim="form.nickname" autocomplete="nickname" maxlength="50" placeholder="请输入昵称" :aria-invalid="Boolean(errors.nickname)" /></div><p v-if="errors.nickname" class="field-error">{{ errors.nickname }}</p></div>
            </template>

            <div class="form-field">
              <label for="password">密码</label>
              <div class="field-control" :class="{ invalid: errors.password }">
                <svg><use href="#login-lock" /></svg>
                <input id="password" v-model="form.password" :type="showPassword ? 'text' : 'password'" :autocomplete="isRegister ? 'new-password' : 'current-password'" placeholder="请输入密码" :aria-invalid="Boolean(errors.password)" aria-describedby="password-error" />
                <button type="button" class="visibility-button" :aria-label="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword"><svg><use :href="showPassword ? '#login-eye-off' : '#login-eye'" /></svg></button>
              </div>
              <p v-if="errors.password" id="password-error" class="field-error">{{ errors.password }}</p>
            </div>

            <div v-if="isRegister" class="form-field">
              <label for="confirm-password">确认密码</label>
              <div class="field-control" :class="{ invalid: errors.confirmPassword }">
                <svg><use href="#login-lock" /></svg>
                <input id="confirm-password" v-model="form.confirmPassword" :type="showPassword ? 'text' : 'password'" autocomplete="new-password" placeholder="请再次输入密码" :aria-invalid="Boolean(errors.confirmPassword)" aria-describedby="confirm-error" />
              </div>
              <p v-if="errors.confirmPassword" id="confirm-error" class="field-error">{{ errors.confirmPassword }}</p>
            </div>

            <p v-if="submitError" class="form-status" :class="{ success: isRegister && submitError.startsWith('注册成功') }" role="status">{{ submitError }}</p>
            <button class="auth-submit" type="submit" :disabled="loading">
              <span v-if="loading" class="spinner" aria-hidden="true"></span>
              <span>{{ loading ? '正在处理…' : isRegister ? '创建偶得账户' : '登录偶得' }}</span>
              <svg v-if="!loading"><use href="#login-arrow" /></svg>
            </button>
          </form>

          <aside class="mode-panel">
            <h3>{{ isRegister ? '已有账户？' : '还没有账户？' }}</h3>
            <p>{{ isRegister ? '返回登录，继续查看你的积分与专属奖励。' : '注册后即可建立会员身份，并开始使用积分服务。' }}</p>
            <button type="button" @click="isRegister ? switchMode('login') : router.push('/register')"><span>{{ isRegister ? '返回登录' : '注册账号' }}</span><svg><use href="#login-arrow" /></svg></button>
          </aside>
        </div>

        <footer class="login-footer"><span>隐私说明</span><span>服务帮助</span><em>© 偶得 · 演示界面</em></footer>
      </section>
    </main>
  </div>
</template>

<style scoped>
.login-page{--login-blue:#174fa7;--login-navy:#102f65;--login-yellow:#f1c84a;--login-paper:#f8f7f2;--login-ink:#17213a;--login-muted:#697287;--login-line:#d7dde6;display:flex;min-width:1180px;min-height:100vh;color:var(--login-ink);background:var(--login-paper)}
.login-page svg{width:19px;height:19px;fill:none;stroke:currentColor;stroke-width:1.8;stroke-linecap:round;stroke-linejoin:round}.login-defs{position:absolute;width:0!important;height:0!important;overflow:hidden}
.brand-rail{display:flex;width:420px;min-height:100vh;flex:0 0 420px;padding:40px 54px 36px;flex-direction:column;color:white;background:var(--login-navy)}.login-brand{display:flex;align-items:center;gap:12px;font-size:19px}.login-mark{display:grid;width:38px;height:38px;place-items:center;color:var(--login-navy);background:var(--login-yellow);border-radius:10px 10px 4px 10px;font-size:18px;font-weight:800}
.brand-message{margin-top:86px}.brand-message h1{margin:0 0 18px;font-size:40px;line-height:1.3;letter-spacing:-.03em}.brand-message p{margin:0;color:#bdcce1;font-size:14px;line-height:1.85}.benefit-list{display:grid;gap:16px;margin:30px 0 0;padding:0;list-style:none}.benefit-list li{display:grid;grid-template-columns:34px 1fr;gap:12px;align-items:center;font-size:13px;font-weight:700}.benefit-list svg{width:30px;height:30px;padding:7px;color:var(--login-navy);background:var(--login-yellow);border-radius:8px 8px 3px 8px}.brand-rail small{margin-top:auto;color:#8fa5c4;font-size:11px;letter-spacing:.04em}
.login-hall{display:flex;min-width:0;flex:1;align-items:center;justify-content:center;padding:70px}.auth-counter{width:min(760px,100%);animation:counter-arrive .55s cubic-bezier(.22,1,.36,1) both}.auth-heading{display:flex;align-items:flex-end;justify-content:space-between;margin-bottom:28px;padding-bottom:22px;border-bottom:1px solid var(--login-line)}.auth-heading h2{margin:0;font-size:34px;letter-spacing:-.025em}.auth-heading p{margin:6px 0 0;color:var(--login-muted);font-size:13px}.security-state{display:flex;align-items:center;gap:7px;padding:7px 10px;color:#315e3f;background:#e2efe4;border-radius:6px;font-size:11px;font-weight:700}.security-state i{width:7px;height:7px;background:#4da572;border-radius:50%}
.auth-layout{display:grid;grid-template-columns:minmax(0,1fr) 230px;gap:40px;align-items:start}.auth-form{display:grid;gap:18px}.form-field{display:grid;gap:8px}.form-field label{font-size:13px;font-weight:600}.field-control{display:flex;height:50px;align-items:center;gap:11px;padding:0 14px;color:#718097;background:white;border:1px solid #cfd6e1;border-radius:9px;transition:border-color .18s ease,box-shadow .18s ease}.field-control:focus-within{border-color:var(--login-blue);box-shadow:0 0 0 3px rgb(23 79 167 / 14%)}.field-control.invalid{border-color:#b64a55;box-shadow:0 0 0 3px rgb(182 74 85 / 10%)}.field-control input{width:100%;min-width:0;border:0;outline:0;color:var(--login-ink);background:transparent;font-size:14px;caret-color:var(--login-blue)}.field-control input::placeholder{color:#7a8497;opacity:1}.visibility-button{display:grid;padding:4px;place-items:center;color:#657087;background:transparent;border:0;border-radius:5px}.visibility-button:hover{color:var(--login-blue)}.visibility-button:focus-visible,.mode-panel button:focus-visible,.auth-submit:focus-visible{outline:3px solid rgb(23 79 167 / 24%);outline-offset:3px}.field-error{margin:0;color:#a23843;font-size:11px}
.auth-submit{display:flex;width:100%;height:50px;align-items:center;justify-content:center;gap:9px;margin-top:7px;color:white;background:var(--login-blue);border:0;border-radius:9px;box-shadow:0 9px 20px rgb(23 79 167 / 22%);font-size:14px;font-weight:700;transition:transform .18s ease,box-shadow .18s ease}.auth-submit:hover:not(:disabled){transform:translateY(-1px);box-shadow:0 12px 25px rgb(23 79 167 / 27%)}.auth-submit:disabled{cursor:wait;opacity:.72}.auth-submit svg{width:17px}.spinner{width:16px;height:16px;border:2px solid rgb(255 255 255 / 35%);border-top-color:white;border-radius:50%;animation:spin .7s linear infinite}.form-status{margin:0;padding:10px 12px;color:#8d303a;background:#f8e7e9;border-radius:6px;font-size:12px;line-height:1.6}.form-status.success{color:#315e3f;background:#e2efe4}
.mode-panel{min-height:248px;padding:25px;background:#e9eef5;border-radius:12px}.mode-panel h3{margin:0 0 9px;font-size:16px}.mode-panel p{margin:0;color:var(--login-muted);font-size:12px;line-height:1.75}.mode-panel button{display:flex;align-items:center;gap:7px;margin-top:25px;padding:0;color:var(--login-blue);background:transparent;border:0;font-size:12px;font-weight:700}.mode-panel button svg{width:16px;transition:transform .18s ease}.mode-panel button:hover svg{transform:translateX(3px)}.login-footer{display:flex;gap:22px;margin-top:42px;padding-top:20px;color:#7c8597;border-top:1px solid var(--login-line);font-size:11px}.login-footer em{margin-left:auto;font-style:normal}
@keyframes counter-arrive{from{opacity:.65;transform:translateY(12px);filter:blur(4px)}to{opacity:1;transform:none;filter:none}}@keyframes spin{to{transform:rotate(360deg)}}@media(prefers-reduced-motion:reduce){.auth-counter,.spinner{animation:none}.auth-submit,.mode-panel button svg{transition:none}}
</style>
