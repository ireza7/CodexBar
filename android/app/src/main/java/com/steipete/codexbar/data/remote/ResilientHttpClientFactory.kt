package com.steipete.codexbar.data.remote

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Builds resilient OkHttpClient instances that bypass DNS pollution / censorship
 * using DNS-over-HTTPS (Cloudflare & Google DoH) as a fallback to system DNS.
 */
object ResilientHttpClientFactory {

    private val dohDns: Dns by lazy {
        val bootstrapClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val cloudflareDns = DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url("https://1.1.1.1/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                InetAddress.getByName("1.1.1.1"),
                InetAddress.getByName("1.0.0.1")
            )
            .build()

        val googleDns = DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url("https://8.8.8.8/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                InetAddress.getByName("8.8.8.8"),
                InetAddress.getByName("8.8.4.4")
            )
            .build()

        object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                // 1. Try standard system DNS
                try {
                    val systemResults = Dns.SYSTEM.lookup(hostname)
                    if (systemResults.isNotEmpty()) return systemResults
                } catch (_: Exception) {}

                // 2. Try Cloudflare DNS-over-HTTPS
                try {
                    val cfResults = cloudflareDns.lookup(hostname)
                    if (cfResults.isNotEmpty()) return cfResults
                } catch (_: Exception) {}

                // 3. Try Google DNS-over-HTTPS
                try {
                    val googleResults = googleDns.lookup(hostname)
                    if (googleResults.isNotEmpty()) return googleResults
                } catch (_: Exception) {}

                throw UnknownHostException("Unable to resolve host \"$hostname\" via System, Cloudflare DoH, or Google DoH")
            }
        }
    }

    fun createClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .dns(dohDns)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
