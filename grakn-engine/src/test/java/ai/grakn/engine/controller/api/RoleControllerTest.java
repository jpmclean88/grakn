/*
 * Grakn - A Distributed Semantic Database
 * Copyright (C) 2016  Grakn Labs Limited
 *
 * Grakn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grakn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grakn. If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package ai.grakn.engine.controller.api;

import ai.grakn.GraknTx;
import ai.grakn.GraknTxType;
import ai.grakn.engine.controller.SparkContext;
import ai.grakn.engine.factory.EngineGraknTxFactory;
import ai.grakn.test.SampleKBContext;
import ai.grakn.test.kbs.MovieKB;
import ai.grakn.util.SampleKBLoader;
import com.jayway.restassured.response.Response;
import mjson.Json;
import org.apache.commons.httpclient.HttpStatus;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static ai.grakn.util.REST.Request.CONCEPT_ID_JSON_FIELD;
import static ai.grakn.util.REST.Request.KEYSPACE;
import static ai.grakn.util.REST.Request.LABEL_JSON_FIELD;
import static ai.grakn.util.REST.Request.ROLE_OBJECT_JSON_FIELD;
import static ai.grakn.util.REST.WebPath.Api.ROLE;
import static com.jayway.restassured.RestAssured.with;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>
 *     Endpoint tests for Graph API
 * </p>
 *
 * @author Ganeshwara Herawan Hananda
 */

public class RoleControllerTest {
    private static GraknTx mockTx;
    private static EngineGraknTxFactory mockFactory = mock(EngineGraknTxFactory.class);

    @ClassRule
    public static SampleKBContext sampleKB = SampleKBContext.preLoad(MovieKB.get());

    @ClassRule
    public static SparkContext sparkContext = SparkContext.withControllers(spark -> {
        new RoleController(mockFactory, spark);
    });

    @Before
    public void setupMock(){
        mockTx = mock(GraknTx.class, RETURNS_DEEP_STUBS);

        when(mockTx.getKeyspace()).thenReturn(SampleKBLoader.randomKeyspace());

        when(mockTx.putRole(anyString())).thenAnswer(invocation ->
            sampleKB.tx().putRole((String) invocation.getArgument(0)));
        when(mockTx.getRole(anyString())).thenAnswer(invocation ->
            sampleKB.tx().getRole(invocation.getArgument(0)));

        when(mockFactory.tx(mockTx.getKeyspace(), GraknTxType.READ)).thenReturn(mockTx);
        when(mockFactory.tx(mockTx.getKeyspace(), GraknTxType.WRITE)).thenReturn(mockTx);
    }

    @Test
    public void getRoleFromMovieGraphShouldExecuteSuccessfully() {
        String productionWithCluster = "production-with-cluster";

        Response response = with()
            .queryParam(KEYSPACE, mockTx.getKeyspace().getValue())
            .get(ROLE + "/" + productionWithCluster);

        Json responseBody = Json.read(response.body().asString());

        assertThat(response.statusCode(), equalTo(HttpStatus.SC_OK));
        assertThat(responseBody.at(ROLE_OBJECT_JSON_FIELD).at(CONCEPT_ID_JSON_FIELD).asString(), notNullValue());
        assertThat(responseBody.at(ROLE_OBJECT_JSON_FIELD).at(LABEL_JSON_FIELD).asString(), equalTo(productionWithCluster));
    }
}
