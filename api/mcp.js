const { ok, load, save, loadData, saveData } = require('./_lib');

const GUIDE = 'Quick way: call create_widget with name, title, value, subtitle and style (big_number, progress_card, row_stats or minimal). ' +
  'value, title and subtitle may contain placeholders like {orders} or {stats.today} that are filled from the data store. ' +
  'Fill the data store with set_data (key and value); call get_data to see the current keys. Leave source_url empty unless the user has their own JSON endpoint (then call test_source to see its keys). ' +
  'Colors are #RRGGBB (bg, accent, text_color). For progress_card set progress to a placeholder or a number between 0 and 1, e.g. {goal}. ' +
  'Make each widget look different by changing style and colors. After saving, tell the user to open the phone app and tap Sync, then add the widget and pick the design by name. ' +
  'Advanced: save_widget with spec_json, for example: ' +
  '{\"name\":\"neon\",\"bg\":\"#0B0220\",\"radius\":28,\"padding\":14,\"layout\":\"column\",\"items\":[{\"type\":\"text\",\"text\":\"ORDERS\",\"size\":12,\"color\":\"#B794F4\"},{\"type\":\"text\",\"text\":\"{orders}\",\"size\":48,\"color\":\"#FFFFFF\",\"bold\":true},{\"type\":\"progress\",\"value\":\"{goal}\",\"color\":\"#F472B6\"}]}';

const S = { type: 'string' };
const TOOLS = [
  { name: 'how_to_create_widgets', description: 'Read this first. Explains how to create phone widgets.', inputSchema: { type: 'object', properties: {} } },
  {
    name: 'create_widget',
    description: 'Create or update a home-screen widget (same name overwrites). Easiest way to make a widget.',
    inputSchema: {
      type: 'object',
      properties: {
        name: { ...S, description: 'short unique name, e.g. orders_neon' },
        title: { ...S, description: 'small label text, e.g. ORDERS' },
        value: { ...S, description: 'main big text, e.g. {orders.today} or 42' },
        subtitle: { ...S, description: 'small line under the value' },
        style: { type: 'string', enum: ['big_number', 'progress_card', 'row_stats', 'minimal'] },
        bg: { ...S, description: 'background color #RRGGBB' },
        accent: { ...S, description: 'accent color #RRGGBB' },
        text_color: { ...S, description: 'main text color #RRGGBB' },
        progress: { ...S, description: 'for progress_card: number 0..1 or placeholder like {goal.ratio}' },
        source_url: { ...S, description: 'optional. Leave empty to use the built-in data store' },
        radius: { type: 'integer', description: 'corner radius 0-40' }
      },
      required: ['name']
    }
  },
  {
    name: 'save_widget',
    description: 'Advanced: save a full custom widget design. Pass spec_json (a JSON string). See how_to_create_widgets for an example.',
    inputSchema: {
      type: 'object',
      properties: {
        spec_json: { ...S, description: 'full widget design as a JSON string' },
        name: S, bg: S, radius: { type: 'integer' }, padding: { type: 'integer' }, layout: { type: 'string', enum: ['column', 'row'] },
        items: { type: 'array', items: { type: 'object' } }, source: { type: 'object' }
      }
    }
  },
  { name: 'set_data', description: 'Set a value in the data store that widgets display. Use key and value, or data_json for several at once.', inputSchema: { type: 'object', properties: { key: S, value: S, data_json: S } } },
  { name: 'get_data', description: 'Show the current data store values (the keys usable as placeholders).', inputSchema: { type: 'object', properties: {} } },
  { name: 'list_widgets', description: 'List saved widget designs.', inputSchema: { type: 'object', properties: {} } },
  { name: 'delete_widget', description: 'Delete a widget design by name.', inputSchema: { type: 'object', properties: { name: S }, required: ['name'] } },
  { name: 'test_source', description: 'Fetch a JSON URL and show a sample, to learn which keys exist for placeholders.', inputSchema: { type: 'object', properties: { url: S }, required: ['url'] } }
];

function build(a) {
  const bg = a.bg || '#12141A', accent = a.accent || '#4FD1C5', fg = a.text_color || '#FFFFFF', muted = '#8A93A6';
  const T = (text, size, color, bold) => ({ type: 'text', text: String(text), size, color, bold: !!bold });
  const sp = (size) => ({ type: 'spacer', size });
  const title = a.title || '', value = a.value || '--', sub = a.subtitle || '';
  let items, layout = 'column';
  if (a.style === 'minimal') items = [T(value, 48, fg, true)];
  else if (a.style === 'progress_card') items = [T(title, 12, muted), sp(4), T(value, 40, fg, true), sp(8), { type: 'progress', value: String(a.progress || '0.5'), color: accent }, sp(6), T(sub, 13, accent)];
  else if (a.style === 'row_stats') { layout = 'row'; items = [T(value, 38, accent, true), sp(12), T(title + (sub ? ' - ' + sub : ''), 14, fg)]; }
  else items = [T(title, 12, muted), T(value, 46, fg, true), T(sub, 13, accent)];
  items = items.filter((i) => i.type !== 'text' || i.text);
  const spec = { name: String(a.name).slice(0, 40), bg, radius: a.radius === undefined ? 24 : a.radius, padding: 14, layout, items };
  if (a.source_url) spec.source = { url: a.source_url };
  return spec;
}

async function store(spec) {
  if (!spec.name || !Array.isArray(spec.items)) throw new Error('name and items are required');
  spec.name = String(spec.name).slice(0, 40);
  const w = await load(); w[spec.name] = spec; await save(w);
  return 'Saved widget ' + spec.name + '. Tell the user: open the phone app, tap Sync, then add the widget to the home screen and pick ' + spec.name + '.';
}

async function callTool(name, a) {
  if (name === 'how_to_create_widgets') return GUIDE;
  if (name === 'create_widget') return store(build(a));
  if (name === 'save_widget') {
    const spec = a.spec_json ? JSON.parse(a.spec_json) : { ...a };
    if (typeof spec.items === 'string') spec.items = JSON.parse(spec.items);
    return store(spec);
  }
  if (name === 'set_data') {
    const add = a.data_json ? JSON.parse(a.data_json) : {};
    if (a.key) { const v = a.value; add[a.key] = typeof v === 'string' && v.trim() !== '' && !isNaN(Number(v)) ? Number(v) : v; }
    const d = { ...(await loadData()), ...add }; await saveData(d);
    return 'Data updated: ' + JSON.stringify(d) + '. The phone shows it after the next Sync.';
  }
  if (name === 'get_data') return JSON.stringify(await loadData());
  if (name === 'list_widgets') return JSON.stringify(await load(), null, 1);
  if (name === 'delete_widget') { const w = await load(); delete w[a.name]; await save(w); return 'Deleted ' + a.name; }
  if (name === 'test_source') {
    const ctl = new AbortController(); setTimeout(() => ctl.abort(), 8000);
    const r = await fetch(a.url, { signal: ctl.signal });
    return 'HTTP ' + r.status + ' ' + (await r.text()).slice(0, 3000);
  }
  throw new Error('Unknown tool ' + name);
}

async function handle(m) {
  const reply = (result) => ({ jsonrpc: '2.0', id: m.id, result });
  try {
    if (m.method === 'initialize')
      return reply({ protocolVersion: (m.params && m.params.protocolVersion) || '2025-03-26', capabilities: { tools: {} }, serverInfo: { name: 'widget-studio', version: '1.1.0' }, instructions: GUIDE });
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
