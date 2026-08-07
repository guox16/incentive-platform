import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
export default defineConfig({
  plugins: [vue({ template: { compilerOptions: { comments: true } } })],
  server: { proxy: { '/api/v1': 'http://localhost:8080' } },
});
