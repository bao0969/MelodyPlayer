package com.example.melodyplayer.otp

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object OtpApi {

    private const val TAG = "OtpApi"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // QUAN TRỌNG: Thay đổi IP này thành:
    // - 10.0.2.2:3000 nếu dùng Android Emulator
    // - IP máy tính thật trên mạng LAN nếu dùng thiết bị thật
    // - URL domain thật nếu đã deploy lên server
    private const val BASE_URL = "http://backend.ngocanh648.id.vn:3000"

    fun sendOtp(email: String): Boolean {
        return try {
            Log.d(TAG, "========== BẮT ĐẦU GỬI OTP ==========")
            Log.d(TAG, "Email: $email")
            Log.d(TAG, "URL: $BASE_URL/send-otp")

            val json = JSONObject().put("email", email).toString()
            Log.d(TAG, "JSON Request: $json")

            val body = json.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/send-otp")
                .post(body)
                .build()

            Log.d(TAG, "Đang gửi request...")

            client.newCall(request).execute().use { res ->
                Log.d(TAG, "Response code: ${res.code}")
                Log.d(TAG, "Response success: ${res.isSuccessful}")

                if (!res.isSuccessful) {
                    Log.e(TAG, "Request thất bại với code: ${res.code}")
                    Log.e(TAG, "Response body: ${res.body?.string()}")
                    return false
                }

                val responseBody = res.body?.string()
                Log.d(TAG, "Response body: $responseBody")

                if (responseBody.isNullOrEmpty()) {
                    Log.e(TAG, "Response body rỗng")
                    return false
                }

                val jsonObj = JSONObject(responseBody)
                val success = jsonObj.getBoolean("success")

                Log.d(TAG, "Success: $success")
                Log.d(TAG, "========== KẾT THÚC GỬI OTP ==========")

                success
            }
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "❌ LỖI: Không tìm thấy host - Kiểm tra IP/URL server", e)
            Log.e(TAG, "Đảm bảo server đang chạy và IP đúng")
            false
        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "❌ LỖI: Không kết nối được - Server có đang chạy không?", e)
            Log.e(TAG, "URL: $BASE_URL/send-otp")
            false
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "❌ LỖI: Timeout - Server phản hồi quá lâu", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "❌ LỖI không xác định khi gửi OTP", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Message: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun verifyOtp(email: String, otp: String): Boolean {
        return try {
            Log.d(TAG, "========== BẮT ĐẦU XÁC THỰC OTP ==========")
            Log.d(TAG, "Email: $email")
            Log.d(TAG, "OTP: $otp")
            Log.d(TAG, "URL: $BASE_URL/verify-otp")

            val json = JSONObject()
                .put("email", email)
                .put("otp", otp)
                .toString()

            Log.d(TAG, "JSON Request: $json")

            val body = json.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$BASE_URL/verify-otp")
                .post(body)
                .build()

            Log.d(TAG, "Đang gửi request...")

            client.newCall(request).execute().use { res ->
                Log.d(TAG, "Response code: ${res.code}")
                Log.d(TAG, "Response success: ${res.isSuccessful}")

                if (!res.isSuccessful) {
                    Log.e(TAG, "Request thất bại với code: ${res.code}")
                    Log.e(TAG, "Response body: ${res.body?.string()}")
                    return false
                }

                val responseBody = res.body?.string()
                Log.d(TAG, "Response body: $responseBody")

                if (responseBody.isNullOrEmpty()) {
                    Log.e(TAG, "Response body rỗng")
                    return false
                }

                val jsonObj = JSONObject(responseBody)
                val success = jsonObj.getBoolean("success")

                Log.d(TAG, "Success: $success")
                Log.d(TAG, "========== KẾT THÚC XÁC THỰC OTP ==========")

                success
            }
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "❌ LỖI: Không tìm thấy host khi verify OTP", e)
            false
        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "❌ LỖI: Không kết nối được khi verify OTP", e)
            false
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "❌ LỖI: Timeout khi verify OTP", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "❌ LỖI không xác định khi verify OTP", e)
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Message: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}