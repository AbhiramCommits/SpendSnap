package com.spendsnap.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface SpendSnapApi {
    @POST("expenses")
    suspend fun createExpense(
        @Header("Idempotency-Key") clientUuid: String,
        @Body expense: ExpenseDto,
    ): ExpenseDto

    @GET("expenses")
    suspend fun getExpenses(): ExpenseListResponse
}
