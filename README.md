# zeebe-lambda-worker
POC for a worker connecting to AWS Lambda for serverless function orchestration


# Expressions that can be used

* Single Variables by name
* variables (All variables as Json Object)
* variablesJsonEscaped (All variables as Json String, escaped, to be used as String attribute, e.g. used to call a function that is also called via API Gateway) - use {{{ to prevent the templating to add quotes:

```
{"body":  "{{{variablesJsonEscaped}}}"  }
```
