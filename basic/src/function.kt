/* Fungsi
 *
 * Fungsi adalah satu kumpulan instruksi yang bertujuan mempermudah
 * pekerjaan yang pasti dan berulang.
 *
 * contoh implementasi fungsi adalah pada rumus matematika
 * 1. Rumus menghitung keliling persegi panjang
 *    Keliling = 2 x (panjang + lebar)
 * 2. Rumus menghitung luas persegi panjang
 *    Luas = panjang x lebar
 *
 * Setiap fungsi harus memiliki:
 * - fungsi harus di awali dengan kata 'fun'
 * - nama fungsi harus harus menggunakan huruf kecil jika lebih dari
 *   satu kata maka harus menggunakan camel case. contoh "menghitungKeliling"
 * - fungsi harus memiliki kurung kurawal ()
 * - fungsi memiliki block, di tandai dengan kurung siku {}
 *
 * 1. Fungsi standar
 * Cara membuat fungsi sederhana di kotlin dengan mengikuti standar:
 *
 * contoh:
 * fun tampilkan() {
 *  println("Hallo") 
 * }
 *
 * 2. Fungsi dengan parameter
 * Cara membuat fungsi dengan parameter di kotlin dengan mengikuti standar:
 * - tujuan dari kurung kurawal di kotlin adalah menyimpan parameter
 * - kita harus menyebutkan nama parameter dan jenis data apa saja yang boleh dikirim
 *
 * contoh:
 * fun tampilkan(nama: String){
 *  println("Nama: $nama")
 * }
 *
 * 3. Fungsi dengan kembalian
 * Cara membuat fungsi dengan kembalian di koltin dengan mengikuti standar:
 * - mengisi parameter adalah satu hal yang optional
 * - setelah kurung kurawal kita bisa menyebutkan nilai apa yang harus di kembalikan 
 *   oleh fungsi tersebut
 * - menyebutkan nilai atau type data seperti ": Int"
 *
 * contoh:
 * fun menghitungKeliling(panjang: Int, lebar: Int): Int {
 *  return 2 * (panjang + lebar)
 * }
 *
 */

fun main(){

}
