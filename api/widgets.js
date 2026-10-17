const { ok, load, save, loadData } = require('./_lib');

module.exports = async (req, res) => {
  if (!ok(req)) return res.status(401).json({ error: 'unauthorized' });
  try {
    if (new URL(req.url, 'http://x').searchParams.get('selftest')) {
      const w = await load(); await save(w); return res.status(200).json({ selftest: 'ok', count: Object.keys(w).length });
    }
    res.status(200).json({ widgets: await load(), data: await loadData() });
  } catch (e) { res.status(500).json({ error: String(e.message || e) }); }
};
