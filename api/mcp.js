const { ok, load, save } = require('./_lib');

const DESIGN = 'Widget spec for an Android home-screen widget. The app draws it with a fixed engine, so only use these fields. ' +
  'Make every design distinct, bold and good looking: pick a strong palette and typographic hierarchy. ' +
  'Text may contain {json.path} placeholders (e.g. {orders.today}) filled from the JSON returned by source.url. Use test_source first to see the real keys.';

const ITEM = {
  type: 'object',
  properties: {
    type: { type: 'string', enum: ['text', 'spacer', 'progress'] },
    text: { type: 'string', description: 'for type=text; may contain {json.path} placeholders' },
    size: { type: 'number', description: 'text: font size sp (10-60); spacer: dp (4-24)' },
    color: { type: 'string', description: '#RRGGBB' },
    bold: { type: 'boolean' },
    value: { type: 'string', description: 'for type=progress: placeholder like {goal.progress} resolving to a number 0..1' }
  },
  required: ['type']
};

const TOOLS = [
  {
    name: 'save_widget',
    description: 'Create or update (same name overwrites) a widget design. ' + DESIGN,
    inputSchema: {
      type: 'object',
      properties: {
        name: { type: 'string', description: 'unique short id, shown in the phone picker' },
        bg: { type: 'string', description: 'background #RRGGBB' },
        radius: { type: 'integer', description: 'corner radius dp 0-40' },
        padding: { type: 'integer', description: 'dp 6-24' },
        layout: { type: 'string', enum: ['column', 'row'] },
        items: { type: 'array', items: ITEM },
        source: { type: 'object', properties: { url: { type: 'string', description: 'JSON endpoint the phone polls every ~15 min' } } }
      },
      required: ['name', 'items']
    }
  },
  { name: 'list_widgets', description: 'List saved widget designs with their specs.', inputSchema: { type: 'object', properties: {} } },
  {
    name: 'delete_widget', description: 'Delete a widget design by name.',
    inputSchema: { type: 'object', properties: { name: { type: 'string' } }, required: ['name'] }
  },
  {
    name: 'test_source', description: 'Fetch a JSON endpoint and return a sample so you can see which keys exist for placeholders.',
    inputSchema: { type: 'object', properties: { url: { type: 'string' } }, required: ['url'] }
  }
];

async function callTool(name, a) {
  if (name === 'save_widget') {
    if (!a.name || !Array.isArray(a.items)) throw new Error('name and items are required');
    const w = await load();
    w[String(a.name).slice(0, 40)] = { ...a, name: String(a.name).slice(0, 40) };
    await save(w);
    return 'Saved "' + a.name + '". On the phone: open the app and tap Sync, then add the widget and pick this design.';
  }
  if (name === 'list_widgets') return JSON.stringify(await load(), null, 1);
  if (name === 'delete_widget') {
    const w = await load(); delete w[a.name]; await save(w); return 'Deleted ' + a.name;
  }
  if (name === 'test_source') {
    const ctl = new AbortController(); setTimeout(() => ctl.abort(), 8000);
    const r = await fetch(a.url, { signal: ctl.signal });
    return 'HTTP ' + r.status + '\n' + (await r.text()).slice(0, 3000);
  }
  throw new Error('Unknown tool ' + name);
}

async function handle(m) {
  const reply = (result) => ({ jsonrpc: '2.0', id: m.id, result });
  try {
    if (m.method === 'initialize')
      return reply({ protocolVersion: (m.params && m.params.protocolVersion) || '2025-03-26', capabilities: { tools: {} }, serverInfo: { name: 'widget-studio', version: '1.0.0' } });
    if (m.method === 'ping') return reply({});
    if (m.method === 'tools/list') return reply({ tools: TOOLS });
    if (m.method === 'tools/call') {
      try { return reply({ content: [{ type: 'text', text: await callTool(m.params.name, m.params.arguments || {}) }] }); }
      catch (e) { return reply({ isError: true, content: [{ type: 'text', text: String(e.message || e) }] }); }
    }
    return { jsonrpc: '2.0', id: m.id, error: { code: -32601, message: 'Method not found' } };
  } catch (e) {
    return { jsonrpc: '2.0', id: m.id, error: { code: -32603, message: String(e.message || e) } };
  }
}

module.exports = async (req, res) => {
  if (!ok(req)) return res.status(401).json({ error: 'unauthorized' });
  if (req.method !== 'POST') return res.status(405).setHeader('Allow', 'POST').end();
  const body = req.body;
  const msgs = Array.isArray(body) ? body : [body];
  const out = [];
  for (const m of msgs) if (m && m.id !== undefined) out.push(await handle(m));
  if (!out.length) return res.status(202).end();
  res.status(200).json(Array.isArray(body) ? out : out[0]);
};
