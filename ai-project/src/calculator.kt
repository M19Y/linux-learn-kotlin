import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.util.Locale
import java.util.Scanner
import kotlin.math.*
import kotlin.system.exitProcess

class CalcException(message: String) : RuntimeException(message)

// ============================ Tokenizer ============================

enum class TT { NUM, IDENT, OP, LP, RP, COMMA, EOF }

data class Token(val type: TT, val text: String, val pos: Int)

fun tokenize(src: String): List<Token> {
    val out = mutableListOf<Token>()
    var i = 0
    val n = src.length

    while (i < n) {
        val c = src[i]
        when {
            c.isWhitespace() -> i++

            c.isDigit() || (c == '.' && i + 1 < n && src[i + 1].isDigit()) -> {
                val start = i
                if (c == '0' && i + 1 < n && (src[i + 1] == 'x' || src[i + 1] == 'X')) {
                    i += 2
                    val h = i
                    while (i < n && src[i].isDigit() || i < n && src[i] in 'a'..'f' || i < n && src[i] in 'A'..'F') i++
                    if (i == h) throw CalcException("heksadesimal tidak valid pada kolom ${start + 1}")
                    out.add(Token(TT.NUM, src.substring(h, i).toLong(16).toString(), start))
                } else if (c == '0' && i + 1 < n && (src[i + 1] == 'b' || src[i + 1] == 'B')) {
                    i += 2
                    val h = i
                    while (i < n && (src[i] == '0' || src[i] == '1')) i++
                    if (i == h) throw CalcException("biner tidak valid pada kolom ${start + 1}")
                    out.add(Token(TT.NUM, src.substring(h, i).toLong(2).toString(), start))
                } else {
                    var dot = false
                    while (i < n) {
                        val d = src[i]
                        if (d.isDigit()) i++
                        else if (d == '.' && !dot) { dot = true; i++ }
                        else break
                    }
                    if (i < n && (src[i] == 'e' || src[i] == 'E')) {
                        val j = i + 1
                        val ok = j < n && (src[j].isDigit() ||
                            ((src[j] == '+' || src[j] == '-') && j + 1 < n && src[j + 1].isDigit()))
                        if (ok) {
                            i = j + (if (src[j].isDigit()) 0 else 1)
                            while (i < n && src[i].isDigit()) i++
                        }
                    }
                    out.add(Token(TT.NUM, src.substring(start, i), start))
                }
                if (i < n && (src[i] == '(' || src[i].isLetter() || src[i] == '_'))
                    out.add(Token(TT.OP, "*", i))
            }

            c.isLetter() || c == '_' -> {
                val start = i
                while (i < n && (src[i].isLetterOrDigit() || src[i] == '_')) i++
                out.add(Token(TT.IDENT, src.substring(start, i), start))
            }

            c == '(' -> { out.add(Token(TT.LP, "(", i)); i++ }
            c == ')' -> {
                out.add(Token(TT.RP, ")", i)); i++
                if (i < n && (src[i].isDigit() || src[i] == '.' || src[i] == '(' ||
                        src[i].isLetter() || src[i] == '_'))
                    out.add(Token(TT.OP, "*", i))
            }
            c == ',' -> { out.add(Token(TT.COMMA, ",", i)); i++ }
            c in "+-*/%^!" -> { out.add(Token(TT.OP, c.toString(), i)); i++ }
            else -> throw CalcException("karakter tidak dikenal '$c' pada kolom ${i + 1}")
        }
    }
    out.add(Token(TT.EOF, "", n))
    return out
}

// ============================ AST ============================

sealed class Node {
    abstract fun eval(ctx: Ctx): Double
}

class NumNode(val v: Double) : Node() {
    override fun eval(ctx: Ctx) = v
}

class VarNode(val name: String) : Node() {
    override fun eval(ctx: Ctx): Double {
        ctx.vars[name]?.let { return it }
        CONSTANTS[name.lowercase()]?.let { return it }
        throw CalcException("variabel atau konstanta '$name' tidak dikenal")
    }
}

class NegNode(val child: Node) : Node() {
    override fun eval(ctx: Ctx) = -child.eval(ctx)
}

class BinNode(val op: String, val l: Node, val r: Node) : Node() {
    override fun eval(ctx: Ctx): Double {
        val a = l.eval(ctx)
        val b = r.eval(ctx)
        return when (op) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> if (b == 0.0) throw CalcException("pembagian dengan nol")
                   else a / b
            "%" -> if (b == 0.0) throw CalcException("modulo dengan nol")
                   else a % b
            "^" -> {
                if (a == 0.0 && b < 0.0) throw CalcException("nol tidak boleh dipangkatkan dengan bilangan negatif")
                if (a < 0.0 && b != floor(b)) throw CalcException("bilangan negatif hanya boleh dipangkatkan bilangan bulat")
                Math.pow(a, b)
            }
            else -> throw CalcException("operator '$op' tidak didukung")
        }
    }
}

class FactNode(val child: Node) : Node() {
    override fun eval(ctx: Ctx): Double {
        val v = child.eval(ctx)
        if (v != floor(v)) throw CalcException("faktorial hanya untuk bilangan bulat")
        return factorial(v.toLong())
    }
}

class CallNode(val name: String, val args: List<Node>) : Node() {
    override fun eval(ctx: Ctx): Double = callFunction(name, args.map { it.eval(ctx) }, ctx)
}

// ============================ Parser ============================

class Parser(private val toks: List<Token>) {
    private var p = 0

    private fun peek() = toks[p]
    private fun next(): Token { val t = toks[p]; if (p < toks.size - 1) p++; return t }
    private fun match(type: TT, text: String? = null) =
        peek().type == type && (text == null || peek().text == text)

    private fun expect(type: TT, what: String) {
        if (peek().type != type)
            throw CalcException("diharapkan $what pada kolom ${peek().pos + 1}")
        next()
    }

    fun parse(): Node {
        val node = parseExpr()
        if (peek().type != TT.EOF)
            throw CalcException("sintaks tidak valid dekat '${peek().text}' pada kolom ${peek().pos + 1}")
        return node
    }

    private fun parseExpr(): Node {
        var node = parseTerm()
        while (match(TT.OP, "+") || match(TT.OP, "-"))
            node = BinNode(next().text, node, parseTerm())
        return node
    }

    private fun parseTerm(): Node {
        var node = parseUnary()
        while (match(TT.OP, "*") || match(TT.OP, "/") || match(TT.OP, "%"))
            node = BinNode(next().text, node, parseUnary())
        return node
    }

    private fun parseUnary(): Node {
        if (match(TT.OP, "-")) { next(); return NegNode(parseUnary()) }
        if (match(TT.OP, "+")) { next(); return parseUnary() }
        return parsePower()
    }

    private fun parsePower(): Node {
        val base = parsePostfix()
        if (match(TT.OP, "^")) { next(); return BinNode("^", base, parseUnary()) }
        return base
    }

    private fun parsePostfix(): Node {
        var node = parsePrimary()
        while (match(TT.OP, "!")) { next(); node = FactNode(node) }
        return node
    }

    private fun parsePrimary(): Node {
        val t = peek()
        return when (t.type) {
            TT.NUM -> { next(); NumNode(t.text.toDouble()) }
            TT.LP -> { next(); val node = parseExpr(); expect(TT.RP, "')'"); node }
            TT.IDENT -> {
                next()
                if (match(TT.LP)) {
                    next()
                    val args = mutableListOf<Node>()
                    if (!match(TT.RP)) {
                        args.add(parseExpr())
                        while (match(TT.COMMA)) { next(); args.add(parseExpr()) }
                    }
                    expect(TT.RP, "')'")
                    if (t.text.lowercase() !in FUNCTIONS)
                        throw CalcException("fungsi '${t.text}' tidak dikenal pada kolom ${t.pos + 1}")
                    CallNode(t.text, args)
                } else VarNode(t.text)
            }
            TT.EOF -> throw CalcException("input tidak lengkap")
            else -> throw CalcException("sintaks tidak valid dekat '${t.text}' pada kolom ${t.pos + 1}")
        }
    }
}

// ============================ Fungsi & konstanta ============================

val FUNCTIONS = setOf(
    "sin", "cos", "tan", "asin", "acos", "atan", "atan2", "sinh", "cosh", "tanh",
    "ln", "log", "log2", "exp", "sqrt", "cbrt", "abs", "sign", "round", "floor", "ceil", "trunc",
    "fact", "perm", "npr", "comb", "ncr", "gcd", "lcm",
    "and", "or", "xor", "not", "shl", "shr",
    "sum", "avg", "mean", "min", "max", "std", "var", "pct", "hyp"
)

val CONSTANTS = mapOf(
    "pi" to PI, "e" to E, "tau" to 2.0 * PI,
    "phi" to (1.0 + sqrt(5.0)) / 2.0,
    "g" to 9.80665, "c" to 299792458.0
)

fun factorial(n: Long): Double {
    if (n < 0) throw CalcException("faktorial dari bilangan negatif tidak terdefinisi")
    if (n > 170) throw CalcException("hasil faktorial terlalu besar")
    var r = BigInteger.ONE
    for (k in 2..n) r = r.multiply(BigInteger.valueOf(k))
    return r.toDouble()
}

fun perm(n: Long, k: Long): Double {
    if (k < 0 || k > n) throw CalcException("argumen perm(n, k) tidak valid")
    var r = BigInteger.ONE
    for (i in 0 until k) r = r.multiply(BigInteger.valueOf(n - i))
    return r.toDouble()
}

fun comb(n: Long, k: Long): Double {
    if (k < 0 || k > n) return 0.0
    var r = BigInteger.ONE
    for (i in 0 until k)
        r = r.multiply(BigInteger.valueOf(n - i)).divide(BigInteger.valueOf(i + 1))
    return r.toDouble()
}

fun gcdLong(a: Long, b: Long): Long {
    var x = abs(a); var y = abs(b)
    while (y != 0L) { val t = y; y = x % y; x = t }
    return x
}

fun lcmLong(a: Long, b: Long): Long {
    if (a == 0L || b == 0L) return 0L
    return abs(a / gcdLong(a, b) * b)
}

fun stdDev(a: List<Double>): Double {
    if (a.isEmpty()) throw CalcException("fungsi statistik memerlukan minimal satu argumen")
    val m = a.average()
    return sqrt(a.map { (it - m) * (it - m) }.average())
}

fun variance(a: List<Double>): Double {
    if (a.isEmpty()) throw CalcException("fungsi statistik memerlukan minimal satu argumen")
    val m = a.average()
    return a.map { (it - m) * (it - m) }.average()
}

fun callFunction(name: String, a: List<Double>, ctx: Ctx): Double {
    val fname = name.lowercase()
    fun arg(i: Int): Double =
        if (i >= a.size) throw CalcException("fungsi '$fname' kekurangan argumen") else a[i]
    fun rad(x: Double) = if (ctx.degrees) Math.toRadians(x) else x
    fun deg(x: Double) = if (ctx.degrees) Math.toDegrees(x) else x
    fun longArg(i: Int): Long {
        val v = arg(i)
        if (v.isNaN() || v != floor(v))
            throw CalcException("argumen '$fname' harus berupa bilangan bulat")
        return v.toLong()
    }

    return when (fname) {
        // trigonometri (mengikuti mode sudut: derajat/radian)
        "sin" -> sin(rad(arg(0)))
        "cos" -> cos(rad(arg(0)))
        "tan" -> tan(rad(arg(0)))
        "asin" -> deg(asin(arg(0)))
        "acos" -> deg(acos(arg(0)))
        "atan" -> deg(atan(arg(0)))
        "atan2" -> deg(atan2(arg(0), arg(1)))
        "sinh" -> sinh(arg(0))
        "cosh" -> cosh(arg(0))
        "tanh" -> tanh(arg(0))
        // logaritma & eksponen
        "ln" -> {
            if (arg(0) <= 0) throw CalcException("logaritma hanya untuk bilangan positif")
            ln(arg(0))
        }
        "log" -> {
            if (a.size >= 2) {
                val base = arg(0); val x = arg(1)
                if (base <= 0 || base == 1.0) throw CalcException("basis logaritma tidak valid")
                if (x <= 0) throw CalcException("logaritma hanya untuk bilangan positif")
                ln(x) / ln(base)
            } else {
                if (arg(0) <= 0) throw CalcException("logaritma hanya untuk bilangan positif")
                log10(arg(0))
            }
        }
        "log2" -> {
            if (arg(0) <= 0) throw CalcException("logaritma hanya untuk bilangan positif")
            log2(arg(0))
        }
        "exp" -> exp(arg(0))
        "sqrt" -> {
            if (arg(0) < 0) throw CalcException("akar kuadrat dari bilangan negatif tidak terdefinisi")
            sqrt(arg(0))
        }
        "cbrt" -> cbrt(arg(0))
        "abs" -> abs(arg(0))
        "sign" -> sign(arg(0))
        "round" -> round(arg(0))
        "floor" -> floor(arg(0))
        "ceil" -> ceil(arg(0))
        "trunc" -> truncate(arg(0))
        "hyp" -> hypot(arg(0), arg(1))
        "pct" -> arg(0) / 100.0
        // kombinatorial
        "fact" -> factorial(longArg(0))
        "perm", "npr" -> perm(longArg(0), longArg(1))
        "comb", "ncr" -> comb(longArg(0), longArg(1))
        // bilangan bulat / bitwise
        "gcd" -> gcdLong(longArg(0), longArg(1)).toDouble()
        "lcm" -> lcmLong(longArg(0), longArg(1)).toDouble()
        "and" -> (longArg(0) and longArg(1)).toDouble()
        "or" -> (longArg(0) or longArg(1)).toDouble()
        "xor" -> (longArg(0) xor longArg(1)).toDouble()
        "not" -> longArg(0).inv().toDouble()
        "shl" -> (longArg(0) shl longArg(1).toInt()).toDouble()
        "shr" -> (longArg(0) shr longArg(1).toInt()).toDouble()
        // statistik
        "sum" -> { if (a.isEmpty()) throw CalcException("sum memerlukan argumen"); a.sum() }
        "avg", "mean" -> { if (a.isEmpty()) throw CalcException("avg memerlukan argumen"); a.average() }
        "min" -> { if (a.isEmpty()) throw CalcException("min memerlukan argumen"); a.min() }
        "max" -> { if (a.isEmpty()) throw CalcException("max memerlukan argumen"); a.max() }
        "std" -> stdDev(a)
        "var" -> variance(a)
        else -> throw CalcException("fungsi '$fname' tidak dikenal")
    }
}

// ============================ Konteks ============================

class Ctx {
    val vars: MutableMap<String, Double> = mutableMapOf("ans" to 0.0)
    var degrees: Boolean = true
    var decimals: Int = -1
    var sciDigits: Int = -1
    val history: MutableList<Pair<String, String>> = mutableListOf()
}

fun evalExpression(src: String, ctx: Ctx): Double {
    val node = Parser(tokenize(src)).parse()
    val v = node.eval(ctx)
    if (v.isNaN()) throw CalcException("hasil tidak terdefinisi")
    if (v.isInfinite()) throw CalcException("hasil tidak terhingga (overflow)")
    return v
}

// ============================ Format angka ============================

fun formatNumber(d: Double, ctx: Ctx): String {
    if (d.isNaN()) return "NaN"
    if (d.isInfinite()) return if (d > 0) "Infinity" else "-Infinity"
    if (ctx.decimals >= 0) return String.format(Locale.US, "%.${ctx.decimals}f", d)
    if (ctx.sciDigits >= 0) return String.format(Locale.US, "%.${ctx.sciDigits}E", d)
    if (d == 0.0 || d == -0.0) return "0"
    if (d == floor(d) && abs(d) < 1e16) return d.toLong().toString()
    return BigDecimal(d).setScale(12, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}

// ============================ Konversi satuan ============================

private val LENGTH = mapOf(
    "mm" to 0.001, "cm" to 0.01, "dm" to 0.1, "m" to 1.0, "dam" to 10.0, "hm" to 100.0, "km" to 1000.0,
    "mil" to 0.0254, "inch" to 0.0254, "ft" to 0.3048, "feet" to 0.3048, "yd" to 0.9144, "yard" to 0.9144,
    "mi" to 1609.344, "mile" to 1609.344, "nmi" to 1852.0
)
private val MASS = mapOf(
    "mg" to 1e-6, "g" to 1e-3, "kg" to 1.0, "ton" to 1000.0, "t" to 1000.0,
    "lb" to 0.45359237, "lbs" to 0.45359237, "ons" to 0.028349523125, "oz" to 0.028349523125,
    "carat" to 0.0002
)
private val VOLUME = mapOf(
    "ml" to 0.001, "cl" to 0.01, "l" to 1.0, "m3" to 1000.0,
    "tsp" to 0.00492892, "tbsp" to 0.0147868, "cup" to 0.236588,
    "pt" to 0.473176, "qt" to 0.946353, "gal" to 3.78541
)
private val TIME = mapOf(
    "ms" to 0.001, "s" to 1.0, "sec" to 1.0, "min" to 60.0, "jam" to 3600.0, "h" to 3600.0,
    "hari" to 86400.0, "minggu" to 604800.0, "tahun" to 3.1536e7
)
private val DATA = mapOf(
    "bit" to 0.125, "byte" to 1.0, "kb" to 1024.0, "mb" to 1048576.0,
    "gb" to 1073741824.0, "tb" to 1099511627776.0
)
private val TEMP = setOf("c", "f", "k", "celsius", "fahrenheit", "kelvin")

fun convertUnit(value: Double, from: String, to: String): Double {
    val f = from.lowercase()
    val t = to.lowercase()
    if (f in TEMP && t in TEMP) {
        val c = when (f) {
            "c", "celsius" -> value
            "f", "fahrenheit" -> (value - 32.0) * 5.0 / 9.0
            "k", "kelvin" -> value - 273.15
            else -> throw CalcException("satuan '$from' tidak dikenal")
        }
        return when (t) {
            "c", "celsius" -> c
            "f", "fahrenheit" -> c * 9.0 / 5.0 + 32.0
            "k", "kelvin" -> c + 273.15
            else -> throw CalcException("satuan '$to' tidak dikenal")
        }
    }
    val table = listOf(LENGTH, MASS, VOLUME, TIME, DATA).firstOrNull { f in it && t in it }
        ?: throw CalcException("satuan '$from' dan '$to' tidak dikenal atau tidak sejenis")
    return value * (table[f]!! / table[t]!!)
}

// ============================ Basis bilangan ============================

fun toBase(v: Double, base: Int, name: String): String {
    if (v != floor(v)) throw CalcException("perintah $name hanya untuk bilangan bulat")
    val n = v.toLong()
    return when {
        n < 0 -> "-" + (-n).toString(base)
        else -> n.toString(base)
    }.uppercase(Locale.US)
}

// ============================ Perintah ============================

fun handleCommand(line: String, ctx: Ctx): Boolean {
    val parts = line.split(Regex("\\s+"))
    val cmd = parts[0].lowercase()

    when (cmd) {
        "exit", "quit", "keluar" -> { println("Sampai jumpa!"); exitProcess(0) }
        "help", "?" -> { printHelp(); return true }
        "clear", "cls", "hapus" -> {
            ctx.vars.clear(); ctx.vars["ans"] = 0.0; ctx.history.clear()
            print("\u001b[H\u001b[2J"); System.out.flush(); return true
        }
        "history", "hist", "riwayat" -> { printHistory(ctx); return true }
        "vars", "mem", "variabel" -> { printVars(ctx); return true }
        "deg", "derajat" -> { ctx.degrees = true; println("Mode sudut: derajat (°)"); return true }
        "rad", "radian" -> { ctx.degrees = false; println("Mode sudut: radian"); return true }
        "norm" -> { ctx.decimals = -1; ctx.sciDigits = -1; println("Format angka: normal (otomatis)"); return true }
    }

    if (cmd == "fix" && parts.size == 2) {
        val n = parts[1].toIntOrNull()
        if (n == null || n < 0 || n > 15) { println("Error: penggunaan: fix <0..15>"); return true }
        ctx.decimals = n; ctx.sciDigits = -1
        println("Format angka: $n angka di belakang koma"); return true
    }
    if (cmd == "sci" && parts.size == 2) {
        val n = parts[1].toIntOrNull()
        if (n == null || n < 0 || n > 15) { println("Error: penggunaan: sci <0..15>"); return true }
        ctx.sciDigits = n; ctx.decimals = -1
        println("Format angka: notasi ilmiah dengan $n angka penting"); return true
    }
    if (cmd in setOf("hex", "bin", "oct") && parts.size >= 2) {
        val expr = parts.subList(1, parts.size).joinToString(" ")
        val v = evalExpression(expr, ctx)
        val base = when (cmd) { "hex" -> 16; "bin" -> 2; else -> 8 }
        println("= ${toBase(v, base, cmd)}")
        return true
    }
    if (cmd == "conv" && parts.size == 4) {
        val value = evalExpression(parts[1], ctx)
        println("= ${formatNumber(convertUnit(value, parts[2], parts[3]), ctx)} ${parts[3]}")
        return true
    }
    if ((cmd == "del" || cmd == "unset") && parts.size == 2) {
        if (parts[1] == "ans") { println("Error: variabel 'ans' tidak dapat dihapus"); return true }
        if (ctx.vars.remove(parts[1]) == null) println("Error: variabel '${parts[1]}' tidak ditemukan")
        else println("Variabel '${parts[1]}' dihapus")
        return true
    }
    return false
}

// ============================ Tampilan ============================

fun printHelp() {
    println("""
        |╭────────────────────── BANTUAN ──────────────────────╮
        │ Tulis ekspresi matematika apa adanya, lalu Enter.   │
        │ Karakter '#' diabaikan sebagai komentar.            │
        ├─────────────────────────────────────────────────────┤
        │ Operator:                                           │
        │   +  tambah      -  kurang     *  kali   /  bagi   │
        │   %  modulo      ^  pangkat    !  faktorial         │
        │   ( ) kurung dengan prioritas operasi yang benar    │
        │                                                     │
        │ Konstanta: pi, e, tau, phi, g, c                    │
        │                                                     │
        │ Fungsi (1 argumen):                                 │
        │   sin cos tan asin acos atan sinh cosh tanh         │
        │   ln  log(10)  log2  exp  sqrt  cbrt  abs  sign     │
        │   round floor ceil trunc pct  fact                  │
        │                                                     │
        │ Fungsi (2 argumen):                                 │
        │   log(b, x)   basis b      atan2(y, x)   hyp(a, b)  │
        │   perm(n,k)  comb(n,k)     gcd(a,b)     lcm(a,b)    │
        │   and or xor not shl shr  (bitwise, bilangan bulat) │
        │                                                     │
        │ Fungsi statistik (banyak argumen):                  │
        │   sum avg min max std var                           │
        │   Contoh: std(1, 2, 3, 4, 5)                        │
        │                                                     │
        │ Variabel:                                           │
        │   x = 5        simpan nilai                         │
        │   y = ans + 1  gunakan hasil sebelumnya (ans)       │
        │   del x        hapus variabel                       │
        │                                                     │
        │ Basis bilangan:  hex 255  /  bin 10  /  oct 64      │
        │   atau ketik langsung: 0xFF  0b1010                 │
        │                                                     │
        │ Konversi:  conv <nilai> <dari> <ke>                 │
        │   Contoh: conv 100 C F, conv 5 km mi, conv 1 kg lb  │
        │   Panjang: mm cm m km inch ft yd mi ...             │
        │   Massa  : mg g kg ton lb oz ...                    │
        │   Volume : ml l cup pt gal ...                      │
        │   Waktu  : s min jam hari minggu tahun              │
        │   Data   : bit byte kb mb gb tb                     │
        │   Suhu   : C F K                                    │
        │                                                     │
        │ Perintah lain:                                      │
        │   deg / rad   pilih mode sudut (default: derajat)   │
        │   fix <n>     tampilkan n angka di belakang koma    │
        │   sci <n>     notasi ilmiah                         │
        │   norm        kembali ke format normal              │
        │   vars        daftar variabel                       │
        │   history     lihat riwayat perhitungan             │
        │   clear       hapus layar, variabel & riwayat       │
        │   help         tampilkan bantuan ini                │
        │   exit         keluar                               │
        ╰─────────────────────────────────────────────────────╯
    """.trimIndent())
}

fun printHistory(ctx: Ctx) {
    if (ctx.history.isEmpty()) { println("(riwayat kosong)"); return }
    println("--- Riwayat ---")
    ctx.history.forEachIndexed { i, (expr, res) -> println("  [${i + 1}] $expr = $res") }
}

fun printVars(ctx: Ctx) {
    if (ctx.vars.isEmpty()) { println("(tidak ada variabel)"); return }
    println("--- Variabel ---")
    ctx.vars.toSortedMap().forEach { (k, v) ->
        val note = if (CONSTANTS.containsKey(k.lowercase())) "  (menimpa konstanta)" else ""
        println("  $k = ${formatNumber(v, ctx)}$note")
    }
}

// ============================ Program utama ============================

fun main() {
    val scanner = Scanner(System.`in`)
    val ctx = Ctx()

    println("=== Advanced Calculator (CLI) — Kotlin ===")
    println("Ketik 'help' untuk bantuan, 'exit' untuk keluar.")
    println("Mode sudut: derajat (°). Gunakan 'rad' untuk radian.")

    while (true) {
        print(if (ctx.degrees) "calc(deg)> " else "calc(rad)> ")
        if (!scanner.hasNextLine()) break

        val line = scanner.nextLine().substringBefore('#').trim()
        if (line.isEmpty()) continue

        try {
            if (handleCommand(line, ctx)) continue

            val assign = Regex("^([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*(.+)$").matchEntire(line)
            if (assign != null) {
                val name = assign.groupValues[1]
                val value = evalExpression(assign.groupValues[2], ctx)
                ctx.vars[name] = value
                ctx.vars["ans"] = value
                ctx.history.add(line to formatNumber(value, ctx))
                if (ctx.history.size > 50) ctx.history.removeAt(0)
                println("$name = ${formatNumber(value, ctx)}")
                continue
            }

            val result = evalExpression(line, ctx)
            ctx.vars["ans"] = result
            ctx.history.add(line to formatNumber(result, ctx))
            if (ctx.history.size > 50) ctx.history.removeAt(0)
            println("= ${formatNumber(result, ctx)}")
        } catch (e: CalcException) {
            println("Error: ${e.message}")
        } catch (e: Exception) {
            println("Error: tidak dapat menghitung ekspresi (${e.message ?: "kesalahan tidak diketahui"})")
        }
    }
    println("Sampai jumpa!")
}
