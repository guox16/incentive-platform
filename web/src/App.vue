<script setup lang="ts">
import { useRoute } from 'vue-router';
import LoginView from './views/LoginView.vue';
import RegisterView from './views/RegisterView.vue';
import ProfileView from './views/ProfileView.vue';
import PointsView from './views/PointsView.vue';
import LotteryView from './views/LotteryView.vue';
import RedemptionView from './views/RedemptionView.vue';
import AdminPrizeManagementView from './views/AdminPrizeManagementView.vue';

const route = useRoute();

const navItems = [
  { label: '用户信息', icon: 'user' },
  { label: '积分明细', icon: 'points' },
  { label: '幸运抽奖', icon: 'gift' },
  { label: '兑换商城', icon: 'bag' },
];
</script>

<template>
  <LoginView v-if="route.name === 'login'" />
  <RegisterView v-else-if="route.name === 'register'" />
  <ProfileView v-else-if="route.name === 'profile'" />
  <PointsView v-else-if="route.name === 'points'" />
  <LotteryView v-else-if="route.name === 'lottery'" />
  <RedemptionView v-else-if="route.name === 'redemption'" />
  <AdminPrizeManagementView v-else-if="route.name === 'admin-prizes'" />
  <!--
  THESIS: 会员服务档案把身份、积分与后续行动组织成一张清晰的服务记录，拒绝通用卡片仪表盘。
  OWN-WORLD: 公共服务手册式的钴蓝、纸白、暖黄与深墨色；清晰分区、编号标签和克制圆角。
  STORY: 用户先确认身份与积分，再查看资料并完成修改或退出。
  FIRST VIEWPORT: 桌面端完整呈现导航、身份摘要、积分余额、联系方式、地址与主要操作。
  FORM: Operate 模式，候选方向 5，seed ecb5507a。
  FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, and DESIGN.md
  -->
  <div v-else class="preview variant-a">
    <svg class="icon-defs" aria-hidden="true">
      <symbol id="i-user" viewBox="0 0 24 24"><circle cx="12" cy="8" r="3.5"/><path d="M5 20c.5-4 3-6 7-6s6.5 2 7 6"/></symbol>
      <symbol id="i-points" viewBox="0 0 24 24"><circle cx="12" cy="12" r="8"/><path d="M9 9.5h4.2a2 2 0 0 1 0 4H10.8a2 2 0 0 0 0 4H15M12 7v2.5M12 17.5V20"/></symbol>
      <symbol id="i-gift" viewBox="0 0 24 24"><path d="M4 10h16v10H4zM3 7h18v3H3zM12 7v13M7.5 7C5 7 5 3.5 7.5 3.5c2 0 4.5 3.5 4.5 3.5s2.5-3.5 4.5-3.5C19 3.5 19 7 16.5 7"/></symbol>
      <symbol id="i-bag" viewBox="0 0 24 24"><path d="M5 8h14l-1 12H6L5 8Z"/><path d="M9 9V6a3 3 0 0 1 6 0v3"/></symbol>
      <symbol id="i-edit" viewBox="0 0 24 24"><path d="m5 16-1 4 4-1L19 8l-3-3L5 16Z"/><path d="m14.5 6.5 3 3"/></symbol>
      <symbol id="i-logout" viewBox="0 0 24 24"><path d="M10 5H5v14h5M14 8l4 4-4 4M8 12h10"/></symbol>
      <symbol id="i-pin" viewBox="0 0 24 24"><path d="M12 21s6-5.4 6-11a6 6 0 1 0-12 0c0 5.6 6 11 6 11Z"/><circle cx="12" cy="10" r="2"/></symbol>
      <symbol id="i-phone" viewBox="0 0 24 24"><path d="M7 4 5 6c0 7.2 5.8 13 13 13l2-2-4-3-2 2c-2.5-.8-5.2-3.5-6-6l2-2-3-4Z"/></symbol>
      <symbol id="i-arrow" viewBox="0 0 24 24"><path d="M5 12h14M14 7l5 5-5 5"/></symbol>
    </svg>

    <aside class="side-nav">
      <div class="brand"><span class="brand-mark">得</span><span>偶得</span></div>
      <nav aria-label="主导航">
        <a v-for="item in navItems" :key="item.label" href="#" :class="{ active: item.label === '用户信息' }">
          <svg><use :href="`#i-${item.icon}`" /></svg><span>{{ item.label }}</span>
        </a>
      </nav>
      <div class="side-foot"><span>服务中心</span><strong>400 · 888 · 2608</strong></div>
    </aside>

    <main class="workspace">
      <div class="sample-note">界面样图 · 内容为演示数据</div>

      <section class="identity-panel">
        <div class="avatar" aria-label="演示用户头像">Y</div>
        <div class="identity-copy">
          <p>欢迎回来</p>
          <h1>演示用户</h1>
          <div class="member-line"><span>悦享会员</span><span>账户状态正常</span></div>
        </div>
        <div class="points-block">
          <span>当前可用积分</span>
          <strong>2,680</strong>
          <a href="#">查看积分明细 <svg><use href="#i-arrow" /></svg></a>
        </div>
      </section>

      <div class="content-grid">
        <section class="details-panel">
          <div class="section-heading">
            <div><h2>个人资料</h2><p>用于账户识别与权益通知</p></div>
            <span>资料完整度 100%</span>
          </div>
          <dl class="details-list">
            <div><dt><svg><use href="#i-user" /></svg>昵称</dt><dd>演示用户</dd></div>
            <div><dt><svg><use href="#i-phone" /></svg>手机号</dt><dd>138 **** 2608</dd></div>
            <div><dt><svg><use href="#i-points" /></svg>会员等级</dt><dd><span class="level-tag">悦享会员</span></dd></div>
            <div class="address-row"><dt><svg><use href="#i-pin" /></svg>默认收货地址</dt><dd>上海市浦东新区示例路 88 号<br><small>演示地址，请以实际资料为准</small></dd></div>
          </dl>
        </section>

        <aside class="action-panel">
          <div class="action-copy"><span>账户管理</span><h2>资料有变化？</h2><p>及时更新联系方式与地址，确保兑换商品能够准确送达。</p></div>
          <button class="primary"><svg><use href="#i-edit" /></svg>修改个人资料</button>
          <button class="secondary"><svg><use href="#i-logout" /></svg>退出当前账户</button>
          <div class="last-login"><span>最近登录</span><strong>今天 09:42</strong><small>上海 · Chrome 浏览器</small></div>
        </aside>
      </div>

      <footer><span>积分规则</span><span>兑换帮助</span><span>隐私说明</span><em>© 偶得 · 演示界面</em></footer>
    </main>
  </div>
</template>
