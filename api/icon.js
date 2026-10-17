const zlib = require('zlib');
const cache = {};

function crc(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) {
    c ^= buf[i];
    for (let k = 0; k < 8; k++) c = (c >>> 1) ^ (0xedb88320 & -(c & 1));
  }
  return (c ^ 0xffffffff) >>> 0;
}
function chunk(type, data) {
  const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
  const td = Buffer.concat([Buffer.from(type), data]);
  const c = Buffer.alloc(4); c.writeUInt32BE(crc(td));
  return Buffer.concat([len, td, c]);
}
const rr = (x, y, a, b, c, d, r) => {
  if (x < a || x > c || y < b || y > d) return false;
  const cx = Math.min(Math.max(x, a + r), c - r), cy = Math.min(Math.max(y, b + r), d - r);
  return (x - cx) ** 2 + (y - cy) ** 2 <= r * r;
};
function color(x, y) {
  if (rr(x, y, 34, 34, 66, 44, 5)) return [255, 255, 255];
  if (rr(x, y, 34, 52, 54, 62, 5)) return [34, 211, 238];
  if (rr(x, y, 24, 24, 76, 76, 12)) return [11, 13, 20];
  return [124, 92, 255];
}
function png(n) {
  const raw = Buffer.alloc(n * (n * 3 + 1));
  let p = 0;
  for (let y = 0; y < n; y++) {
    raw[p++] = 0;
    for (let x = 0; x < n; x++) {
      let r = 0, g = 0, b = 0;
      for (let sy = 0; sy < 2; sy++) for (let sx = 0; sx < 2; sx++) {
        const c = color((x + sx / 2 + 0.25) * 100 / n, (y + sy / 2 + 0.25) * 100 / n);
        r += c[0]; g += c[1]; b += c[2];
      }
      raw[p++] = r >> 2; raw[p++] = g >> 2; raw[p++] = b >> 2;
    }
  }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(n, 0); ihdr.writeUInt32BE(n, 4); ihdr[8] = 8; ihdr[9] = 2;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}

module.exports = (req, res) => {
  const s = new URL(req.url, 'http://x').searchParams.get('size');
  const n = Math.min(Math.max(parseInt(s) || 192, 16), 1024);
  if (!cache[n]) cache[n] = png(n);
  res.setHeader('Content-Type', 'image/png');
  res.setHeader('Cache-Control', 'public, max-age=86400');
  res.status(200).end(cache[n]);
};
