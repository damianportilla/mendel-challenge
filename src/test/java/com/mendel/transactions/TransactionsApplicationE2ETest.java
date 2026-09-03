package com.mendel.transactions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TransactionsApplicationE2ETest {

  @Container
  static final LocalStackContainer localstack =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
          .withServices(Service.DYNAMODB);

  @DynamicPropertySource
  static void awsProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "AWS_ENDPOINT_URL", () -> localstack.getEndpointOverride(Service.DYNAMODB).toString());
    registry.add("AWS_REGION", localstack::getRegion);
    registry.add("AWS_ACCESS_KEY_ID", localstack::getAccessKey);
    registry.add("AWS_SECRET_ACCESS_KEY", localstack::getSecretKey);
  }

  @Autowired private MockMvc mockMvc;

  @Test
  void shouldSumTheDescendantSubtreeNotTheParent() throws Exception {
    putTransaction(10, "{\"amount\": 5000, \"type\": \"cars\"}");
    putTransaction(11, "{\"amount\": 10000, \"type\": \"shopping\", \"parent_id\": 10}");
    putTransaction(12, "{\"amount\": 5000, \"type\": \"shopping\", \"parent_id\": 11}");

    mockMvc
        .perform(get("/transactions/types/cars"))
        .andExpect(status().isOk())
        .andExpect(content().json("[10]"));

    mockMvc
        .perform(get("/transactions/sum/10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sum").value(20000.0));

    mockMvc
        .perform(get("/transactions/sum/11"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sum").value(15000.0));
  }

  @Test
  void shouldAcceptOrphanParentIdAndSumOnlyItself() throws Exception {
    putTransaction(20, "{\"amount\": 300, \"type\": \"misc\", \"parent_id\": 999999}");

    mockMvc
        .perform(get("/transactions/sum/20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sum").value(300.0));

    mockMvc.perform(get("/transactions/sum/999999")).andExpect(status().isNotFound());
  }

  @Test
  void shouldSumEachNodeOnceWithoutHangingOnCyclicReference() throws Exception {
    putTransaction(30, "{\"amount\": 100, \"type\": \"a\", \"parent_id\": 31}");
    putTransaction(31, "{\"amount\": 50, \"type\": \"b\", \"parent_id\": 30}");

    mockMvc
        .perform(get("/transactions/sum/30"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sum").value(150.0));
  }

  private void putTransaction(long id, String body) throws Exception {
    mockMvc
        .perform(put("/transactions/" + id).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk());
  }
}
