package integ

import org.apache.logging.log4j.kotlin.Logging
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import software.amazon.awssdk.services.iot.IotClient
import software.amazon.awssdk.services.iot.model.CreateThingRequest
import software.amazon.awssdk.services.iot.model.DeleteThingRequest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class TestCreateThingTest {
    companion object : Logging

    val iotClient = IotClient.create()

    val thingName = "thingName1"

    @Test @Order(1)
    fun `GIVEN first time WHEN create thing THEN call succeeds`() {
        val createThingRequest = CreateThingRequest.builder()
            .thingName(thingName)
            .build()
        val createThingResponse = iotClient.createThing(createThingRequest)
        logger.info("first create response: $createThingRequest")
    }

    @Test @Order(2)
    fun `GIVEN second time WHEN create thing THEN call succeeds`() {
        val createThingRequest = CreateThingRequest.builder()
            .thingName(thingName)
            .build()
        val createThingResponse = iotClient.createThing(createThingRequest)
        logger.info("second create response: $createThingRequest")
    }

    @Test @Order(3)
    fun `WHEN delete thing THEN succeed`() {
        val deleteThingRequest = DeleteThingRequest.builder()
            .thingName(thingName)
            .build()
        val deleteThingResponse = iotClient.deleteThing(deleteThingRequest)
        logger.info("delete response: $deleteThingResponse")
    }
}
