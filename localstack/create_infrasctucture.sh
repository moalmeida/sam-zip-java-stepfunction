#!/bin/sh

API_NAME=api
REGION=us-east-1
STAGE=local

echo "########### Creating profile ###########"

aws configure set aws_access_key_id localstack --profile localstack
aws configure set aws_secret_access_key localstack --profile localstack
aws configure set region us-east-1  --profile localstack

echo "########### Creating API ###########"

awslocal apigateway create-rest-api --profile localstack \
    --region ${REGION} \
    --name ${API_NAME}

API_ID=$(awslocal apigateway get-rest-apis  --profile localstack --query "items[?name==\`${API_NAME}\`].id" --output text --region ${REGION})

PARENT_RESOURCE_ID=$(awslocal apigateway get-resources  --profile localstack --rest-api-id ${API_ID} --query 'items[?path==`/`].id' --output text --region ${REGION})

echo "########### Creating Lambda CheckRejectedFunction ###########"

(cd app/.aws-sam/build/CheckRejectedFunction; zip -r ../CheckRejectedFunction.zip .)

awslocal lambda create-function --profile localstack \
    --function-name=CheckRejectedFunction \
    --runtime="java11" \
    --role=arn:aws:iam::012345678901:role/localstack \
    --memory-size 1538 \
    --handler=com.itau.fj1.backoffice.App::handleRequest \
    --zip-file fileb://app/.aws-sam/build/CheckRejectedFunction.zip

LAMBDA_CHECK_REJECTED_ARN=arn:aws:lambda:us-east-1:000000000000:function:CheckRejectedFunction

echo "########### Creating STANDARD StepFunction ###########"

awslocal stepfunctions create-state-machine --profile localstack  \
    --type STANDARD \
    --definition "{\
        \"StartAt\": \"CheckRejectedFunction\",\
        \"States\": {\
            \"CheckRejectedFunction\": {\
            \"Type\": \"Task\",\
            \"Resource\": \"arn:aws:lambda:us-east-1:000000000000:function:CheckRejectedFunction\",\
            \"End\": true\
            }\
        }\
    }" \
    --name "StandardStateMachine" \
    --region ${REGION} \
    --role-arn "arn:aws:iam::012345678901:role/localstack"

# echo "########### Executing STANDARD StepFunction ###########"
# awslocal stepfunctions start-execution --profile localstack \
#    --state-machine arn:aws:states:us-east-1:000000000000:stateMachine:StandardStateMachine \
#    --name Test123 \
#    --input "{\"ping\":\"pong\"}"
# sleep 5 
# awslocal stepfunctions describe-execution --profile localstack \
#   --execution-arn arn:aws:states:us-east-1:000000000000:execution:StandardStateMachine:us-east-1_Test123

echo "########### Creating Lambda StartFunction ###########"

(cd app/.aws-sam/build/StartFunction; zip -r ../StartFunction.zip .)

awslocal lambda create-function --profile localstack \
    --function-name=StartFunction \
    --runtime="java11" \
    --role=arn:aws:iam::012345678901:role/localstack \
    --memory-size 1538 \
    --handler=com.itau.fj1.backoffice.App::handleRequest \
    --zip-file fileb://app/.aws-sam/build/StartFunction.zip

LAMBDA_START_ARN=arn:aws:lambda:us-east-1:000000000000:function:StartFunction

echo "########### Creating API StartFunction ###########"

awslocal apigateway create-resource --profile localstack \
    --region ${REGION} \
    --rest-api-id ${API_ID} \
    --parent-id ${PARENT_RESOURCE_ID} \
    --path-part "start"

API_START_ID=$(awslocal apigateway get-resources --profile localstack --rest-api-id ${API_ID} --query "items[?path=='/start'].id" --output text --region ${REGION})

awslocal apigateway put-method --profile localstack  \
    --region ${REGION} \
    --rest-api-id ${API_ID} \
    --resource-id ${API_START_ID} \
    --http-method GET \
    --authorization-type "NONE"

awslocal apigateway put-integration --profile localstack  \
    --region ${REGION} \
    --rest-api-id ${API_ID} \
    --resource-id ${API_START_ID} \
    --http-method GET \
    --type AWS_PROXY \
    --integration-http-method POST \
    --uri arn:aws:apigateway:${REGION}:lambda:path/2015-03-31/functions/${LAMBDA_START_ARN}/invocations \
    --passthrough-behavior WHEN_NO_MATCH 

echo "########### Creating API Deployment ###########"

awslocal apigateway create-deployment --profile localstack  \
    --region ${REGION} \
    --rest-api-id ${API_ID} \
    --stage-name ${STAGE} 

echo "########### Executing API ###########"

ENDPOINT_START=http://localhost:4566/restapis/${API_ID}/${STAGE}/_user_request_/start

echo "API START available at: ${ENDPOINT_START}"

echo "Testing START GET:"
curl ${ENDPOINT_START}


