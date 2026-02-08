package com.htmake.reader

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.http.*
import io.vertx.core.json.Json
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.core.net.ProxyOptions
import io.vertx.core.net.ProxyType
import com.htmake.reader.config.AppConfig
import org.springframework.beans.factory.annotation.Autowired
import mu.KotlinLogging
import com.htmake.reader.api.YueduApi

import com.htmake.reader.verticle.RestVerticle
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean
import javax.annotation.PostConstruct

private val logger = KotlinLogging.logger {}

@SpringBootApplication
@EnableScheduling
class ReaderApplication {

    @Autowired
    private lateinit var yueduApi: YueduApi

    companion object {
        val vertx by lazy { Vertx.vertx() }
        fun vertx() = vertx
    }

    @PostConstruct
    fun deployVerticle() {
        Json.mapper.apply {
            registerKotlinModule()
        }

        Json.prettyMapper.apply {
            registerKotlinModule()
        }

        Json.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        vertx().deployVerticle(yueduApi)
    }

    @Autowired
    private lateinit var appConfig: AppConfig
    
    @Bean
    fun webClient(): WebClient {
        val webClientOptions = WebClientOptions()
        webClientOptions.isTryUseCompression = true
        webClientOptions.logActivity = true
        webClientOptions.isFollowRedirects = true
        webClientOptions.isTrustAll = true
        
        // 配置代理
        if (appConfig.proxy && appConfig.proxyHost.isNotEmpty() && appConfig.proxyPort.isNotEmpty()) {
            try {
                val proxyOptions = ProxyOptions()
                proxyOptions.host = appConfig.proxyHost
                proxyOptions.port = appConfig.proxyPort.toInt()
                
                // 设置代理类型
                when (appConfig.proxyType.uppercase()) {
                    "HTTP" -> proxyOptions.type = ProxyType.HTTP
                    "SOCKS", "SOCKS5" -> proxyOptions.type = ProxyType.SOCKS5
                    "SOCKS4" -> proxyOptions.type = ProxyType.SOCKS4
                    else -> proxyOptions.type = ProxyType.HTTP
                }
                
                // 设置代理认证（如果有）
                if (appConfig.proxyUsername.isNotEmpty()) {
                    proxyOptions.username = appConfig.proxyUsername
                }
                if (appConfig.proxyPassword.isNotEmpty()) {
                    proxyOptions.password = appConfig.proxyPassword
                }
                
                webClientOptions.proxyOptions = proxyOptions
                logger.info("代理已启用: {}://{}:{}", appConfig.proxyType, appConfig.proxyHost, appConfig.proxyPort)
            } catch (e: Exception) {
                logger.error("代理配置错误: {}", e.message)
            }
        } else {
            logger.info("代理未启用")
        }

        val httpClient = vertx().createHttpClient(HttpClientOptions().setTrustAll(true))

//        val webClient = WebClient.wrap(HttpClient(delegateHttpClient), webClientOptions)
        val webClient = WebClient.wrap(httpClient, webClientOptions)

        return webClient
    }

}

fun main(args: Array<String>) {
    SpringApplication.run(ReaderApplication::class.java, *args)
}




