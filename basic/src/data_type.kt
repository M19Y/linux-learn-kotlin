/* Tipe Data
 *
 * Tipe data adalah bentuk pengelompokan yang memberitahu komputer jenis
 * nilai apa yang dapat di simpan dan diproses oleh suatu variable
 *
 * Contoh penggunaan:
 * val nama: String = "Ipil"
 * val hobi = "Bola" // type inference (Kotlin menebak tipe data)
 *
 * Ada banyak sekali jenis tipe data di Kotlin diantaranya:
 *
 * 1. Tipe data nomor
 *    - bilangan bulan
 *      Byte      (8-bit)     (-128 sd 127)
 *      Short     (16-bit)    (-32,768 sd 32,767)
 *      Int       (32-bit)    (-2,147,483,648 sd 2,147,483,647) dipilih secara otomatis jika type data tidak di sebutkan
 *      Long      (64-bit)    (-9,223,372,036,854,775,808 sd 9,223,372,036,854,775,807)
 *
 *    - bilangan desimal
 *      Float     (32-bit)
 *      Double    (64-bit) dipilih secara otomatis jika type data tidak di sebutkan
 *
 * 2. Tipe data teks dan karakter
 *    String      (n)       ditulis di dalam "" -> "Ipil"
 *    Char        (1)       ditulis di dalam '' -> 'a'
 *
 * 3. Tipe data kebenaran
 *    Boolean     (true/false)
 * 4. Tipe data larik
 *    arrayOf     (kumpulan data)
 *
 */
fun main() {
   val umur: Byte = 18
   val warga: Short = 1500
   val uang: Int = 1_000_000_000
   val bb: Float = 50.21F
   val kekuatan: Double = 1000.45
   val nama: String = "Ipil"
   val inisial: Char = 'I' 
   val lulus: Boolean = false
   val hobi = arrayOf("Rumpi", "Menjemur")

   println("umur: $umur, bertipe: ${umur.javaClass.simpleName}")
   println("warga: $warga, bertipe: ${warga.javaClass.simpleName}")
   println("uang: $uang, bertipe: ${uang.javaClass.simpleName}")
   println("bb: $bb, bertipe: ${bb.javaClass.simpleName}")
   println("kekuatan: $kekuatan, bertipe: ${kekuatan.javaClass.simpleName}")
   println("nama: $nama, bertipe: ${nama.javaClass.simpleName}")
   println("inisial: $inisial, bertipe: ${inisial.javaClass.simpleName}")
   println("lulus: $lulus, bertipe: ${lulus.javaClass.simpleName}")
}
