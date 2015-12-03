/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.core.manager.impl;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.core.definition.Api;
import io.gravitee.gateway.core.definition.validator.ValidationException;
import io.gravitee.gateway.core.definition.validator.Validator;
import io.gravitee.gateway.core.event.ApiEvent;
import io.gravitee.gateway.core.manager.ApiManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiManagerImpl implements ApiManager {

    private final Logger logger = LoggerFactory.getLogger(ApiManagerImpl.class);

    @Autowired
    private EventManager eventManager;

    @Autowired
    private Validator validator;

    private final Map<String, Api> apis = new HashMap<>();

    @Override
    public void deploy(Api api) {
        logger.info("Deploying {} [{}]", api.getName(), api.getId());

        try {
            validator.validate(api);
            apis.put(api.getId(), api);

            if (api.isEnabled()) {
                eventManager.publishEvent(ApiEvent.DEPLOY, api);
            } else {
                logger.debug("{} is not enabled. Skip deployment.", api);
            }
        } catch (ValidationException ve) {
            logger.error("API {} can't be deployed because of validation errors", api, ve);
        }
    }

    @Override
    public void update(Api api) {
        logger.info("Updating {} [{}]", api.getName(), api.getId());

        try {
            validator.validate(api);

            apis.put(api.getId(), api);
            eventManager.publishEvent(ApiEvent.UPDATE, api);
        } catch (ValidationException ve) {
            logger.error("API {} can't be updated because of validation errors", api, ve);
        }
    }

    @Override
    public void undeploy(String apiId) {
        Api currentApi = apis.remove(apiId);
        if (currentApi != null) {
            logger.info("Undeploying {} [{}]", currentApi.getName(), currentApi.getId());

            eventManager.publishEvent(ApiEvent.UNDEPLOY, currentApi);
            logger.info("{} has been undeployed", apiId);
        }
    }

    @Override
    public Collection<Api> apis() {
        return apis.values();
    }

    @Override
    public Api get(String name) {
        return apis.get(name);
    }

    public void setEventManager(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    public void setValidator(Validator validator) {
        this.validator = validator;
    }
}
