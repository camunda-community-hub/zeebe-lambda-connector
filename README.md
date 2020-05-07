# zeebe-lambda-worker

A Zeebe worker to invoke AWS Lambdas (Serverless functions), allowing to orchestrate functions. It uses the AWS SDK to connect to Lambda.

> Requirements: Java 11

# Usage

## BPMN Service Task

Example service task in BPMN:

```xml
<bpmn:serviceTask id="BookHotel" name="Book Hotel">
  <bpmn:extensionElements>
    <zeebe:taskDefinition type="lambda" />
    <zeebe:taskHeaders>
      <zeebe:header key="functionName" value="trip-booking-functions-dev-book-hotel" />
      <zeebe:header key="body" value="{&#34;body&#34;:  &#34;{{{variablesJsonEscaped}}}&#34;  }" />
      <zeebe:header key="resultName" value="bookHotelResult" />
    </zeebe:taskHeaders>
  </bpmn:extensionElements>
</bpmn:serviceTask>

```

* the worker is registered for the type `lambda`
* required headers:
  * `functionName` - the function name of the Lambda to invoke

* Optional Headers
  * `body` - the payload to send over to the function, can be constructed using placeholders (as explained below)
  * `resultName` - the variable the result shall be written to (another variable `resultNameStatusCode` and `resultNameJsonString` is written additionally, see example below)
  * `functionErrorCode` - If the function results in an error (status code != 200), you can define a [BPMN Error]() that is raised in this case, which allows to react to failures in your BPMN model (TODO: Think about if the statusCode itself should end up in the BPMN error code, then you could react differently to different errors)

## Placeholders

> Please note that the current way of handling placeholders is subject to change in the future, especially with https://github.com/zeebe-io/zeebe/issues/3417 and the new FEEL expressions.

You can use placeholders in the form of `{{PLACEHOLDER}}` at all places, they will be replaced by 

* custom headers from the BPMN model
* Workflow variables
* Configuration Variables from URL (see below)
* `workflowInstanceKey` or `jobKey`

[Mustache](https://github.com/spullara/mustache.java) is used for replacing the placeholders, refer to their docs to check possibilities.

If you want to avoid that the resulting string gets quoted, you have to use triple quotes: `{{{PLACEHOLDER}}}`. This is handy to e.g. pass all variables into the request in the body attribute (which can be the case if your functions are also called via API Gateway):

```
{"body":  "{{{variablesJsonEscaped}}}" }
```

Example:

```xml
<bpmn:serviceTask id="http-get" name="stargazers check">
  <bpmn:extensionElements>
    <zeebe:taskDefinition type="lambda" />
    <zeebe:taskHeaders>
      <zeebe:header key="functionName" value="{{FUNCTION_BASE}}-book-hotel" />     
    </zeebe:taskHeaders>
  </bpmn:extensionElements>
</bpmn:serviceTask>
```

`FUNCTION_BASE` could be configured by the configuration variables from the URL or environment properties.

You can use

* Single Variables by name
* `variables` uses all variable as Json object
* `variablesJsonEscaped` includes all variables as Json String, escaped, to be used as String attribute, e.g. used to call a function that is also called via API Gateway) - use {{{ to prevent the templating to add quotes:

```
{"body":  "{{{variablesJsonEscaped}}}"  }
```




# Install

## JAR 

* Download the [JAR file](https://github.com/zeebe-io/zeebe-http-worker/releases) 
* Execute the JAR via

    `java -jar target/zeebe-lambda-worker-{VERSION}.jar`

## Docker

    `docker run camunda/zeebe-lambda-worker`

## Readiness probes

You can check health of the worker:

  http://localhost:8080/actuator/health

This uses the Spring Actuator, so other metrics are available as well

## Configuration of Zeebe Connection

The connection to the broker Zeebe can be changed by setting the environment variables 


* `ZEEBE_CLIENT_BROKER_CONTACTPOINT` (default: `127.0.0.1:26500`).
* `ZEEBE_CLIENT_SECURITY_PLAINTEXT` (default: true).

or if you want to connect to Camunda Cloud: 

* `ZEEBE_CLIENT_CLOUD_CLUSTERID`
* `ZEEBE_CLIENT_CLOUD_CLIENTID`
* `ZEEBE_CLIENT_CLOUD_CLIENTSECRET`

And you could adjust the topic or worker name itself:

* `ZEEBE_CLIENT_WORKER_DEFAULTNAME` (default `lambda-worker`)
* `ZEEBE_CLIENT_WORKER_DEFAULTTYPE` (default `lambda`)


This worker uses [Spring Zeebe]( https://github.com/zeebe-io/spring-zeebe/) underneath, so all configuration options available there are also available here.

## Configuration Variables from URL

You can load additional configuration values used to substitute placeholders. Therefor the worker will query an HTTP endpoint and expects a JSON back:

```
[
  {
    "key": "someValue",
    "value": 42
  },
  {
    "key": "anotherValue",
    "value": 42
  }
]
```

To load additional config variables from an URL set these environment variables:

* `ENV_VARS_URL` (e.g. `http://someUrl/config`, default: null)
* `ENV_VARS_RELOAD_RATE` (default `15000`)
* `ENV_VARS_M2M_BASE_URL`
* `ENV_VARS_M2M_CLIENT_ID`
* `ENV_VARS_M2M_CLIENT_SECRET`
* `ENV_VARS_M2M_AUDIENCE`


## Code of Conduct

This project adheres to the Contributor Covenant [Code of
Conduct](/CODE_OF_CONDUCT.md). By participating, you are expected to uphold
this code. Please report unacceptable behavior to code-of-conduct@zeebe.io.
