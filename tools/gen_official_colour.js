// "Official style" levels: one big colour that covers a whole row and reaches into the board, solvable with
// singles only (no guessing), and impossible WITHOUT crossing out the rest of a colour that already has a cat.
'use strict';
const { rng } = require('./gen_levels.js');

function units(n, reg) {
  const u = [];
  for (let g = 0; g < n; g++) u.push([]);
  reg.forEach((g, i) => u[g].push(i));
  return u;
}
// weak = the player forgets to cross out the rest of the cat's colour
function singles(n, reg, start, weak) {
  const U = units(n, reg);
  const cand = new Uint8Array(n * n).fill(1), cat = new Uint8Array(n * n);
  const place = i => {
    cat[i] = 1;
    const r = Math.floor(i / n), c = i % n;
    for (let j = 0; j < n * n; j++) {
      const rr = Math.floor(j / n), cc = j % n;
      if (rr === r || cc === c || (!weak && reg[j] === reg[i]) || (Math.abs(rr - r) <= 1 && Math.abs(cc - c) <= 1)) cand[j] = 0;
    }
    if (weak) cand[i] = 0;
  };
  place(start);
  let placed = 1;
  for (;;) {
    let f = -1;
    for (const u of U) {
      if (u.some(i => cat[i])) continue;
      const c = u.filter(i => cand[i] && !cat[i]);
      if (c.length === 1) { f = c[0]; break; }
    }
    if (f < 0) break;
    place(f); placed++;
  }
  return placed;
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
function build(n, R) {
  let sol;
  for (;;) {
    sol = [...Array(n).keys()];
    for (let i = n - 1; i > 0; i--) { const j = Math.floor(R() * (i + 1)); [sol[i], sol[j]] = [sol[j], sol[i]]; }
    if (sol.every((c, r) => r === 0 || Math.abs(c - sol[r - 1]) > 1)) break;
  }
  const solCell = new Set(sol.map((c, r) => r * n + c));
  const bigRow = R() < 0.5 ? 0 : n - 1;           // the big colour owns a whole edge row, like the official level
  const reg = new Array(n * n).fill(-1);
  sol.forEach((c, r) => { reg[r * n + c] = r; });
  for (let c = 0; c < n; c++) reg[bigRow * n + c] = bigRow;
  let left = reg.filter(x => x < 0).length, guard = 0;
  while (left > 0 && guard++ < 300000) {
    const i = Math.floor(R() * n * n); if (reg[i] >= 0) continue;
    const r = Math.floor(i / n), c = i % n;
    const nb = [r > 0 ? i - n : -1, r < n - 1 ? i + n : -1, c > 0 ? i - 1 : -1, c < n - 1 ? i + 1 : -1].filter(j => j >= 0 && reg[j] >= 0);
    if (!nb.length) continue;
    // the big colour grows a bit faster, so it snakes into the board
    const pick = nb.find(j => reg[j] === bigRow && R() < 0.45);
    reg[i] = reg[pick !== undefined ? pick : nb[Math.floor(R() * nb.length)]]; left--;
  }
  if (left > 0) return null;
  const given = bigRow * n + sol[bigRow];
  const sizes = () => { const s = new Array(n).fill(0); reg.forEach(g => s[g]++); return s; };
  const score = () => {
    const strong = singles(n, reg, given, false);
    if (strong < n) return strong;
    return n + (s => Math.max(...s))(sizes()) / (n * n);   // solved: prefer a bigger snaking colour                          // solved, and the more the colour rule is needed, the better
  };
  let cur = score();
  for (let it = 0; it < 500 * n * n; it++) {
    const i = Math.floor(R() * n * n);
    if (solCell.has(i) || Math.floor(i / n) === bigRow) continue;
    const r = Math.floor(i / n), c = i % n;
    const nb = [r > 0 ? i - n : -1, r < n - 1 ? i + n : -1, c > 0 ? i - 1 : -1, c < n - 1 ? i + 1 : -1].filter(j => j >= 0 && reg[j] !== reg[i]);
    if (!nb.length) continue;
    const old = reg[i], neu = reg[nb[Math.floor(R() * nb.length)]];
    reg[i] = neu;
    const s = sizes();
    if (!connected(n, reg, old) || s[old] < 1 || s[neu] > 3.4 * n) { reg[i] = old; continue; }
    const nx = score();
    if (nx >= cur || R() < 0.01) cur = nx; else reg[i] = old;
  }
  const s = sizes();
  if (singles(n, reg, given, false) < n) return null;
  const weak = singles(n, reg, given, true);
  if (s[bigRow] < 2 * n) return null;
  return { line: n + '|g|' + given + '|' + reg.map(x => String.fromCharCode(97 + x)).join('') + '|' + sol.join(''), weak: weak, big: s[bigRow] };
}
if (require.main === module) {
  const [n, count, seed0, secs] = process.argv.slice(2).map(Number);
  const out = [], t0 = Date.now();
  for (let k = 0; out.length < count && Date.now() - t0 < secs * 1000; k++) {
    const p = build(n, rng(seed0 + k * 7919));
    if (p) out.push(p);
  }
  process.stdout.write(JSON.stringify(out));
}
module.exports = { singles, build };
