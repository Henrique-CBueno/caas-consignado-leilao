package com.caas.proposal.infrastructure.outbox

import com.caas.events.ProposalCreatedEvent
import com.caas.proposal.infrastructure.web.CreateProposalRequest
import com.caas.proposal.infrastructure.web.CreateProposalResponse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.time.Duration
import java.util.Properties
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ProposalOutboxKafkaIntegrationTest {

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    private val objectMapper = ObjectMapper().registerKotlinModule()

    @Test
    fun `creating a proposal publishes a ProposalCreated event to Kafka`() {
        val tenantId = UUID.randomUUID()
        val request = CreateProposalRequest(borrowerId = "12345678900", requestedAmount = BigDecimal("5000.00"), termMonths = 24)
        val headers = HttpHeaders().apply { set("X-Tenant-Id", tenantId.toString()) }

        val created = restTemplate.exchange(
            "http://localhost:$port/proposals",
            HttpMethod.POST,
            HttpEntity(request, headers),
            CreateProposalResponse::class.java,
        ).body!!

        val consumerProps = Properties().apply {
            putAll(
                KafkaTestUtils.consumerProps(kafka.bootstrapServers, "test-group-${UUID.randomUUID()}", "true"),
            )
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        }
        val consumer = KafkaConsumer<String, String>(consumerProps)
        consumer.subscribe(listOf("proposal.created"))

        val deadline = System.currentTimeMillis() + Duration.ofSeconds(15).toMillis()
        var event: ProposalCreatedEvent? = null
        while (System.currentTimeMillis() < deadline && event == null) {
            val records = consumer.poll(Duration.ofMillis(500))
            for (record in records) {
                val candidate = objectMapper.readValue(record.value(), ProposalCreatedEvent::class.java)
                if (candidate.proposalId == created.id) {
                    event = candidate
                }
            }
        }
        consumer.close()

        assertThat(event).isNotNull
        assertThat(event!!.tenantId).isEqualTo(tenantId)
        assertThat(event.borrowerId).isEqualTo("12345678900")
        assertThat(event.requestedAmount).isEqualByComparingTo("5000.00")
        assertThat(event.termMonths).isEqualTo(24)
    }

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @Container
        @JvmStatic
        val kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1"))

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
            registry.add("app.outbox.relay-fixed-delay-ms") { "200" }
        }
    }
}
