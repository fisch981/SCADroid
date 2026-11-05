package de.fisch981.scadroid

import de.fisch981.scadroid.engine.CsgSolid
import de.fisch981.scadroid.engine.ScadMathEvaluator
import de.fisch981.scadroid.engine.ScadParser
import de.fisch981.scadroid.engine.TriangleMesh
import de.fisch981.scadroid.engine.Vector3
import org.junit.Assert.*
import org.junit.Test

class CsgEngineTest {

    @Test
    fun testMathEvaluatorBasic() {
        val vars = mapOf("a" to 10.0, "b" to 2.5, "fn" to 32.0)
        val eval = ScadMathEvaluator(vars)

        assertEquals(23.5, eval.eval("47/2"), 1e-4)
        assertEquals(35.5, eval.eval("47/2+12"), 1e-4)
        assertEquals(11.5, eval.eval("47/2-12"), 1e-4)
        assertEquals(7.5, eval.eval("23.5-16"), 1e-4)
        assertEquals(40.0, eval.eval("24-16+32"), 1e-4)
        assertEquals(25.0, eval.eval("a * b"), 1e-4)
        assertEquals(30.0, eval.eval("(a + 5) * 2"), 1e-4)
        assertEquals(-5.0, eval.eval("-5"), 1e-4)
        assertEquals(8.0, eval.eval("2^3"), 1e-4)
        assertEquals(1.0, eval.eval("10 % 3"), 1e-4)
    }

    @Test
    fun testMathEvaluatorTrigAndFunctions() {
        val eval = ScadMathEvaluator()

        // OpenSCAD verwendet Grad (Degrees) für trigonometrische Funktionen
        assertEquals(1.0, eval.eval("sin(90)"), 1e-4)
        assertEquals(0.0, eval.eval("sin(0)"), 1e-4)
        assertEquals(1.0, eval.eval("cos(0)"), 1e-4)
        assertEquals(0.0, eval.eval("cos(90)"), 1e-4)
        assertEquals(1.0, eval.eval("tan(45)"), 1e-4)
        assertEquals(90.0, eval.eval("asin(1)"), 1e-4)
        assertEquals(0.0, eval.eval("acos(1)"), 1e-4)
        assertEquals(45.0, eval.eval("atan(1)"), 1e-4)
        assertEquals(45.0, eval.eval("atan2(1, 1)"), 1e-4)

        // Standardfunktionen
        assertEquals(4.0, eval.eval("sqrt(16)"), 1e-4)
        assertEquals(10.0, eval.eval("abs(-10)"), 1e-4)
        assertEquals(3.0, eval.eval("min(3, 7)"), 1e-4)
        assertEquals(7.0, eval.eval("max(3, 7)"), 1e-4)
        assertEquals(8.0, eval.eval("pow(2, 3)"), 1e-4)
        assertEquals(Math.PI, eval.eval("PI"), 1e-4)
        assertEquals(5.0, eval.eval("round(4.8)"), 1e-4)
        assertEquals(4.0, eval.eval("floor(4.8)"), 1e-4)
        assertEquals(5.0, eval.eval("ceil(4.2)"), 1e-4)

        // Ternärer Operator & Vergleiche
        assertEquals(42.0, eval.eval("1 > 0 ? 42 : 10"), 1e-4)
        assertEquals(10.0, eval.eval("5 < 2 ? 42 : 10"), 1e-4)
    }

    @Test
    fun testMathEvaluatorVectorIndexing() {
        val vecVars = mapOf("dim" to listOf(47.0, 30.0, 15.0))
        val eval = ScadMathEvaluator(vectorVariables = vecVars)

        assertEquals(47.0, eval.eval("dim[0]"), 1e-4)
        assertEquals(30.0, eval.eval("dim[1]"), 1e-4)
        assertEquals(15.0, eval.eval("dim[2]"), 1e-4)
        assertEquals(77.0, eval.eval("dim[0] + dim[1]"), 1e-4)
    }

    @Test
    fun testDefaultSampleCode() {
        val sampleCode = """
            // SCADroid parametrisches Modell
            /* [Geometrie] */
            breite = 30; // [10:1:60]
            hoehe = 15; // [5:1:40]
            bohrung = 7; // [2:1:14]

            /* [Qualitaet] */
            ${'$'}fn = 32;

            difference() {
                cube([breite, breite, hoehe], center = true);
                cylinder(r = bohrung, h = hoehe + 4, center = true);
            }
        """.trimIndent()

        val parser = ScadParser()
        val result = parser.parseAndEvaluate(sampleCode)
        val mesh = TriangleMesh.fromSolid(result.solid)

        println("Sample Code mesh triangle count: ${mesh.triangleCount}")
        assertTrue("Sample model should create cut hole (triangles > 12, got ${mesh.triangleCount})", mesh.triangleCount > 12)
    }

    @Test
    fun testUserCodeParsing() {
        val userCode = """
            ${'$'}fn=48;

            difference()
            {
                cube([47,47,34]);
                translate ([3,0,3]) cube([41,44,28]);
                translate ([23.5,23.5,0]) cylinder(3,16,16);
                translate ([23.5-16,0,0]) cube([32,23.5,3]);

                translate ([47/2,47/2+12,31]) cylinder(3,2,2);
                translate ([47/2,47/2+12,31]) cylinder(4,4,0);

                translate ([47/2,47/2-12,31]) cylinder(3,2,2);
                translate ([47/2,47/2-12,31]) cylinder(4,4,0);
            }
            translate ([23-16,2,0]) cylinder(3,2,2);
            translate ([24-16+32,2,0]) cylinder(3,2,2);
        """.trimIndent()

        val parser = ScadParser()
        val result = parser.parseAndEvaluate(userCode)
        val mesh = TriangleMesh.fromSolid(result.solid)

        println("User Code mesh triangle count: ${mesh.triangleCount}")
        println("User Code bounds: min=${mesh.minBound}, max=${mesh.maxBound}")

        assertTrue("User model should have many triangles (got ${mesh.triangleCount})", mesh.triangleCount >= 100)
    }

    @Test
    fun testVectorVariableInCubeAndTranslate() {
        val code = """
            box = [50, 40, 20];
            pos = [10, 5, 0];
            translate(pos) cube(box);
        """.trimIndent()

        val parser = ScadParser()
        val result = parser.parseAndEvaluate(code)
        val mesh = TriangleMesh.fromSolid(result.solid)

        assertEquals(12, mesh.triangleCount)
        assertEquals(10f, mesh.minBound.x, 1e-3f)
        assertEquals(60f, mesh.maxBound.x, 1e-3f)
    }

    @Test
    fun testMirrorAndColor() {
        val code = """
            color("blue") cube([10, 20, 30]);
            translate([-5, 0, 0]) mirror([1, 0, 0]) color("red") cube([10, 20, 30]);
        """.trimIndent()

        val parser = ScadParser()
        val result = parser.parseAndEvaluate(code)
        val mesh = TriangleMesh.fromSolid(result.solid)

        println("Mirror and Color mesh triangle count: ${mesh.triangleCount}, minBound: ${mesh.minBound}, maxBound: ${mesh.maxBound}")
        assertTrue(mesh.triangleCount >= 20)
        assertTrue(mesh.minBound.x < 0f)
        assertTrue(mesh.maxBound.x > 0f)
    }

    @Test
    fun testForLoop() {
        val code = """
            for (i = [0:3]) {
                translate([i * 15, 0, 0]) cube([10, 10, 10]);
            }
        """.trimIndent()

        val parser = ScadParser()
        val result = parser.parseAndEvaluate(code)
        val mesh = TriangleMesh.fromSolid(result.solid)

        // 4 Würfel = 48 Dreiecke
        assertEquals(48, mesh.triangleCount)
        assertEquals(0f, mesh.minBound.x, 1e-3f)
        assertEquals(55f, mesh.maxBound.x, 1e-3f)
    }

    @Test
    fun testUserModule() {
        val code = """
            module pin(h = 10, r = 2) {
                cylinder(h = h, r = r);
            }

            pin(15, 3);
            translate([10, 0, 0]) pin(r = 4);
        """.trimIndent()

        val parser = ScadParser()
        val result = parser.parseAndEvaluate(code)
        val mesh = TriangleMesh.fromSolid(result.solid)

        assertTrue("Module calls should render solids", mesh.triangleCount > 0)
    }

    @Test
    fun testLinearExtrude() {
        val code = """
            linear_extrude(height = 20) square([30, 40]);
        """.trimIndent()

        val parser = ScadParser()
        val result = parser.parseAndEvaluate(code)
        val mesh = TriangleMesh.fromSolid(result.solid)

        assertTrue(mesh.triangleCount >= 12)
        assertEquals(20f, mesh.maxBound.z, 1e-3f)
    }

    @Test
    fun testCylinderWithConeAndSegments() {
        // Hexagon: $fn = 6
        val hexCode = "cylinder(h = 10, r = 5, \$fn = 6);"
        val parser = ScadParser()
        val hexResult = parser.parseAndEvaluate(hexCode)
        val hexMesh = TriangleMesh.fromSolid(hexResult.solid)
        // 6 Seiten (12 Dreiecke) + 2 Deckelflächen (6 + 6 Dreiecke) = 24 Dreiecke
        assertTrue("Hexagon cylinder should render", hexMesh.triangleCount > 0)

        // Cone with r2 = 0
        val coneCode = "cylinder(h = 10, r1 = 5, r2 = 0, \$fn = 16);"
        val coneResult = parser.parseAndEvaluate(coneCode)
        val coneMesh = TriangleMesh.fromSolid(coneResult.solid)
        assertTrue("Cone should produce valid triangles without crashing", coneMesh.triangleCount > 0)
    }

    @Test
    fun testEmptyOrNonGeometryCodeYieldsEmptyMesh() {
        val parser = ScadParser()
        val result = parser.parseAndEvaluate("\$fn = 32;\n")
        val mesh = TriangleMesh.fromSolid(result.solid)

        assertEquals(0, mesh.triangleCount)
        assertEquals(0, mesh.vertexCount)
    }

    @Test
    fun testMinkowskiAndHull() {
        val minkowskiCode = """
            minkowski() {
                cube([10, 10, 10], center = true);
                sphere(r = 2, ${'$'}fn = 8);
            }
        """.trimIndent()
        val p1 = ScadParser()
        val r1 = p1.parseAndEvaluate(minkowskiCode)
        val m1 = TriangleMesh.fromSolid(r1.solid)
        assertTrue("Minkowski should produce geometry (got ${m1.triangleCount})", m1.triangleCount > 0)

        val hullCode = """
            hull() {
                translate([0, 0, 0]) cube([5, 5, 5]);
                translate([20, 0, 0]) cube([5, 5, 5]);
            }
        """.trimIndent()
        val p2 = ScadParser()
        val r2 = p2.parseAndEvaluate(hullCode)
        val m2 = TriangleMesh.fromSolid(r2.solid)
        assertTrue("Hull should produce geometry (got ${m2.triangleCount})", m2.triangleCount > 0)
    }

    @Test
    fun testIfElseAndStringVariables() {
        val code = """
            output = "both";

            module partA() {
                cube([10, 10, 10]);
            }

            module partB() {
                cylinder(h = 10, r = 5);
            }

            if (output == "case") {
                partA();
            } else if (output == "lid") {
                partB();
            } else if (output == "both") {
                partA();
                translate([20, 0, 0]) partB();
            }
        """.trimIndent()

        val parser = ScadParser()
        val result = parser.parseAndEvaluate(code)
        val mesh = TriangleMesh.fromSolid(result.solid)
        assertTrue("If-Else string match should evaluate 'both' branch", mesh.triangleCount > 12)
        assertTrue("Mesh maxBound.x should extend to partB", mesh.maxBound.x > 15f)
    }

    @Test
    fun testESP32CaseGehaeuseSnippet() {
        val esp32Code = """
            ${'$'}fn = 16;
            output = "both";
            pcb_width = 28.0;
            pcb_depth = 55.4;
            wall = 4.0;
            outer_width = pcb_width + 2 * wall;
            outer_depth = pcb_depth + 2 * wall;
            case_height = 20.0;
            corner_r = 2.0;

            module rounded_box(width, depth, height, radius) {
                intersection() {
                    minkowski() {
                        cube([width - 2 * radius, depth - 2 * radius, height], center = true);
                        sphere(r = radius);
                    }
                    cube([width, depth, height], center = true);
                }
            }

            module case_body() {
                translate([0, 0, case_height / 2])
                    rounded_box(outer_width, outer_depth, case_height, corner_r);
            }

            module case_bottom() {
                case_body();
            }

            module lid() {
                translate([0, 0, 2])
                    cube([outer_width, outer_depth, 4], center = true);
            }

            if (output == "case") {
                case_bottom();
            } else if (output == "lid") {
                lid();
            } else if (output == "both") {
                case_bottom();
                translate([outer_width + 10, 0, 0]) lid();
            }
        """.trimIndent()

        val parser = ScadParser()
        val result = parser.parseAndEvaluate(esp32Code)
        val mesh = TriangleMesh.fromSolid(result.solid)

        println("ESP32 Case mesh triangles: ${mesh.triangleCount}, min=${mesh.minBound}, max=${mesh.maxBound}")
        assertTrue("ESP32 Case should produce valid geometry (triangles > 50, got ${mesh.triangleCount})", mesh.triangleCount > 50)
    }

    @Test
    fun testUserSlowScript() {
        val fullScript = """
            ${'$'}fn=32;
            difference()
            {
                minkowski()
                {
                    cube([43.5, 27.5,67]);
                    sphere(3);
                }
                translate([-3,-3,67]) cube([49.5,33.5,3]);
                cube([43.5, 27.5,67]);
            }
            difference()
            {
                translate([-1,-1,67]) cube([45.5, 29.5,5]);
                translate([0,0,67]) cube([43.5, 27.5,5]);
            }
            translate([8.6,-5.6,67]) scharnier(8.5);
            translate([8.7+8.7+8.6,-5.6,67]) scharnier(8.5);
            translate([8.7+8.7+8.7+8.7+8.6,-5.6,67]) scharnier(8.5);

            translate([0,70,5]) rotate([180,0,0])
            {
                difference()
                {
                    minkowski()
                    {
                        cube([43.5, 27.5,5]);
                        sphere(3);
                    }
                    translate([-3,-3,-3]) cube([49.5,33.5,3]);
                    translate([-1,-1,0]) cube([45.5, 29.5,5]);
                }
                translate([8.8,-5.6,-0.1]) rotate([0,180,0]) scharnier(8.5);
                translate([8.8+8.7+8.7,-5.6,-0.1]) rotate([0,180,0]) scharnier(8.5);
                hull()
                {
                    translate([17,30.5,3]) sphere(1.5);
                    translate([26.5,30.5,3]) sphere(1.5);
                }
            }

            difference()
            {
                translate([26.75,-3,50]) rotate([90,0,-90]) linear_extrude(10) polygon(points=[[0,0],[5,3],[5,6],[0,6]]);
                translate([26.75,-5,53.7]) rotate([90,0,-90]) cylinder(h=10, d=2);
            }

            translate([53,8,40]) difference()
            {
                cylinder(h=25, d1=16, d2=16);
                cylinder(h=25, d1=12, d2=12);
                translate([-9,-10,-2]) rotate([0,45,0]) cube([25,25,25]);
            }

            module scharnier(e)
            {
                rotate([90,0,-90]) linear_extrude(e) difference()
                {
                    hull()
                    {
                        circle(d=5);
                        polygon(points=[[-2.7,0],[2.5,0],[-2.7,-5]]);
                    }
                    circle(d=2);
                }
            }
        """.trimIndent()

        val parser = ScadParser()
        val t0 = System.currentTimeMillis()
        val result = parser.parseAndEvaluate(fullScript)
        val dt = System.currentTimeMillis() - t0
        println("Full user script evaluated in ${'$'}dt ms, total polygons: ${'$'}{result.solid.polygons.size}")
        assertTrue("Should evaluate in under 5 seconds (got ${'$'}dt ms)", dt < 5000)
        assertTrue("Polygons should be valid (> 50, got ${'$'}{result.solid.polygons.size})", result.solid.polygons.size > 50)
    }
}
