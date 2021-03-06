package com.restbucks.ordering.feature;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.HttpClientConfig;
import com.restbucks.ordering.profile.ProfileConfiguration;
import io.tracee.Tracee;
import io.tracee.binding.httpcomponents.TraceeHttpRequestInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.io.IOException;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.HttpClientConfig.httpClientConfig;
import static com.jayway.restassured.http.ContentType.JSON;
import static com.jayway.restassured.path.json.JsonPath.from;
import static com.restbucks.ordering.rest.TestUtils.readFileAsString;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
        loader = AnnotationConfigContextLoader.class,
        classes = {
                ProfileConfiguration.class
        }
)
public class CashierShould {
    @Autowired
    private ProfileConfiguration profile;

    @Rule
    public final TestRule traceTestWatcher = new TestWatcher() {
        @Override
        protected void starting(Description d) {
            Tracee.getBackend().put("testCase", d.getDisplayName());
        }

        @Override
        protected void finished(Description d) {
            Tracee.getBackend().clear();
        }
    };


    @Before
    public void config_rest_assured() {
        RestAssured.baseURI = profile.getApplicationBaseUri();
        RestAssured.port = profile.getApplicationPort();
        RestAssured.config = RestAssured.config().
                httpClient(httpClientConfig().
                        httpClientFactory(new HttpClientConfig.HttpClientFactory() {

                            @Override
                            public HttpClient createHttpClient() {
                                //noinspection deprecation as only DefaultHttpClient is available here
                                DefaultHttpClient httpClient = new DefaultHttpClient();
                                httpClient.addRequestInterceptor(new TraceeHttpRequestInterceptor());
                                //httpClient.addResponseInterceptor(new TraceeHttpResponseInterceptor());
                                return httpClient;
                            }
                        }));
    }

    @Test
    public void acceptOrder() throws IOException {

        String placeOrderCommand = readFileAsString("classpath:feature/order.json");

        // @formatter:off
        String orderRepresentation = given()
            .contentType(JSON).content(placeOrderCommand)
        .when()
            .post("/order")
        .then()
            .log().everything()
            .assertThat().statusCode(SC_CREATED)
            .extract().body().asString();
        // @formatter:on

        double orderAmount = from(orderRepresentation).getDouble("cost");
        String paymentLinkHref = from(orderRepresentation).get("_links.payment.href");

        // @formatter:off
        String paymentRepresentation = given()
            .contentType(JSON).content("{\"amount\":" + orderAmount + "}")
        .when()
            .put(paymentLinkHref)
        .then()
            .log().everything()
            .assertThat().statusCode(SC_CREATED)
            .extract().body().asString();
        // @formatter:on

        String orderLinkHref = from(paymentRepresentation).get("_links.order.href");

        // @formatter:off
        orderRepresentation = given()
            .contentType(JSON)
        .when()
            .get(orderLinkHref)
        .then()
            .log().everything()
            .assertThat().statusCode(SC_OK)
            .extract().body().asString();
        // @formatter:on

        String preparationLinkHref = from(orderRepresentation).get("_links.order-in-preparation.href");

        // @formatter:off
        orderRepresentation = given()
            .contentType(JSON)
        .when()
            .put(preparationLinkHref)
        .then()
            .log().everything()
            .assertThat().statusCode(SC_OK)
            .extract().body().asString();
        // @formatter:on

        String preparedLinkHref = from(orderRepresentation).get("_links.order-in-preparation.href");

        // @formatter:off
        orderRepresentation = given()
            .contentType(JSON)
        .when()
            .delete(preparedLinkHref)
        .then()
            .log().everything()
            .assertThat().statusCode(SC_OK)
            .extract().body().asString();
        // @formatter:on

        String receiptLinkHref = from(orderRepresentation).get("_links.receipt.href");

        // @formatter:off
        given()
            .contentType(JSON)
        .when()
            .delete(receiptLinkHref)
        .then()
            .log().everything()
            .assertThat().statusCode(SC_OK)
            .extract().body().asString();
        // @formatter:on
    }
}
