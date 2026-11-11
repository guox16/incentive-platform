<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { http } from '../api/http';
import type { UserAwardPage } from '../api/types';

const route = useRoute();
const panel = ref<HTMLElement | null>(null);
const awards = ref<UserAwardPage | null>(null);
const loading = ref(true);
const error = ref(false);
const requestedPage = ref(0);
const date = new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium', timeStyle: 'short' });

async function load(page: number) {
  requestedPage.value = page;
  loading.value = true;
  error.value = false;
  try {
    const response = await http.get<UserAwardPage>('/awards/me', { params: { page, size: 10 } });
    awards.value = response.data;
  } catch {
    error.value = true;
  } finally {
    loading.value = false;
  }
}

function revealPanel() {
  if (route.hash !== '#my-awards') return;
  panel.value?.scrollIntoView({ block: 'start' });
  panel.value?.focus({ preventScroll: true });
}

onMounted(() => {
  revealPanel();
  void load(0);
});
watch(() => route.hash, revealPanel, { flush: 'post' });
</script>

<template>
  <section id="my-awards" ref="panel" class="my-awards" tabindex="-1" aria-labelledby="my-awards-title">
    <h2 id="my-awards-title">我的奖品</h2>
    <p v-if="loading && !awards" role="status">正在加载奖品…</p>
    <div v-if="error" class="award-error" role="alert">
      <p>奖品暂时无法加载。</p>
      <button type="button" @click="load(requestedPage)">重新加载</button>
    </div>
    <template v-if="awards?.items.length">
      <table aria-labelledby="my-awards-title" :aria-busy="loading">
        <thead>
          <tr><th scope="col">奖品</th><th scope="col">类型</th><th scope="col">来源</th><th scope="col">发放时间</th></tr>
        </thead>
        <tbody>
          <tr v-for="award in awards.items" :key="award.id">
            <th scope="row">{{ award.name }}</th>
            <td>{{ award.type === 'POINTS' ? '积分奖励' : '虚拟权益' }}</td>
            <td>{{ award.source === 'LOTTERY' ? '活动抽奖' : '积分兑换' }}</td>
            <td><time :datetime="award.obtainedAt">{{ date.format(new Date(award.obtainedAt)) }}</time></td>
          </tr>
        </tbody>
      </table>
      <nav v-if="awards.totalPages > 1" aria-label="奖品分页">
        <button type="button" :disabled="loading || awards.page === 0" @click="load(awards.page - 1)">上一页</button>
        <span>第 {{ awards.page + 1 }} / {{ awards.totalPages }} 页</span>
        <button type="button" :disabled="loading || awards.page + 1 >= awards.totalPages" @click="load(awards.page + 1)">下一页</button>
      </nav>
    </template>
    <p v-else-if="!loading && !error">暂无已发放奖品</p>
  </section>
</template>

<style scoped>
.my-awards {
  margin-top: 22px;
  padding: 24px 32px;
  scroll-margin-block-start: 18px;
  color: var(--ink);
  background: var(--paper);
  border-radius: 16px;
}
.my-awards h2 {
  margin: 0 0 14px;
  font-size: 21px;
  letter-spacing: -.015em;
}
.my-awards p {
  margin: 10px 0;
  color: #697287;
  font-size: 14px;
}
.my-awards table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 14px;
}
.my-awards th, .my-awards td {
  padding: 12px 12px 12px 0;
  border-bottom: 1px solid var(--line);
}
.my-awards thead th {
  color: #697287;
  font-size: 13px;
  font-weight: 600;
}
.my-awards tbody th {
  width: 42%;
  font-weight: 600;
  overflow-wrap: anywhere;
}
.my-awards nav, .award-error {
  display: flex;
  align-items: center;
  gap: 14px;
}
.my-awards nav {
  justify-content: flex-end;
  margin-top: 16px;
  font-size: 13px;
}
.my-awards button {
  min-height: 36px;
  padding: 0 12px;
  color: var(--blue);
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 9px;
}
.my-awards button:disabled {
  color: #697287;
  cursor: default;
}
.my-awards :focus-visible, .my-awards:focus-visible {
  outline: 2px solid var(--blue);
  outline-offset: 3px;
}
</style>
