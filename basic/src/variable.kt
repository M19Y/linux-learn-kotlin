
/* Variable
 *
 * Variable adalah cara kita menyimpan sebuah data
 * Bayangkan kita menyimpan buah manga di dalam sebuah kotak
 * variable adalah kotak, dan manga adalah isi dari kotak tersebut
 *
 * Ada 2 cara menyimpan data di kotlin:
 * 1. Data yang bisa berubah ubah (mutable)
 * - Data yang harus di tulis dengan awalan 'var' -> vary
 * - Data yang harus di tulis dengan awalan 'lateinit var'
 *
 * contoh:
 * var umur = 25
 *  
 * 2. Data yang tidak bisa diubah (immutable)
 * - Data yang harus di tulis dengan awalan 'val' -> value
 * - Data yang harus di tulis dengan awalan 'const val'
 *
 * contoh
 * val nama = "Ipil"
 * val TEMPAT_LAHIR = "Sulawesi"
 */

const val TEMPAT_LAHIR = "Sulawesi"

fun main() {

  // Tahun 2026
  val nama = "Ipil"
  var umur = 25
  lateinit var graduate: String

  graduate = "Belum"

  println(umur)
  println(nama)
  println(graduate)

  // Tahun 2027
  umur = 26
  graduate = "Sudah"
  println(umur)
  println(graduate)

  println(TEMPAT_LAHIR)
}
