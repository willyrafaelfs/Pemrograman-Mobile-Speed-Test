package com.example.speedtest.data

import com.example.speedtest.model.PingUpdate
import com.example.speedtest.model.SpeedUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * ═══════════════════════════════════════════════════════════════
 *  SpeedTestManager.kt
 *  Repository / Data Layer: berisi seluruh logika pengukuran
 *  performa jaringan (Ping, Download, Upload).
 *
 *  Setiap pengukuran mengembalikan Flow<T> yang di-collect
 *  oleh ViewModel, sehingga UI bisa menampilkan progress
 *  secara real-time tanpa memblokir Main Thread.
 *
 *  Semua operasi berjalan di Dispatchers.IO.
 * ═══════════════════════════════════════════════════════════════
 *  Author: Kukuh Yudhistiro, S.Kom., M.Kom.
 * ═══════════════════════════════════════════════════════════════
 */
class SpeedTestManager {

    companion object {
        // ── Konfigurasi Ping ──────────────────────────────
        private const val PING_HOST = "google.com"
        private const val PING_COUNT = 3  // Jumlah paket ICMP yang dikirim

        // ── Konfigurasi Download ──────────────────────────
        // File dummy ~10MB dari Cloudflare Speed Test CDN
        private const val DOWNLOAD_URL =
            "https://speed.cloudflare.com/__down?bytes=10000000"

        // ── Konfigurasi Upload ────────────────────────────
        // Endpoint POST Cloudflare Speed Test
        private const val UPLOAD_URL =
            "https://speed.cloudflare.com/__up"
        private const val UPLOAD_SIZE = 5_000_000  // 5MB dummy payload

        // ── Buffer & Timing ───────────────────────────────
        private const val BUFFER_SIZE = 8_192       // 8KB read/write buffer
        private const val EMIT_INTERVAL_MS = 150L   // Interval emit progress (ms)
    }

    // ╔═══════════════════════════════════════════════════════╗
    // ║  1. PING MEASUREMENT                                  ║
    // ║  Mengukur Round-Trip Time (RTT) ke google.com          ║
    // ╚═══════════════════════════════════════════════════════╝

    /**
     * Mengukur latency (ping) ke [PING_HOST] menggunakan ICMP ping.
     *
     * Mekanisme:
     * 1. Eksekusi command `ping -c 3 google.com` via Runtime
     * 2. Parse setiap baris output untuk mencari "time=XX.X ms"
     * 3. Hitung rata-rata dari semua RTT yang berhasil di-parse
     *
     * Contoh output `ping` yang di-parse:
     * ```
     * 64 bytes from 142.250.80.46: icmp_seq=1 ttl=116 time=10.1 ms
     * 64 bytes from 142.250.80.46: icmp_seq=2 ttl=116 time=11.2 ms
     * 64 bytes from 142.250.80.46: icmp_seq=3 ttl=116 time=9.8 ms
     * ---
     * rtt min/avg/max/mdev = 9.800/10.367/11.200/0.585 ms
     * ```
     *
     * Regex `time=(\d+\.?\d*)\s*ms` menangkap nilai setelah "time="
     * dan sebelum "ms". Contoh match: "time=10.1 ms" → group[1] = "10.1"
     *
     * @return Flow<PingUpdate> — stream update ping ke ViewModel
     */
    fun measurePing(): Flow<PingUpdate> = flow {
        emit(PingUpdate.Started)

        try {
            // Jalankan perintah ping via subprocess
            // -c 3 = kirim 3 paket ICMP echo request
            val process = Runtime.getRuntime()
                .exec("ping -c $PING_COUNT $PING_HOST")

            val reader = BufferedReader(
                InputStreamReader(process.inputStream)
            )

            // Kumpulkan semua nilai RTT yang berhasil diparsing
            val rttValues = mutableListOf<Double>()

            // Regex untuk mengekstrak nilai waktu dari output ping
            // Cocok dengan pola: "time=10.1 ms" atau "time=9 ms"
            val timeRegex = Regex("""time=(\d+\.?\d*)\s*ms""")

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val match = timeRegex.find(line!!)
                if (match != null) {
                    // group[1] berisi angka RTT, contoh: "10.1"
                    val rtt = match.groupValues[1].toDouble()
                    rttValues.add(rtt)

                    // Emit setiap RTT individual agar UI bisa menunjukkan progress
                    emit(PingUpdate.Progress(rtt))
                }
            }

            // Tunggu proses selesai
            process.waitFor()

            if (rttValues.isNotEmpty()) {
                // ── Kalkulasi Rata-rata ───────────────────────
                // avgPing = jumlah_semua_rtt / banyak_rtt
                // Contoh: (10.1 + 11.2 + 9.8) / 3 = 10.37 ms
                val avgPing = rttValues.average()
                emit(PingUpdate.Completed(avgPing))
            } else {
                emit(PingUpdate.Error("Gagal membaca output ping"))
            }

        } catch (e: SecurityException) {
            // Beberapa device melarang eksekusi ping via Runtime
            // Fallback: gunakan HTTP HEAD request sebagai alternatif
            emit(PingUpdate.Error("Ping diblokir, coba metode HTTP: ${e.message}"))
        } catch (e: Exception) {
            emit(PingUpdate.Error(e.message ?: "Ping gagal"))
        }
    }.flowOn(Dispatchers.IO)

    // ╔═══════════════════════════════════════════════════════╗
    // ║  2. DOWNLOAD SPEED MEASUREMENT                        ║
    // ║  Mengukur kecepatan unduh dari server Cloudflare       ║
    // ╚═══════════════════════════════════════════════════════╝

    /**
     * Mengukur kecepatan download dengan mengunduh file dummy
     * dari [DOWNLOAD_URL] dan menghitung throughput real-time.
     *
     * ── Rumus Kalkulasi Kecepatan ─────────────────────────
     *
     *   elapsedSeconds = (currentNanoTime - startNanoTime) / 1.000.000.000
     *
     *   speedBytesPerSec = totalBytesRead / elapsedSeconds
     *
     *   speedMbps = (totalBytesRead × 8) / (elapsedSeconds × 1.000.000)
     *                ↑ bytes→bits          ↑ bits→megabits
     *
     * Kenapa × 8?
     *   1 byte = 8 bits. Kecepatan internet diukur dalam
     *   bits per second (bps), bukan bytes per second (Bps).
     *
     * Kenapa ÷ 1.000.000?
     *   1 Megabit = 1.000.000 bits (standar SI, bukan 1.048.576)
     *
     * @return Flow<SpeedUpdate> — stream kecepatan download real-time
     */
    fun measureDownload(): Flow<SpeedUpdate> = flow {
        emit(SpeedUpdate.Started)

        try {
            val url = URL(DOWNLOAD_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000   // 15 detik timeout koneksi
                readTimeout = 60_000      // 60 detik timeout baca
                setRequestProperty("User-Agent", "AndroidSpeedTest/1.0")
                // Cloudflare memerlukan header ini untuk respons yang benar
                setRequestProperty("Accept", "*/*")
            }

            connection.connect()

            val inputStream = connection.inputStream
            val buffer = ByteArray(BUFFER_SIZE)  // Buffer baca 8KB
            var totalBytesRead = 0L
            val startTime = System.nanoTime()
            var lastEmitTime = startTime
            var bytesRead: Int

            // Baca stream secara chunked (per 8KB)
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                totalBytesRead += bytesRead

                val currentTime = System.nanoTime()
                val timeSinceLastEmit =
                    (currentTime - lastEmitTime) / 1_000_000  // Konversi ke ms

                // Emit progress setiap EMIT_INTERVAL_MS (150ms)
                // agar UI tidak kebanjiran update dan tetap smooth
                if (timeSinceLastEmit >= EMIT_INTERVAL_MS) {
                    val elapsedSeconds =
                        (currentTime - startTime) / 1_000_000_000.0

                    if (elapsedSeconds > 0) {
                        // ── Kalkulasi kecepatan real-time ────────
                        // Formula: (totalBytes × 8) / (detik × 1.000.000)
                        // Contoh: (2.500.000 bytes × 8) / (1.5s × 1.000.000)
                        //       = 20.000.000 / 1.500.000
                        //       = 13.33 Mbps
                        val speedMbps =
                            (totalBytesRead * 8.0) / (elapsedSeconds * 1_000_000.0)

                        emit(SpeedUpdate.Progress(speedMbps, totalBytesRead))
                    }
                    lastEmitTime = currentTime
                }
            }

            inputStream.close()
            connection.disconnect()

            // ── Kalkulasi kecepatan rata-rata akhir ──────────
            val totalElapsedSec =
                (System.nanoTime() - startTime) / 1_000_000_000.0
            val finalSpeedMbps = if (totalElapsedSec > 0) {
                (totalBytesRead * 8.0) / (totalElapsedSec * 1_000_000.0)
            } else 0.0

            emit(SpeedUpdate.Completed(finalSpeedMbps, totalBytesRead))

        } catch (e: Exception) {
            emit(SpeedUpdate.Error(e.message ?: "Download test gagal"))
        }
    }.flowOn(Dispatchers.IO)

    // ╔═══════════════════════════════════════════════════════╗
    // ║  3. UPLOAD SPEED MEASUREMENT                          ║
    // ║  Mengukur kecepatan unggah ke server Cloudflare        ║
    // ╚═══════════════════════════════════════════════════════╝

    /**
     * Mengukur kecepatan upload dengan mengirim dummy byte array
     * via HTTP POST ke [UPLOAD_URL].
     *
     * Mekanisme:
     * 1. Buat array byte dummy berukuran [UPLOAD_SIZE] (5MB)
     * 2. Kirim secara chunked (per 8KB) ke server
     * 3. Hitung kecepatan pengiriman secara real-time
     *
     * ── Rumus Kalkulasi (sama dengan download) ───────────
     *   speedMbps = (totalBytesSent × 8) / (elapsedSeconds × 1.000.000)
     *
     * Catatan: Kecepatan upload biasanya lebih lambat dari download
     * karena ISP mengalokasikan bandwidth asimetris (ADSL/fiber).
     *
     * @return Flow<SpeedUpdate> — stream kecepatan upload real-time
     */
    fun measureUpload(): Flow<SpeedUpdate> = flow {
        emit(SpeedUpdate.Started)

        try {
            val url = URL(UPLOAD_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true                          // Izinkan kirim data
                connectTimeout = 15_000
                readTimeout = 60_000
                setRequestProperty("Content-Type", "application/octet-stream")
                setRequestProperty("Content-Length", UPLOAD_SIZE.toString())
                // Disable buffering agar bytes langsung dikirim
                // tanpa menunggu seluruh payload selesai ditulis
                setFixedLengthStreamingMode(UPLOAD_SIZE)
            }

            connection.connect()

            // ── Buat dummy payload ──────────────────────────
            // Array byte berisi angka 0 (default). Isinya tidak penting,
            // yang penting adalah volume data yang dikirim untuk
            // mengukur throughput jaringan.
            val dummyData = ByteArray(BUFFER_SIZE)  // Chunk 8KB

            val outputStream = connection.outputStream
            var totalBytesSent = 0L
            val startTime = System.nanoTime()
            var lastEmitTime = startTime

            // Kirim data secara chunked
            while (totalBytesSent < UPLOAD_SIZE) {
                // Hitung sisa bytes yang perlu dikirim
                val remaining = UPLOAD_SIZE - totalBytesSent
                val chunkSize = minOf(remaining, BUFFER_SIZE.toLong()).toInt()

                outputStream.write(dummyData, 0, chunkSize)
                totalBytesSent += chunkSize

                val currentTime = System.nanoTime()
                val timeSinceLastEmit =
                    (currentTime - lastEmitTime) / 1_000_000

                if (timeSinceLastEmit >= EMIT_INTERVAL_MS) {
                    val elapsedSeconds =
                        (currentTime - startTime) / 1_000_000_000.0

                    if (elapsedSeconds > 0) {
                        val speedMbps =
                            (totalBytesSent * 8.0) / (elapsedSeconds * 1_000_000.0)

                        emit(SpeedUpdate.Progress(speedMbps, totalBytesSent))
                    }
                    lastEmitTime = currentTime
                }
            }

            // Flush dan tutup stream
            outputStream.flush()
            outputStream.close()

            // Baca response (diperlukan agar request selesai sempurna)
            val responseCode = connection.responseCode
            connection.disconnect()

            // ── Kalkulasi kecepatan rata-rata akhir ──────────
            val totalElapsedSec =
                (System.nanoTime() - startTime) / 1_000_000_000.0
            val finalSpeedMbps = if (totalElapsedSec > 0) {
                (totalBytesSent * 8.0) / (totalElapsedSec * 1_000_000.0)
            } else 0.0

            emit(SpeedUpdate.Completed(finalSpeedMbps, totalBytesSent))

        } catch (e: Exception) {
            emit(SpeedUpdate.Error(e.message ?: "Upload test gagal"))
        }
    }.flowOn(Dispatchers.IO)
}
