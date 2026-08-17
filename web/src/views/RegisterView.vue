<script setup lang="ts">
import axios from 'axios';
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { http } from '../api/http';
import type { ApiError } from '../api/types';

const router = useRouter();
const showPassword = ref(false);
const loading = ref(false);
const submitError = ref('');
const form = reactive({ username: '', phone: '', nickname: '', password: '', confirmPassword: '' });
const errors = reactive({ username: '', phone: '', nickname: '', password: '', confirmPassword: '' });

function clearFeedback() {
  errors.phone = '';
  errors.username = '';
  errors.nickname = '';
  errors.password = '';
  errors.confirmPassword = '';
  submitError.value = '';
}

function validate() {
  clearFeedback();
  if (!form.phone) errors.phone = '请输入手机号';
  else if (!/^1\d{10}$/.test(form.phone)) errors.phone = '请输入 11 位中国大陆手机号';
  if (!form.username) errors.username = '请输入用户名';
  else if (!/^[A-Za-z0-9_]{3,32}$/.test(form.username)) errors.username = '用户名为 3–32 位字母、数字或下划线';
  if (!form.nickname) errors.nickname = '请输入昵称';
  else if (form.nickname.length > 50) errors.nickname = '昵称不能超过 50 个字符';
  if (!form.password) errors.password = '请输入密码';
  else if (form.password.length < 6) errors.password = '密码至少需要 6 位';
  if (!form.confirmPassword) errors.confirmPassword = '请再次输入密码';
  else if (form.confirmPassword !== form.password) errors.confirmPassword = '两次输入的密码不一致';
  return !errors.username && !errors.phone && !errors.nickname && !errors.password && !errors.confirmPassword;
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
    await http.post('/auth/register', {
      username: form.username,
      phone: form.phone,
      nickname: form.nickname,
      password: form.password,
    });
    await router.push('/login');
  } catch (error) {
    submitError.value = getRequestError(error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <!--
  THESIS: 注册是服务厅中清晰的账户建立动作，拒绝堆叠多余信息字段和旁支入口。
  OWN-WORLD: 固定深蓝服务栏、暖黄品牌标记、纸白表单柜台与冷灰登录说明面板。
  STORY: 用户确认偶得权益后，以手机号和两次密码建立账户，完成后直接进入个人中心。
  FIRST VIEWPORT: 左侧品牌承诺与三项权益固定可见；右侧三字段注册表单、主提交按钮和登录切换同屏。
  FORM: Operate 模式，入口服务厅的注册分支，approved comp register-final.png。
  FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, and DESIGN.md
  -->
  <div class="register-page">
    <svg class="register-defs" aria-hidden="true">
      <symbol id="register-phone" viewBox="0 0 24 24"><path d="M7 4 5 6c0 7.2 5.8 13 13 13l2-2-4-3-2 2c-2.5-.8-5.2-3.5-6-6l2-2-3-4Z" /></symbol>
      <symbol id="register-lock" viewBox="0 0 24 24"><rect x="5" y="10" width="14" height="10" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v2" /></symbol>
      <symbol id="register-eye" viewBox="0 0 24 24"><path d="M3 12s3-5 9-5 9 5 9 5-3 5-9 5-9-5-9-5Z" /><circle cx="12" cy="12" r="2" /></symbol>
      <symbol id="register-eye-off" viewBox="0 0 24 24"><path d="m4 4 16 16M10.6 7.2A9.8 9.8 0 0 1 12 7c6 0 9 5 9 5a14 14 0 0 1-2.2 2.7M6.2 6.3C4.1 7.8 3 10 3 12c0 0 3 5 9 5 1 0 1.9-.1 2.7-.4M10.5 10.5a2.1 2.1 0 0 0 3 3" /></symbol>
      <symbol id="register-arrow" viewBox="0 0 24 24"><path d="M5 12h14M14 7l5 5-5 5" /></symbol>
      <symbol id="register-check" viewBox="0 0 24 24"><path d="m5 12 4 4L19 6" /></symbol>
    </svg>

    <aside class="brand-rail">
      <div class="brand"><span class="brand-mark">得</span><strong>偶得</strong></div>
      <div class="brand-message"><h1>每一次偶遇，<br />都有新的收获。</h1><p>登录偶得，查看你的积分、活动与专属奖励，把每<br />一次参与，都沉淀成值得期待的下一次。</p></div>
      <ul class="benefit-list">
        <li><svg><use href="#register-check" /></svg><span>积分明细随时可查</span></li>
        <li><svg><use href="#register-check" /></svg><span>专属活动与奖励提醒</span></li>
        <li><svg><use href="#register-check" /></svg><span>账户权益安全完整</span></li>
      </ul>
      <small>偶得账户服务 · 演示界面</small>
    </aside>

    <main class="register-hall">
      <section class="auth-counter" aria-labelledby="register-title">
        <header class="auth-heading"><div><h1 id="register-title">注册账户</h1><p>使用手机号创建偶得账户</p></div><span class="security-state"><i></i>安全连接正常</span></header>
        <div class="auth-layout">
          <form class="auth-form" novalidate @submit.prevent="submit">
            <div class="form-field"><label for="username">用户名</label><div class="field-control" :class="{ invalid: errors.username }"><svg><use href="#register-phone" /></svg><input id="username" v-model.trim="form.username" autocomplete="username" maxlength="32" placeholder="3–32 位字母、数字或下划线" :aria-invalid="Boolean(errors.username)" aria-describedby="username-error" /></div><p v-if="errors.username" id="username-error" class="field-error">{{ errors.username }}</p></div>
            <div class="form-field"><label for="phone">手机号</label><div class="field-control" :class="{ invalid: errors.phone }"><svg><use href="#register-phone" /></svg><input id="phone" v-model.trim="form.phone" inputmode="numeric" autocomplete="tel" maxlength="11" placeholder="请输入手机号" :aria-invalid="Boolean(errors.phone)" aria-describedby="phone-error" /></div><p v-if="errors.phone" id="phone-error" class="field-error">{{ errors.phone }}</p></div>
            <div class="form-field"><label for="nickname">昵称</label><div class="field-control" :class="{ invalid: errors.nickname }"><svg><use href="#register-phone" /></svg><input id="nickname" v-model.trim="form.nickname" autocomplete="nickname" maxlength="50" placeholder="请输入昵称" :aria-invalid="Boolean(errors.nickname)" aria-describedby="nickname-error" /></div><p v-if="errors.nickname" id="nickname-error" class="field-error">{{ errors.nickname }}</p></div>
            <div class="form-field"><label for="password">密码</label><div class="field-control" :class="{ invalid: errors.password }"><svg><use href="#register-lock" /></svg><input id="password" v-model="form.password" :type="showPassword ? 'text' : 'password'" autocomplete="new-password" placeholder="请输入密码" :aria-invalid="Boolean(errors.password)" aria-describedby="password-error" /><button type="button" class="visibility-button" :aria-label="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword"><svg><use :href="showPassword ? '#register-eye-off' : '#register-eye'" /></svg></button></div><p v-if="errors.password" id="password-error" class="field-error">{{ errors.password }}</p></div>
            <div class="form-field"><label for="confirm-password">确认密码</label><div class="field-control" :class="{ invalid: errors.confirmPassword }"><svg><use href="#register-lock" /></svg><input id="confirm-password" v-model="form.confirmPassword" :type="showPassword ? 'text' : 'password'" autocomplete="new-password" placeholder="请再次输入密码" :aria-invalid="Boolean(errors.confirmPassword)" aria-describedby="confirm-error" /></div><p v-if="errors.confirmPassword" id="confirm-error" class="field-error">{{ errors.confirmPassword }}</p></div>
            <p v-if="submitError" class="form-status" role="alert">{{ submitError }}</p>
            <button class="auth-submit" type="submit" :disabled="loading"><span v-if="loading" class="spinner" aria-hidden="true"></span><span>{{ loading ? '正在创建…' : '创建偶得账户' }}</span><svg v-if="!loading"><use href="#register-arrow" /></svg></button>
          </form>
          <aside class="mode-panel"><h2>已有账户？</h2><p>返回登录，继续查看你的积分与专属奖励。</p><RouterLink to="/login"><span>返回登录</span><svg><use href="#register-arrow" /></svg></RouterLink></aside>
        </div>
        <footer class="register-footer"><span>隐私说明</span><span>服务帮助</span><em>© 偶得 · 演示界面</em></footer>
      </section>
    </main>
  </div>
</template>

<style scoped>
.register-page{--blue:#174fa7;--navy:#102f65;--yellow:#f1c84a;--paper:#f8f7f2;--ink:#17213a;--muted:#697287;--line:#d7dde6;display:flex;min-width:1180px;min-height:100vh;color:var(--ink);background:var(--paper)}.register-page svg{width:19px;height:19px;fill:none;stroke:currentColor;stroke-width:1.8;stroke-linecap:round;stroke-linejoin:round}.register-defs{position:absolute;width:0!important;height:0!important;overflow:hidden}
.brand-rail{display:flex;width:420px;min-height:100vh;flex:0 0 420px;padding:40px 54px 36px;flex-direction:column;color:#fff;background:var(--navy)}.brand{display:flex;align-items:center;gap:12px;font-size:19px}.brand-mark{display:grid;width:38px;height:38px;place-items:center;color:var(--navy);background:var(--yellow);border-radius:10px 10px 4px 10px;font-size:18px;font-weight:800}.brand-message{margin-top:86px}.brand-message h1{margin:0 0 18px;font-size:40px;line-height:1.3;letter-spacing:-.03em}.brand-message p{margin:0;color:#bdcce1;font-size:14px;line-height:1.85}.benefit-list{display:grid;gap:16px;margin:30px 0 0;padding:0;list-style:none}.benefit-list li{display:grid;grid-template-columns:34px 1fr;gap:12px;align-items:center;font-size:13px;font-weight:700}.benefit-list svg{width:30px;height:30px;padding:7px;color:var(--navy);background:var(--yellow);border-radius:8px 8px 3px 8px}.brand-rail small{margin-top:auto;color:#8fa5c4;font-size:11px;letter-spacing:.04em}
.register-hall{display:flex;min-width:0;flex:1;align-items:center;justify-content:center;padding:70px}.auth-counter{width:min(760px,100%);animation:counter-arrive .55s cubic-bezier(.22,1,.36,1) both}.auth-heading{display:flex;align-items:flex-end;justify-content:space-between;margin-bottom:28px;padding-bottom:22px;border-bottom:1px solid var(--line)}.auth-heading h1{margin:0;font-size:34px;line-height:1.25;letter-spacing:-.025em}.auth-heading p{margin:6px 0 0;color:var(--muted);font-size:13px}.security-state{display:flex;align-items:center;gap:7px;padding:7px 10px;color:#315e3f;background:#e2efe4;border-radius:6px;font-size:11px;font-weight:700}.security-state i{width:7px;height:7px;background:#4da572;border-radius:50%}.auth-layout{display:grid;grid-template-columns:minmax(0,1fr) 230px;gap:40px;align-items:start}.auth-form{display:grid;gap:18px}.form-field{display:grid;gap:8px}.form-field label{font-size:13px;font-weight:600}.field-control{display:flex;height:50px;align-items:center;gap:11px;padding:0 14px;color:#718097;background:#fff;border:1px solid #cfd6e1;border-radius:9px;transition:border-color .18s ease,box-shadow .18s ease}.field-control:focus-within{border-color:var(--blue);box-shadow:0 0 0 3px rgb(23 79 167 / 14%)}.field-control.invalid{border-color:#a23843;box-shadow:0 0 0 3px rgb(162 56 67 / 10%)}.field-control input{width:100%;min-width:0;border:0;outline:0;color:var(--ink);background:transparent;font-size:14px;caret-color:var(--blue)}.field-control input::placeholder{color:#7a8497;opacity:1}.visibility-button{display:grid;padding:4px;place-items:center;color:#657087;background:transparent;border:0;border-radius:6px}.visibility-button:hover{color:var(--blue)}.visibility-button:focus-visible,.mode-panel a:focus-visible,.auth-submit:focus-visible{outline:3px solid rgb(23 79 167 / 24%);outline-offset:3px}.field-error{margin:0;color:#a23843;font-size:11px}.form-status{margin:0;padding:10px 12px;color:#a23843;background:#f8e7e9;border-radius:6px;font-size:12px;line-height:1.6}
.auth-submit{display:flex;width:100%;height:50px;align-items:center;justify-content:center;gap:9px;margin-top:7px;color:#fff;background:var(--blue);border:0;border-radius:9px;box-shadow:0 9px 20px rgb(23 79 167 / 22%);font-size:14px;font-weight:700;transition:transform .18s ease,box-shadow .18s ease}.auth-submit:hover:not(:disabled){transform:translateY(-1px);box-shadow:0 12px 25px rgb(23 79 167 / 27%)}.auth-submit:disabled{cursor:wait;opacity:.72}.auth-submit svg{width:17px}.spinner{width:16px;height:16px;border:2px solid rgb(255 255 255 / 35%);border-top-color:#fff;border-radius:50%;animation:spin .7s linear infinite}.mode-panel{min-height:248px;padding:25px;background:#e9eef5;border-radius:12px}.mode-panel h2{margin:0 0 9px;font-size:16px;line-height:1.4}.mode-panel p{margin:0;color:var(--muted);font-size:12px;line-height:1.75}.mode-panel a{display:flex;align-items:center;gap:7px;width:max-content;margin-top:25px;color:var(--blue);font-size:12px;font-weight:700}.mode-panel a svg{width:16px;transition:transform .18s ease}.mode-panel a:hover svg{transform:translateX(3px)}.register-footer{display:flex;gap:22px;margin-top:42px;padding-top:20px;color:#7c8597;border-top:1px solid var(--line);font-size:11px}.register-footer em{margin-left:auto;font-style:normal}@keyframes counter-arrive{from{opacity:.65;transform:translateY(12px);filter:blur(4px)}to{opacity:1;transform:none;filter:none}}@keyframes spin{to{transform:rotate(360deg)}}@media(prefers-reduced-motion:reduce){.auth-counter,.spinner{animation:none}.auth-submit,.mode-panel a svg{transition:none}}
</style>
