/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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
package org.kie.kogito.eventdriven.rules;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.kie.kogito.config.ConfigBean;
import org.kie.kogito.event.EventEmitter;
import org.kie.kogito.event.EventReceiver;

import io.quarkus.runtime.Startup;

@Startup
public class QuarkusEventDrivenRulesController extends EventDrivenRulesController {

    @Inject
    ConfigBean config;

    @Inject
    EventEmitter eventEmitter;

    @Inject
    EventReceiver eventReceiver;

    @PostConstruct
    private void onPostConstruct() {
        init(config, eventEmitter, eventReceiver);
    }
}
