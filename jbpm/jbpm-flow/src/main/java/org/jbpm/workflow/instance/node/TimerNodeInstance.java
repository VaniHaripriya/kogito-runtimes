/*
 * Copyright 2010 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jbpm.workflow.instance.node;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jbpm.process.instance.InternalProcessRuntime;
import org.jbpm.workflow.core.Node;
import org.jbpm.workflow.core.node.TimerNode;
import org.kie.api.runtime.process.EventListener;
import org.kie.kogito.internal.process.runtime.KogitoNodeInstance;
import org.kie.kogito.jobs.ExpirationTime;
import org.kie.kogito.jobs.JobsService;
import org.kie.kogito.jobs.ProcessInstanceJobDescription;
import org.kie.kogito.process.BaseEventDescription;
import org.kie.kogito.process.EventDescription;
import org.kie.kogito.services.uow.BaseWorkUnit;
import org.kie.kogito.timer.TimerInstance;
import org.kie.kogito.uow.WorkUnit;

public class TimerNodeInstance extends StateBasedNodeInstance implements EventListener {

    private static final long serialVersionUID = 510l;
    public static final String TIMER_TRIGGERED_EVENT = "timerTriggered";

    private String timerId;

    public TimerNode getTimerNode() {
        return (TimerNode) getNode();
    }

    public String getTimerId() {
        return timerId;
    }

    public void internalSetTimerId(String timerId) {
        this.timerId = timerId;
    }

    @Override
    public void internalTrigger(KogitoNodeInstance from, String type) {
        if (!Node.CONNECTION_DEFAULT_TYPE.equals(type)) {
            throw new IllegalArgumentException(
                    "A TimerNode only accepts default incoming connections!");
        }
        triggerTime = new Date();
        ExpirationTime expirationTime = createTimerInstance(getTimerNode().getTimer());
        if (getTimerInstances() == null) {
            addTimerListener();
        }
        internalSetTimerId(UUID.randomUUID().toString());
        final InternalProcessRuntime processRuntime = (InternalProcessRuntime) getProcessInstance().getKnowledgeRuntime().getProcessRuntime();
        //Deffer the timer scheduling to the end of current UnitOfWork execution chain
        processRuntime.getUnitOfWorkManager().currentUnitOfWork().intercept(
                new BaseWorkUnit<>(this, instance -> {
                    ProcessInstanceJobDescription jobDescription =
                            ProcessInstanceJobDescription.builder()
                                    .id(getTimerId())
                                    .timerId("-1")
                                    .expirationTime(expirationTime)
                                    .processInstanceId(getProcessInstance().getStringId())
                                    .rootProcessInstanceId(getProcessInstance().getRootProcessInstanceId())
                                    .processId(getProcessInstance().getProcessId())
                                    .rootProcessId(getProcessInstance().getRootProcessId())
                                    .nodeInstanceId(Optional.ofNullable(from).map(KogitoNodeInstance::getStringId).orElse(null))
                                    .build();
                    JobsService jobService = processRuntime.getJobsService();
                    String jobId = jobService.scheduleProcessInstanceJob(jobDescription);
                    internalSetTimerId(jobId);
                }, i -> {
                }, WorkUnit.LOW_PRIORITY));
    }

    @Override
    public void signalEvent(String type, Object event) {
        if (TIMER_TRIGGERED_EVENT.equals(type)) {
            TimerInstance timer = (TimerInstance) event;
            if (Objects.equals(timer.getId(), getTimerId())) {
                triggerCompleted(timer.getRepeatLimit() <= 0);
            }
        }
    }

    @Override
    public String[] getEventTypes() {
        return new String[] { TIMER_TRIGGERED_EVENT };
    }

    @Override
    public void triggerCompleted(boolean remove) {
        triggerCompleted(Node.CONNECTION_DEFAULT_TYPE, remove);
    }

    @Override
    public void cancel() {
        ((InternalProcessRuntime) getProcessInstance().getKnowledgeRuntime().getProcessRuntime()).getJobsService().cancelJob(timerId);
        super.cancel();
    }

    @Override
    public void addEventListeners() {
        super.addEventListeners();
        if (getTimerInstances() == null) {
            addTimerListener();
        }
    }

    @Override
    public void removeEventListeners() {
        super.removeEventListeners();
        getProcessInstance().removeEventListener(TIMER_TRIGGERED_EVENT, this, false);
    }

    @Override
    public Set<EventDescription<?>> getEventDescriptions() {
        Map<String, String> properties = new HashMap<>();
        properties.put("TimerID", getTimerId());
        properties.put("Delay", getTimerNode().getTimer().getDelay());
        properties.put("Period", getTimerNode().getTimer().getPeriod());
        properties.put("Date", getTimerNode().getTimer().getDate());
        return Collections
                .singleton(new BaseEventDescription(TIMER_TRIGGERED_EVENT, getNodeDefinitionId(), getNodeName(), "timer", getStringId(), getProcessInstance().getStringId(), null, properties));
    }
}
