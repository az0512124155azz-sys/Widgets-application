const { put, list } = require('@vercel/blob');

async function loadFile(n) {
  const { blobs } = await list({ prefix: n, limit: 1 });
  if (!blobs.length) return {};
  const r = await fetch(blobs[0].url + '?t=' + Date.now());
  return await r.json();
}
const saveFile = (n, o) => put(n, JSON.stringify(o), {
  access: 'public', addRandomSuffix: false, allowOverwrite: true,
  contentType: 'application/json', cacheControlMaxAge: 60
});

exports.load = () => loadFile('widgets.json');
exports.save = (w) => saveFile('widgets.json', w);
exports.loadData = () => loadFile('data.json');
exports.saveData = (d) => saveFile('data.json', d);

exports.ok = (req) => {
  const key = process.env.MCP_KEY;
  if (!key) return false;
  return new URL(req.url, 'http://x').searchParams.get('key') === key;
};
