const URL_ = process.env.KV_REST_API_URL || process.env.UPSTASH_REDIS_REST_URL;
const TOKEN = process.env.KV_REST_API_TOKEN || process.env.UPSTASH_REDIS_REST_TOKEN;

async function kv(cmd) {
  if (!URL_ || !TOKEN) throw new Error('Storage not connected (add Upstash Redis in Vercel Storage)');
  const r = await fetch(URL_, { method: 'POST', headers: { Authorization: 'Bearer ' + TOKEN }, body: JSON.stringify(cmd) });
  const j = await r.json();
  if (j.error) throw new Error(j.error);
  return j.result;
}

exports.load = async () => {
  const raw = await kv(['GET', 'widgets']);
  return raw ? JSON.parse(raw) : {};
};
exports.save = (w) => kv(['SET', 'widgets', JSON.stringify(w)]);

exports.ok = (req) => {
  const key = process.env.MCP_KEY;
  if (!key) return false;
  const q = new URL(req.url, 'http://x').searchParams.get('key');
  return q === key;
};
