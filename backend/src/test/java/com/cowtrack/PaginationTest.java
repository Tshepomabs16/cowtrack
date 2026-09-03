package com.cowtrack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Paging introduces failure modes an unpaged list could not have: an animal that
 * falls between two pages and is never shown, a total that counts rows the caller
 * may not see, a search that only looks at the page in front of it, and a size
 * parameter that quietly undoes the paging altogether.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaginationTest {

    private static final int HERD_SIZE = 12;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String tokenA;
    private String tokenB;
    private final List<String> namesA = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        tokenA = register("Alpha");
        tokenB = register("Beta");

        namesA.clear();
        for (int i = 0; i < HERD_SIZE; i++) {
            namesA.add(createCow(tokenA, "Alpha Cow " + i, "PAGE-A-" + System.nanoTime() + "-" + i));
        }
    }

    @Test
    void aHerdLargerThanAPageReportsHowManyPagesThereAre() throws Exception {
        JsonNode page = cowPage(tokenA, "?page=0&size=5");

        assertThat(page.get("content").size()).isEqualTo(5);
        assertThat(page.get("totalItems").asLong()).isEqualTo(HERD_SIZE);
        assertThat(page.get("totalPages").asInt()).isEqualTo(3);
        assertThat(page.get("currentPage").asInt()).isZero();
        assertThat(page.get("hasNext").asBoolean()).isTrue();
        assertThat(page.get("hasPrevious").asBoolean()).isFalse();
    }

    /**
     * The property that matters to a farmer: paging through the herd shows every
     * animal, once. An unstable sort would drop some and repeat others without
     * either page looking wrong on its own.
     */
    @Test
    void everyAnimalAppearsExactlyOnceAcrossThePages() throws Exception {
        Set<Long> seen = new HashSet<>();
        int duplicates = 0;

        for (int pageNumber = 0; pageNumber < 3; pageNumber++) {
            JsonNode page = cowPage(tokenA, "?page=" + pageNumber + "&size=5");
            for (JsonNode cow : page.get("content")) {
                if (!seen.add(cow.get("cowId").asLong())) {
                    duplicates++;
                }
            }
        }

        assertThat(duplicates).as("no animal should appear on two pages").isZero();
        assertThat(seen).as("no animal should fall between pages").hasSize(HERD_SIZE);
    }

    @Test
    void theLastPageIsMarkedAsTheLast() throws Exception {
        JsonNode page = cowPage(tokenA, "?page=2&size=5");

        assertThat(page.get("content").size()).isEqualTo(2);
        assertThat(page.get("hasNext").asBoolean()).isFalse();
        assertThat(page.get("hasPrevious").asBoolean()).isTrue();
    }

    /**
     * Without a ceiling, a caller can ask for one enormous page and the endpoint
     * behaves exactly as it did before it was paginated.
     */
    @Test
    void anAbsurdPageSizeIsCapped() throws Exception {
        JsonNode page = cowPage(tokenA, "?page=0&size=100000");

        assertThat(page.get("pageSize").asInt()).isLessThanOrEqualTo(200);
    }

    /**
     * The search has to run against the herd, not against the page. A farmer
     * looking for one animal should not have to be on the right page already.
     */
    @Test
    void searchLooksBeyondTheCurrentPage() throws Exception {
        // "Alpha Cow 11" sorts onto the last page by id, so a search applied
        // after fetching page 0 would not find it.
        JsonNode page = cowPage(tokenA, "?page=0&size=5&search=Cow 11");

        assertThat(page.get("totalItems").asLong()).isEqualTo(1);
        assertThat(page.get("content").get(0).get("name").asText()).isEqualTo("Alpha Cow 11");
    }

    @Test
    void searchMatchesTheEarTagAsWellAsTheName() throws Exception {
        String tag = objectMapper.readTree(
                        mockMvc.perform(get("/api/cows?page=0&size=1")
                                        .header("Authorization", "Bearer " + tokenA))
                                .andReturn().getResponse().getContentAsString())
                .get("data").get("content").get(0).get("tagId").asText();

        JsonNode page = cowPage(tokenA, "?search=" + tag);

        assertThat(page.get("totalItems").asLong()).isEqualTo(1);
        assertThat(page.get("content").get(0).get("tagId").asText()).isEqualTo(tag);
    }

    /**
     * Paging is a new way to walk a collection, so it is a new way to walk out of
     * your own farm. Farmer B asks for every page there could be.
     */
    @Test
    void anotherFarmCannotPageIntoThisOnesHerd() throws Exception {
        for (int pageNumber = 0; pageNumber < 4; pageNumber++) {
            JsonNode page = cowPage(tokenB, "?page=" + pageNumber + "&size=5");

            assertThat(page.get("content"))
                    .as("page %d should hold none of farmer A's cattle", pageNumber)
                    .isEmpty();
            assertThat(page.get("totalItems").asLong()).isZero();
        }
    }

    @Test
    void anotherFarmCannotSearchIntoThisOnesHerd() throws Exception {
        JsonNode page = cowPage(tokenB, "?search=Alpha Cow");

        assertThat(page.get("content")).isEmpty();
        assertThat(page.get("totalItems").asLong()).isZero();
    }

    @Test
    void thePickerListIsScopedToTheCallersFarm() throws Exception {
        MvcResult mine = mockMvc.perform(get("/api/cows/options")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult theirs = mockMvc.perform(get("/api/cows/options")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode options = objectMapper.readTree(mine.getResponse().getContentAsString()).get("data");
        assertThat(options.size()).isEqualTo(HERD_SIZE);
        assertThat(options.get(0).has("cowId")).isTrue();
        assertThat(options.get(0).has("tagId")).isTrue();

        assertThat(objectMapper.readTree(theirs.getResponse().getContentAsString()).get("data"))
                .as("the picker is a full list, so an unscoped one would expose the whole herd")
                .isEmpty();
    }

    /** The picker returns the whole herd, so it must not carry the heavy fields. */
    @Test
    void thePickerListStaysSlim() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/cows/options")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode first = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get(0);

        assertThat(first.has("temperature")).isFalse();
        assertThat(first.has("location")).isFalse();
        assertThat(first.has("status")).isFalse();
    }

    // -------------------------------------------------------------- helpers

    private JsonNode cowPage(String token, String query) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/cows" + query)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
    }

    private String register(String farm) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Farmer %s","email":"%s-%d@example.com","password":"pass1234",
                                 "role":"farmer","farmName":"%s"}"""
                                .formatted(farm, farm, System.nanoTime(), farm)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private String createCow(String token, String name, String tagId) throws Exception {
        mockMvc.perform(post("/api/cows")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagId":"%s","name":"%s","breed":"Nguni",
                                 "dateOfBirth":"2021-01-01"}""".formatted(tagId, name)))
                .andExpect(status().isCreated());
        return name;
    }
}
