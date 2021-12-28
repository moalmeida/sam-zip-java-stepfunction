package com.itau.fj1.backoffice;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.DescribeExecutionRequest;
import com.amazonaws.services.stepfunctions.model.DescribeExecutionResult;
import com.amazonaws.services.stepfunctions.model.StartExecutionRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    final static String STANDARD_STATE_MACHINE = "arn:aws:states:us-east-1:000000000000:stateMachine:StandardStateMachine";
    final static String ENDPOINT_CONFIGURATION = "http://localhost:4566/";
    final static String REGION_CONFIGURATION = Regions.US_EAST_1.getName();

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);
        try {
            final String output = executeStateMachine();
            return response
                    .withStatusCode(200)
                    .withBody(output);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return response
                    .withBody("{}")
                    .withStatusCode(500);
        }
    }

    private static AWSStepFunctions getAWSStepFunctionsClient() {
        AwsClientBuilder.EndpointConfiguration endpointConfig = new AwsClientBuilder.EndpointConfiguration(ENDPOINT_CONFIGURATION, REGION_CONFIGURATION);
        return AWSStepFunctionsClientBuilder.standard()
                .withEndpointConfiguration(endpointConfig)
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("localstack", "localstack")))
                .build();
    }

    private String getExecutionResult(DescribeExecutionRequest request) {
        DescribeExecutionResult describeExecutionResult;
        do {
            describeExecutionResult = getAWSStepFunctionsClient().describeExecution(request);
        } while ("RUNNING".equals(describeExecutionResult.getStatus()));

        if ("SUCCEEDED".equals(describeExecutionResult.getStatus())) {
            return describeExecutionResult.getOutput();
        }
        return null;
    }

    private String executeStateMachine() throws ExecutionException, InterruptedException {
        var startExecution = getAWSStepFunctionsClient().startExecution(
                new StartExecutionRequest()
                        .withStateMachineArn(STANDARD_STATE_MACHINE)
                        .withInput("{\"ping\": \"pong\"}")
        );
        return getExecutionResult(
                new DescribeExecutionRequest()
                        .withExecutionArn(startExecution.getExecutionArn())
        );
    }

}
