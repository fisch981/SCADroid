const fs = require("fs");
const path = require("path");
const assert = require("assert");

const ROOT_DIR = path.resolve(__dirname, "..");
const OPENSCAD_JS = path.join(ROOT_DIR, "app", "src", "main", "assets", "openscad", "openscad.js");
const FONT_PATH = path.join(ROOT_DIR, "app", "src", "main", "assets", "openscad", "LiberationSans-Regular.ttf");
const LIBS_DIR = path.join(ROOT_DIR, "app", "src", "main", "assets", "openscad", "libraries");

const { createOpenSCAD } = require(OPENSCAD_JS);

function setupBridgeMock() {
  const fontBase64 = fs.readFileSync(FONT_PATH).toString("base64");
  globalThis.AndroidBridge = {
    getFontBase64() {
      return fontBase64;
    },
    getProjectFileBase64(fileName) {
      const normalized = fileName.replace(/\\/g, "/").replace(/^\/+/, "");
      const leaf = path.posix.basename(normalized);
      const candidates = [
        path.join(LIBS_DIR, normalized),
        path.join(LIBS_DIR, "MCAD", normalized),
        path.join(LIBS_DIR, "MCAD", leaf)
      ];
      for (const p of candidates) {
        if (fs.existsSync(p) && fs.statSync(p).isFile()) {
          return fs.readFileSync(p).toString("base64");
        }
      }
      return "";
    }
  };
}

function parseScadOutput(b64) {
  assert(typeof b64 === "string" && b64.length > 0, "Expected non-empty base64 mesh output");
  const buf = Buffer.from(b64, "base64");
  assert(buf.length >= 84, `Buffer too small (${buf.length} bytes)`);
  const triCount = buf.readUInt32LE(80);
  assert(triCount > 0, `Expected triCount > 0, got ${triCount}`);

  const stlSize = 84 + triCount * 50;
  assert(buf.length >= stlSize + 8 + triCount * 4, "Missing SCADCOL1 trailer");
  const magic = buf.slice(stlSize, stlSize + 8).toString("ascii");
  assert.strictEqual(magic, "SCADCOL1", `Expected SCADCOL1 magic, got '${magic}'`);

  const attrs = new Set();
  const colors = new Set();
  const colorStart = stlSize + 8;

  for (let i = 0; i < triCount; i++) {
    const attr = buf.readUInt16LE(84 + i * 50 + 48);
    attrs.add(attr);
    const cOff = colorStart + i * 4;
    const rgba = `${buf[cOff]},${buf[cOff + 1]},${buf[cOff + 2]},${buf[cOff + 3]}`;
    colors.add(rgba);
  }

  return { triCount, attrs: Array.from(attrs), colors: Array.from(colors) };
}

async function runTests() {
  setupBridgeMock();
  const echoLines = [];
  const scad = await createOpenSCAD({
    print: t => {
      if (t && t.startsWith("ECHO:")) echoLines.push(t);
    },
    printErr: t => {
      if (t && t.startsWith("ECHO:")) echoLines.push(t);
    }
  });

  const cases = [
    {
      name: "color named ('red')",
      code: 'color("red") cube(10);',
      verify(res) {
        assert.strictEqual(res.triCount, 12);
        assert.deepStrictEqual(res.colors, ["255,0,0,255"]);
      }
    },
    {
      name: "color hex with alpha ('#00ff8880')",
      code: 'color("#00ff8880") cylinder(r=5, h=10);',
      verify(res) {
        assert.deepStrictEqual(res.colors, ["0,255,136,128"]);
      }
    },
    {
      name: "color RGBA vector [0.2, 0.6, 1.0, 0.4]",
      code: 'color([0.2, 0.6, 1.0, 0.4]) sphere(5);',
      verify(res) {
        assert.deepStrictEqual(res.colors, ["51,153,255,102"]);
      }
    },
    {
      name: "color alpha-only override color(alpha=0.5)",
      code: 'color(alpha=0.5) cube(10);',
      verify(res) {
        const hasAlpha = res.colors.some(c => {
          const a = parseInt(c.split(",")[3], 10);
          return a === 127 || a === 128;
        });
        assert(hasAlpha, `Expected ~50% alpha, got ${res.colors}`);
      }
    },
    {
      name: "boolean difference preserves per-face colors",
      code: 'difference() { color("blue") cube(20, center=true); color("red") cylinder(r=6, h=30, center=true); }',
      verify(res) {
        assert(res.colors.includes("0,0,255,255"), `Missing blue faces in ${res.colors}`);
        assert(res.colors.includes("255,0,0,255"), `Missing red faces in ${res.colors}`);
      }
    },
    {
      name: "2D colored circle()",
      code: 'color("green") circle(10);',
      verify(res) {
        assert(res.colors.includes("0,128,0,255"), `Expected green 2D extrusion, got ${res.colors}`);
      }
    },
    {
      name: "2D colored text()",
      code: 'color("cyan") text("Hi", size=8);',
      verify(res) {
        assert(res.triCount > 0);
        assert(res.colors.includes("0,255,255,255"), `Expected cyan 2D text, got ${res.colors}`);
      }
    },
    {
      name: "modulo operator (%) does not trigger % modifier",
      code: 'x = 10 % 3; y = (2 + 5) % 4; cube([x + 2, y + 2, 5]);',
      verify(res) {
        assert.strictEqual(res.triCount, 12);
        assert.deepStrictEqual(res.attrs, [0]);
        assert.deepStrictEqual(res.colors, ["247,189,56,255"]);
      }
    },
    {
      name: "highlight (#) modifier in difference()",
      code: 'difference() { cube(20, center=true); #cylinder(r=5, h=30, center=true); }',
      verify(res) {
        assert(res.attrs.includes(0), "Missing opaque attr=0");
        assert(res.attrs.includes(1), "Missing highlight attr=1");
        assert(res.colors.includes("255,89,89,102"), "Missing highlight RGBA");
      }
    },
    {
      name: "background (%) modifier preserves explicit color",
      code: 'cube(10); translate([15,0,0]) %color("red") sphere(5);',
      verify(res) {
        assert(res.attrs.includes(0), "Missing opaque attr=0");
        assert(res.attrs.includes(2), "Missing background attr=2");
        assert(res.colors.includes("255,0,0,72"), `Missing tinted red background RGBA in ${res.colors}`);
      }
    },
    {
      name: "root (!) modifier isolates subtree",
      code: 'cube(100); !color("magenta") cube(5);',
      verify(res) {
        assert.strictEqual(res.triCount, 12);
        assert.deepStrictEqual(res.colors, ["255,0,255,255"]);
      }
    },
    {
      name: "disable (*) modifier omits subtree",
      code: 'cube(10); *sphere(20);',
      verify(res) {
        assert.strictEqual(res.triCount, 12);
      }
    },
    {
      name: "roof() builtin",
      code: 'roof() square(10);',
      verify(res) {
        assert(res.triCount >= 4);
      }
    },
    {
      name: "fill() builtin",
      code: 'fill() difference() { square(20, center=true); circle(5); }',
      verify(res) {
        assert(res.triCount >= 4);
      }
    },
    {
      name: "polyhedron(), projection(), offset()",
      code: `
        polyhedron(
          points = [[0,0,0],[10,0,0],[0,10,0],[0,0,10]],
          faces = [[0,2,1],[0,1,3],[1,2,3],[2,0,3]]
        );
        translate([15,0,0]) linear_extrude(2) offset(r=1) projection() cube(5);
      `,
      verify(res) {
        assert(res.triCount >= 16);
      }
    },
    {
      name: "extrusions, hull, minkowski, transforms",
      code: `
        linear_extrude(height=10, twist=45, slices=10) square(5, center=true);
        translate([20,0,0]) rotate_extrude(angle=180) translate([6,0,0]) square(3);
        translate([40,0,0]) hull() { cube(4); translate([5,0,0]) sphere(2); }
        translate([60,0,0]) minkowski() { cube(4); sphere(1, $fn=8); }
        translate([80,0,0]) resize([6,4,2]) mirror([1,0,0]) scale([1,2,1]) rotate([10,20,30]) multmatrix([[1,0,0,0],[0,1,0,0],[0,0,1,0],[0,0,0,1]]) cube(3);
      `,
      verify(res) {
        assert(res.triCount > 50);
      }
    },
    {
      name: "language features: list comprehensions, let, each, lambdas, textmetrics, fontmetrics, math/string/type",
      code: `
        assert(is_undef(undef) && is_bool(true) && is_num(1.5) && is_string("a") && is_list([1]) && is_function(function(x) x));
        pts = [for (i = [0:3]) each [i, i + 0.5]];
        fn = function(x) x * 2;
        val = let(a = abs(-3), b = norm([3,4])) fn(a + b) + len(pts);
        c = cross([1,0,0], [0,1,0]);
        m = [sin(30), cos(60), tan(45), asin(0.5), acos(0.5), atan(1), atan2(1,1), sqrt(9), pow(2,3), exp(1), log(10), ln(2), sign(-5), round(1.6), floor(1.9), ceil(1.1)];
        s = str(chr(65), ord("A"), concat([1],[2]), lookup(2, [[1,10],[3,30]]), search("b", "abc"), rands(0,1,1,42));
        tm = textmetrics("Hi", size=8);
        fm = fontmetrics(size=8);
        echo("PARITY_CHECK", val, c, m[0], s, tm.size[0], fm.nominal.ascent);
        module wrapper() { children(); }
        wrapper() intersection_for(i = [0:1]) translate([i*2, 0, 0]) cube([tm.size[0], 5, 5]);
      `,
      verify(res) {
        assert(res.triCount > 0);
        assert(echoLines.some(l => l.includes("PARITY_CHECK")), "Expected PARITY_CHECK echo output");
      }
    },
    {
      name: "MCAD standard library (use <MCAD/boxes.scad>)",
      code: 'use <MCAD/boxes.scad>; roundedBox([10, 10, 10], 2, sidesonly=false);',
      verify(res) {
        assert(res.triCount > 12);
      }
    }
  ];

  let passed = 0;
  for (const tc of cases) {
    try {
      const b64 = await scad.renderToBinaryStlBase64(tc.code);
      const res = parseScadOutput(b64);
      tc.verify(res);
      console.log(`[PASS] ${tc.name} (tris=${res.triCount}, colors=${res.colors.length})`);
      passed++;
    } catch (err) {
      console.error(`[FAIL] ${tc.name}:`, err && err.stack ? err.stack : err);
      process.exitCode = 1;
    }
  }

  console.log(`\nOpenSCAD Desktop Parity Summary: ${passed}/${cases.length} passed.`);
  if (passed !== cases.length) {
    process.exit(1);
  }
}

runTests().catch(err => {
  console.error("Fatal error running parity tests:", err);
  process.exit(1);
});
