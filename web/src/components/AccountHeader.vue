<script setup lang="ts">
defineProps<{ active: 'profile' | 'points' | 'lottery' | 'redemption' | 'admin-prizes' }>();
</script>

<template>
  <header class="account-header">
    <svg class="header-defs" aria-hidden="true">
      <symbol id="header-user" viewBox="0 0 24 24"><circle cx="12" cy="8" r="3.5"/><path d="M5 20c.5-4 3-6 7-6s6.5 2 7 6"/></symbol>
      <symbol id="header-points" viewBox="0 0 24 24"><circle cx="12" cy="12" r="8"/><path d="M9 9.5h4.2a2 2 0 0 1 0 4H10.8a2 2 0 0 0 0 4H15M12 7v2.5M12 17.5V20"/></symbol>
      <symbol id="header-gift" viewBox="0 0 24 24"><path d="M4 10h16v10H4zM3 7h18v3H3zM12 7v13M7.5 7C5 7 5 3.5 7.5 3.5c2 0 4.5 3.5 4.5 3.5s2.5-3.5 4.5-3.5C19 3.5 19 7 16.5 7"/></symbol>
      <symbol id="header-bag" viewBox="0 0 24 24"><path d="M5 8h14l-1 12H6L5 8Z"/><path d="M9 9V6a3 3 0 0 1 6 0v3"/></symbol>
      <symbol id="header-admin" viewBox="0 0 24 24"><path d="M4 5h16v14H4z"/><path d="M8 5v14M12 9h5M12 13h5M12 17h3"/></symbol>
    </svg>

    <RouterLink class="brand" to="/profile" aria-label="偶得账户中心">
      <span class="brand-mark">得</span><span>偶得</span>
    </RouterLink>
    <nav aria-label="主导航">
      <RouterLink to="/profile" :class="{ active: active === 'profile' }">
        <svg><use href="#header-user" /></svg><span>用户信息</span>
      </RouterLink>
      <RouterLink to="/points" :class="{ active: active === 'points' }">
        <svg><use href="#header-points" /></svg><span>积分明细</span>
      </RouterLink>
      <RouterLink to="/lottery" :class="{ active: active === 'lottery' }">
        <svg><use href="#header-gift" /></svg><span>幸运抽奖</span>
      </RouterLink>
      <RouterLink to="/redemption" :class="{ active: active === 'redemption' }">
        <svg><use href="#header-bag" /></svg><span>兑换商城</span>
      </RouterLink>
      <RouterLink to="/admin/prizes" :class="{ active: active === 'admin-prizes' }">
        <svg><use href="#header-admin" /></svg><span>奖品管理</span>
      </RouterLink>
    </nav>
    <div class="support"><span>服务中心</span><strong>400 · 888 · 2608</strong></div>
  </header>
</template>

<style scoped>
.account-header{display:flex;height:74px;align-items:center;padding:0 48px;color:#eef4ff;background:var(--navy,#102f65);box-shadow:0 2px 8px rgb(16 47 101 / 14%)}
.header-defs{position:absolute;width:0!important;height:0!important;overflow:hidden}
.brand{display:flex;align-items:center;gap:12px;flex:0 0 180px;color:#fff;font-size:18px;font-weight:700;text-decoration:none}
.brand-mark{display:grid;width:34px;height:34px;place-items:center;color:var(--navy,#102f65);background:var(--yellow,#f1c84a);border-radius:9px 9px 4px 9px;font-size:17px;font-weight:800}
nav{display:flex;align-items:center;gap:6px;height:100%}
nav a{display:flex;align-items:center;gap:10px;height:42px;padding:0 15px;color:#afc0dc;border-radius:10px;font-size:14px;text-decoration:none;transition:color .18s ease,background .18s ease}
nav a:hover{color:#fff;background:#183f7b}
nav a:focus-visible,.brand:focus-visible{outline:3px solid rgb(241 200 74 / 55%);outline-offset:3px}
nav a.active{color:var(--navy,#102f65);background:var(--yellow,#f1c84a);font-weight:700}
nav a svg{width:18px;height:18px;fill:none;stroke:currentColor;stroke-width:1.7;stroke-linecap:round;stroke-linejoin:round}
.support{display:flex;align-items:baseline;gap:10px;margin-left:auto;color:#9eb1ce;font-size:12px}
.support strong{color:#f4f7fd;font-size:14px;font-variant-numeric:tabular-nums}
@media(prefers-reduced-motion:reduce){nav a{transition:none}}
</style>
