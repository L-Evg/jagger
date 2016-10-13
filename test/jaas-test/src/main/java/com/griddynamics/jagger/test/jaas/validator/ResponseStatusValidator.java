package com.griddynamics.jagger.test.jaas.validator;

import com.griddynamics.jagger.coordinator.NodeContext;
import com.griddynamics.jagger.invoker.http.v2.JHttpResponse;

public class ResponseStatusValidator<Q, E> extends BaseHttpResponseValidator<Q, E> {
    public ResponseStatusValidator(String taskId, String sessionId, NodeContext kernelContext) {
        super(taskId, sessionId, kernelContext);
    }

    @Override
    public String getName() {
        return "ResponseStatusValidator";
    }

    /* Following method will be called after every successful invoke to validate result
     * @author Grid Dynamics
     *
     * @param query    - Query that was sent to endpoint
     * @param endpoint - Endpoint - service under test
     * @param result   - Result returned from endpoint
     * @param duration - Duration of invoke
     * */
    @Override
    public boolean validate(Q query, E endpoint, JHttpResponse result, long duration)  {
        int statusCode = result.getStatus().value();

        if (statusCode != getExpectedStatusCode()) { //TODO: Need range/RegExp.
            logResponseAsFailed(
                    query,
                    endpoint,
                    result,
                    String.format("Unexpected response status code. %d instead of %d", statusCode, getExpectedStatusCode()));
            return  false;
        }

        return true;
    }

    /**
     * By default expected status code is 200. Should be changed in child classes.
     * @return
     */
    protected int getExpectedStatusCode(){ return 200; }
}