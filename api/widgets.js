const { ok, load } = require('./_lib');

module.exports = async (req, res) => {
  if (!ok(req)) return res.status(401).json({ error: 'unauthorized' });
  try { res.status(200).json({ widgets: await load() }); }
  catch (e) { res.status(500).json({ error: String(e.message || e) }); }
};
