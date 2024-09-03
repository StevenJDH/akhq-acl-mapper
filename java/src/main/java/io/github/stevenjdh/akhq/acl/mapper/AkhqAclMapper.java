/*
 * This file is part of AKHQ ACL Mapper <https://github.com/StevenJDH/akhq-acl-mapper>.
 * Copyright (C) 2024 Steven Jenkins De Haro.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.stevenjdh.akhq.acl.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

public class AkhqAclMapper extends AbstractOIDCProtocolMapper
                           implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    public static final String PROVIDER_ID = "stevenjdh-akhq-acl-mapper";
    public static final String DISPLAY_NAME = "AKHQ ACL Mapper";
    public static final String HELP_TEXT = "An AKHQ ACL mapper for Keycloak to transition from AKHQ version 0.24.x to 0.25.x and above.";
    
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(AkhqAclMapper.class.getName());

    static {
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, AkhqAclMapper.class);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
    
    @Override
    public String getDisplayType() {
        return DISPLAY_NAME;
    }
    
    @Override
    public String getHelpText() {
        return HELP_TEXT;
    }
    
    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }
    
    @Override
    public int getPriority() {
        return ProtocolMapperUtils.PRIORITY_SCRIPT_MAPPER;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    // Ignore SPI warning (KC-SERVICES0047) for implementing the internal SPI protocol-mapper, it's expected.
    // Reference: https://github.com/keycloak/keycloak/discussions/9974
    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel,
                            UserSessionModel userSession, KeycloakSession
                            keycloakSession, ClientSessionContext clientSessionCtx) {

        var user = userSession.getUser();
        Map<String, Object> groupClaims = new HashMap<>();
        Stream<GroupModel> groups = user.getGroupsStream();
        var count = new AtomicInteger(0);

        groups.forEach(group -> {
            String groupName = group.getName();

            // Initialize the list for the current group.
            List<Map<String, Object>> claimEntries = new ArrayList<>();
            groupClaims.put(groupName, claimEntries);

            var topicEntry = getClaimEntry("topic-reader", "topics-filter-regexp", group);
            var groupEntry = getClaimEntry("group-reader", "consumer-groups-filter-regexp", group);
            var connectEntry = getClaimEntry("connect-reader", "connects-filter-regexp", group);

            claimEntries.add(topicEntry);
            claimEntries.add(groupEntry);
            claimEntries.add(connectEntry);

            count.getAndIncrement();
        });

        LOGGER.debug(String.format("=== Number of groups processed [%s]: %d", user.getUsername(), count.get()));
        token.setOtherClaims("groups", groupClaims);
    }

    private static Map<String, Object> getClaimEntry(String role, String attribute, GroupModel group) {
        List<String> patterns = new ArrayList<>();
        Map<String, Object> claimEntries = new HashMap<>();

        patterns.add(group.getFirstAttribute(attribute));
        claimEntries.put("role", role);
        claimEntries.put("patterns", patterns);

        return claimEntries;
    }
}
