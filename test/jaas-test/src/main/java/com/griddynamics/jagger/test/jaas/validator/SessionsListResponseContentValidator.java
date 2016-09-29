package com.griddynamics.jagger.test.jaas.validator;

import com.alibaba.fastjson.JSON;
import com.griddynamics.jagger.coordinator.NodeContext;
import com.griddynamics.jagger.engine.e1.services.data.service.SessionEntity;
import com.griddynamics.jagger.invoker.http.HttpResponse;
import com.griddynamics.jagger.test.jaas.util.TestContext;
import com.jayway.jsonpath.Filter;
import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import org.apache.http.client.methods.HttpRequestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;

/**
 * Validates response of /jaas/sessions.
 * Expected:
 * - list of sessions is of size 2 and greater;
 * - the list is no longer than the one available via DataService;
 * - the list contains no duplicates;
 * - a randomly picked records is the same as corresponding expected one.
 */
public class SessionsListResponseContentValidator<E> extends BaseHttpResponseValidator<HttpRequestBase, E> {
    private static final Logger log = LoggerFactory.getLogger(SessionsListResponseContentValidator.class);

    public SessionsListResponseContentValidator(String taskId, String sessionId, NodeContext kernelContext) {
        super(taskId, sessionId, kernelContext);
    }

    @Override
    public String getName() {
        return "SessionsListResponseContentValidator";
    }

    @Override
    public boolean validate(HttpRequestBase query, E endpoint, HttpResponse result, long duration)  {
        String content = result.getBody();

        List<SessionEntity> actualSessions = JSON.parseArray(content, SessionEntity.class);

        boolean isValid = true;

        //Checks.
        try {
            int actlSize = actualSessions.size();
            int expctdSize = TestContext.getSessions().size();
            Assert.assertTrue("Several session records are expected. Check returned list's size", 1 < actlSize);
            List<SessionEntity> noDuplicatesActualList = actualSessions.stream().distinct().collect(Collectors.toList());
            Assert.assertEquals("Response contains duplicate session records", actlSize, noDuplicatesActualList.size());
            //TODO: Wait for JFG-908 to be resolved and un-comment/re-factor.
 //           Assert.assertTrue(String.format("Actual list(%d) is longer than expected one(%d).", actlSize, expctdSize), actlSize <= expctdSize);
            // Re-factor one JFG-908 is resolved.
            Assert.assertTrue("Actual list is not a sub-set of expected set.", actualSessions.containsAll(TestContext.getSessions()));

            SessionEntity randomActualEntity = actualSessions.get((new Random().nextInt(actlSize-1)));
            SessionEntity correspondingExpectedSession = TestContext.getSession(randomActualEntity.getId());

            Assert.assertEquals("Expected and actual sessions are not equal.", correspondingExpectedSession, randomActualEntity);
        } catch (AssertionFailedError e) {
            isValid = false;
            log.warn("{}'s query response content is not valid, due to [{}].", query.toString(), e.getMessage());
            logResponseAsFailed(endpoint, result);
        }

        return isValid;
    }

    protected Filter getAllSessionFieldsFilter() {
        return filter(where("id").exists(true)
                    .and("comment").exists(true)
                    .and("startDate").exists(true)
                    .and("endDate").exists(true)
                    .and("kernels").exists(true));
    }
}