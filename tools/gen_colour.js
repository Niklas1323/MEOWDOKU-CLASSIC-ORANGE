// COLOUR-ONLY version of gen_singles: there is always a colour with exactly one free tile.
// "No tip ever needed" levels: starting from the helper (a given cat or a 1-tile color), after X-ing every
// tile the placed cats rule out (row, column, color, 8 neighbours) there is ALWAYS a row, column or color
// with exactly one free tile left. Built by local search: color areas are reshaped until that chain
// reaches the last cat. Forced steps imply a unique solution.
'use strict';
const { rng } = require('./gen_levels.js');

function units(n, reg) {
  const u = [];
  for (let g = 0; g < n; g++) u.push([]);
  reg.forEach((g, i) => u[g].push(i));
  return u;
}

// returns { placed, steps, choices[] } running only "one free tile left" logic
function singles(n, reg, start) {
  const U = units(n, reg);
  const cand = new Uint8Array(n * n).fill(1), cat = new Uint8Array(n * n);
  const place = i => {
    cat[i] = 1;
    const r = Math.floor(i / n), c = i % n;
    for (let j = 0; j < n * n; j++) {
      const rr = Math.floor(j / n), cc = j % n;
      if (rr === r || cc === c || reg[j] === reg[i] || (Math.abs(rr - r) <= 1 && Math.abs(cc - c) <= 1)) cand[j] = 0;
    }
  };
  place(start);
  let placed = 1; const choices = [];
  for (;;) {
    let forced = [];
    for (const u of U) {
      if (u.some(i => cat[i])) continue;
      const c = u.filter(i => cand[i]);
      if (c.length === 1 && !forced.includes(c[0])) forced.push(c[0]);
    }
    if (!forced.length) break;
    choices.push(forced.length);
    place(forced[0]); placed++;
  }
  return { placed, choices };
}

function connected(n, reg, g) {
  const cells = []; reg.forEach((x, i) => { if (x === g) cells.push(i); });
  if (!cells.length) return false;
  const seen = new Set([cells[0]]), st = [cells[0]];
  while (st.length) {
    const i = st.pop(), r = Math.floor(i / n), c = i % n;
    for (const j of [r > 0 ? i - n : -1, r < n - 1 ? i + n : -1, c > 0 ? i - 1 : -1, c < n - 1 ? i + 1 : -1])
      if (j >= 0 && reg[j] === g && !seen.has(j)) { seen.add(j); st.push(j); }
  }
  return seen.size === cells.length;
}

function build(n, R, aid) {
  let sol;
  for (;;) {
    sol = [...Array(n).keys()];
    for (let i = n - 1; i > 0; i--) { const j = Math.floor(R() * (i + 1)); [sol[i], sol[j]] = [sol[j], sol[i]]; }
    if (sol.every((c, r) => r === 0 || Math.abs(c - sol[r - 1]) > 1)) break;
  }
  const solCell = new Set(sol.map((c, r) => r * n + c));
  const frozen = aid === 'tile' ? Math.floor(R() * n) : -1;
  const reg = new Array(n * n).fill(-1);
  sol.forEach((c, r) => { reg[r * n + c] = r; });
  let left = n * n - n, guard = 0;
  while (left > 0 && guard++ < 200000) {
    const i = Math.floor(R() * n * n); if (reg[i] >= 0) continue;
    const r = Math.floor(i / n), c = i % n;
    const nb = [r > 0 ? i - n : -1, r < n - 1 ? i + n : -1, c > 0 ? i - 1 : -1, c < n - 1 ? i + 1 : -1].filter(j => j >= 0 && reg[j] >= 0 && reg[j] !== frozen);
    if (!nb.length) continue;
    reg[i] = reg[nb[Math.floor(R() * nb.length)]]; left--;
  }
  if (left > 0) return null;
  const maxSize = Math.round(2.6 * n), minSize = aid === 'given' ? 2 : 1;
  const sizes = () => { const s = new Array(n).fill(0); reg.forEach(g => s[g]++); return s; };
  const startOf = () => {
    if (aid === 'tile') return [frozen * n + sol[frozen]];
    return sol.map((c, r) => r * n + c);
  };
  const evalReg = () => {
    let best = null;
    for (const st of startOf()) {
      const s = singles(n, reg, st);
      const sc = s.placed;
      if (!best || sc > best.sc) best = { sc, st, s };
    }
    return best;
  };
  let cur = evalReg();
  const iters = 400 * n * n;
  for (let it = 0; it < iters && cur.sc < n; it++) {
    const i = Math.floor(R() * n * n);
    if (solCell.has(i) || reg[i] === frozen) continue;
    const r = Math.floor(i / n), c = i % n;
    const nb = [r > 0 ? i - n : -1, r < n - 1 ? i + n : -1, c > 0 ? i - 1 : -1, c < n - 1 ? i + 1 : -1].filter(j => j >= 0 && reg[j] !== reg[i] && reg[j] !== frozen);
    if (!nb.length) continue;
    const old = reg[i], neu = reg[nb[Math.floor(R() * nb.length)]];
    reg[i] = neu;
    const s = sizes();
    if (!connected(n, reg, old) || s[old] < minSize || s[neu] > maxSize) { reg[i] = old; continue; }
    const nx = evalReg();
    if (nx.sc >= cur.sc || R() < 0.02) cur = nx; else reg[i] = old;
  }
  if (cur.sc < n) return null;
  const s = sizes();
  if (aid === 'tile' && s.filter(x => x === 1).length !== 1) return null;
  if (aid === 'given' && s.some(x => x < 2)) return null;
  // difficulty: steps with only one forced tile on the whole board are the "hard to spot" ones
  const hard = cur.s.choices.filter(x => x === 1).length;
  return { n, aid, reg: reg.map(x => String.fromCharCode(97 + x)).join(''), sol: sol.join(''), givens: aid === 'given' ? [cur.st] : [], hard, score: hard };
}

if (require.main === module) {
  const [n, count, seed0, secs] = process.argv.slice(2).map(Number);
  const out = [], t0 = Date.now();
  for (let k = 0; out.length < count && Date.now() - t0 < secs * 1000; k++) {
    const R = rng(seed0 + k * 7919);
    const p = build(n, R, k % 2 === 0 ? 'tile' : 'given');
    if (p) out.push(p);
  }
  process.stdout.write(JSON.stringify(out));
}
module.exports = { singles, build };
