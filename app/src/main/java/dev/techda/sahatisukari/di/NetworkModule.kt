package dev.techda.sahatisukari.di

import dev.techda.sahatisukari.BuildConfig
import dev.techda.sahatisukari.data.AuthApi
import dev.techda.sahatisukari.data.PatientApi
import dev.techda.sahatisukari.data.SessionAuthenticator
import dev.techda.sahatisukari.data.TokenStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import javax.inject.Named
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton fun json(): Json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Provides @Singleton @Named("bare")
    fun bareClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides @Singleton @Named("bareRetrofit")
    fun bareRetrofit(json: Json, @Named("bare") client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides @Singleton fun authApi(@Named("bareRetrofit") retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides @Singleton @Named("sessionRetrofit")
    fun sessionRetrofit(json: Json, @Named("bare") bare: OkHttpClient, authenticator: SessionAuthenticator, tokenStore: TokenStore): Retrofit {
        val client = bare.newBuilder()
            .addInterceptor { chain ->
                val token = kotlinx.coroutines.runBlocking { tokenStore.read() }
                val request = if (token == null) chain.request() else chain.request().newBuilder().header("Authorization", "Bearer ${token.accessToken}").build()
                chain.proceed(request)
            }
            .authenticator(authenticator)
            .build()
        return Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL).client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build()
    }

    @Provides @Singleton @Named("session") fun sessionAuthApi(@Named("sessionRetrofit") retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)
    @Provides @Singleton fun patientApi(@Named("sessionRetrofit") retrofit: Retrofit): PatientApi = retrofit.create(PatientApi::class.java)
}
