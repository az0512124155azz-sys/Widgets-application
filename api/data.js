const { ok, loadData, saveData } = require('./_lib');

const num = (v) => (v.trim() !== '' && !isNaN(Number(v)) ? Number(v) : v);

module.exports = async (req, res) => {
  if (!ok(req)) return res.status(401).json({ error: 'unauthorized' });
  try {
    const q = new URL(req.url, 'http://x').searchParams;
    let add = {};
    q.forEach((v, k) => { if (k !== 'key') add[k] = num(v); });
    if (req.method === 'POST' || req.method === 'PUT') {
      let b = req.body;
      if (typeof b === 'string') b = JSON.parse(b);
      if (!b || typeof b !== 'object' || Array.isArray(b)) return res.status(400).json({ error: 'send a JSON object' });
      add = { ...add, ...b };
    }
    if (Object.keys(add).length) { const d = { ...(await loadData()), ...add }; await saveData(d); return res.status(200).json({ ok: true, data: d }); }
    res.status(200).json(await loadData());
  } catch (e) { res.status(500).json({ error: String(e.message || e) }); }
};
