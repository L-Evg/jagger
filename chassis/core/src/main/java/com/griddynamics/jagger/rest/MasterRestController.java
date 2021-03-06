package com.griddynamics.jagger.rest;

import com.griddynamics.jagger.dbapi.entity.TaskData;
import com.griddynamics.jagger.master.CompositeTask;
import com.griddynamics.jagger.master.SessionIdProvider;
import com.griddynamics.jagger.master.TaskExecutionStatusProvider;
import com.griddynamics.jagger.master.TaskIdProvider;
import com.griddynamics.jagger.master.configuration.Configuration;
import com.griddynamics.jagger.master.configuration.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

/**
 * Master node REST API controller based on Spring MVC.
 * Aimed to expose info about currently running test suit.
 */
@RestController
@RequestMapping("/master")
public class MasterRestController {
    
    @Resource(name = "sessionIdProvider")
    private SessionIdProvider sessionIdProvider;
    
    @Resource(name = "${chassis.master.session.configuration.bean.name}")
    private Configuration configuration;
    
    @Value("${chassis.master.session.configuration.bean.name}")
    private String configurationName;
    
    @Autowired
    private TaskExecutionStatusProvider taskExecutionStatusProvider;
    
    @Autowired
    private TaskIdProvider taskIdProvider;
    
    private Map<String, Task> nameToTaskMap;
    
    @GetMapping(path = "/ping")
    public ResponseEntity<String> hello() {
        return new ResponseEntity<>("pong", HttpStatus.OK);
    }
    
    @GetMapping(path = "/session")
    public ResponseEntity<SessionInfo> getSessionInfo() {
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.id = sessionIdProvider.getSessionId();
        sessionInfo.comment = sessionIdProvider.getSessionComment();
        sessionInfo.name = sessionIdProvider.getSessionName();
        sessionInfo.configName = configurationName;
        
        return new ResponseEntity<>(sessionInfo, HttpStatus.OK);
    }
    
    @GetMapping(path = "/tests")
    public ResponseEntity<List<TestInfo>> getTasks() {
    
        List<TestInfo> testInfos = Lists.newArrayList();
        for (Task task : configuration.getTasks()) {
            testInfos.add(getTestInfoFrom(task));
        }
        
        return new ResponseEntity<>(testInfos, HttpStatus.OK);
    }
    
    @GetMapping(path = "/tests/{name}")
    public ResponseEntity<TestInfo> getTestInfo(@PathVariable String name) {
        
        if (nameToTaskMap == null) {
            nameToTaskMap = mapTaskToName(configuration.getTasks());
        }
        
        Task task = nameToTaskMap.get(name);
        if (task == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(getTestInfoFrom(task), HttpStatus.OK);
    }
    
    private Map<String, Task> mapTaskToName(List<? extends Task> taskList) {
        Map<String, Task> nameToTaskMap = Maps.newHashMap();
        for (Task task : taskList) {
            nameToTaskMap.put(task.getTaskName(), task);
            if (task instanceof CompositeTask) {
                CompositeTask compositeTask = (CompositeTask) task;
                nameToTaskMap.putAll(mapTaskToName(compositeTask.getLeading()));
                nameToTaskMap.putAll(mapTaskToName(compositeTask.getAttendant()));
            }
        }
        
        return nameToTaskMap;
    }
    
    private TestInfo getTestInfoFrom(Task task) {
        TestInfo testInfo = new TestInfo();
        if (task instanceof CompositeTask) {
            CompositeTask compositeTask = (CompositeTask) task;
            TestGroupInfo testGroupInfo = new TestGroupInfo();
            testGroupInfo.leadingTests = Lists.newArrayList();
            for (Task leadingTask : compositeTask.getLeading()) {
                testGroupInfo.leadingTests.add(getTestInfoFrom(leadingTask));
            }
    
            testGroupInfo.attendantTests = Lists.newArrayList();
            for (Task attendantTask : compositeTask.getAttendant()) {
                testGroupInfo.attendantTests.add(getTestInfoFrom(attendantTask));
            }
    
            testInfo = testGroupInfo;
        }
    
        testInfo.name = task.getTaskName();
        testInfo.sessionId = sessionIdProvider.getSessionId();
        if (task.getNumber() > 0) {  // 0 means it wasn't set yet.
            testInfo.number = task.getNumber();
            testInfo.id = taskIdProvider.stringify(task.getNumber());
            testInfo.status = taskExecutionStatusProvider.getStatus(testInfo.id);
        }
        
        return testInfo;
    }
    
    public static class TestGroupInfo extends TestInfo {
        private List<TestInfo> leadingTests;
        private List<TestInfo> attendantTests;
        
        public List<TestInfo> getLeadingTests() {
            return leadingTests;
        }
        
        public List<TestInfo> getAttendantTests() {
            return attendantTests;
        }
    }
    
    public static class TestInfo {
        private String id;
        private String sessionId;
        private String name;
        private Integer number;
        private TaskData.ExecutionStatus status;
        
        public String getId() {
            return id;
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public String getName() {
            return name;
        }
        
        public Integer getNumber() {
            return number;
        }
        
        public TaskData.ExecutionStatus getStatus() {
            return status;
        }
    }
    
    public static class SessionInfo {
        private String id;
        private String comment;
        private String name;
        private String configName;
        
        public String getId() {
            return id;
        }
        
        public String getComment() {
            return comment;
        }
        
        public String getName() {
            return name;
        }
    
        public String getConfigName() {
            return configName;
        }
    }
}
