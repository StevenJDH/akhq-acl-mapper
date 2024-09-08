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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.GroupModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.FullNameMapper;
import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN;
import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN;
import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.INCLUDE_IN_LIGHTWEIGHT_ACCESS_TOKEN;
import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO;
import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AkhqAclMapperTest {

    private final AkhqAclMapper akhqAclMapper = new AkhqAclMapper();
    private static final String CLAIM_NAME_KEY = "groups-test";
    private static final ProtocolMapperModel PROTOCOL_MAPPER = new ProtocolMapperModel();

    @Mock
    private UserSessionModel mockUserSession;

    @Mock
    private UserModel mockUser;
    
    @Mock
    private GroupModel mockGroup;

    @BeforeAll
    static void setUp() {
        Map<String, String> testConfig = new HashMap<>();
        testConfig.put(TOKEN_CLAIM_NAME, CLAIM_NAME_KEY);
        testConfig.put(INCLUDE_IN_ID_TOKEN, Boolean.toString(true));
        PROTOCOL_MAPPER.setConfig(testConfig);
    }

    @Test
    @DisplayName("Should return correct category for mapper.")
    void Should_ReturnCorrectCategory_ForMapper() {
        var expectedDisplayCategory = new FullNameMapper().getDisplayCategory();

        var displayCategory = akhqAclMapper.getDisplayCategory();

        assertThat(displayCategory).isNotBlank()
                .isEqualTo(expectedDisplayCategory);
    }

    @Test
    @DisplayName("Should return correct type for mapper.")
    void Should_ReturnCorrectType_ForMapper() {
        String expectedDisplayType = "AKHQ ACL Mapper";
        
        assertThat(akhqAclMapper.getDisplayType()).isNotBlank()
                .isEqualTo(expectedDisplayType);
    }

    @Test
    @DisplayName("Should return correct help text for mapper.")
    void Should_ReturnCorrectHelpText_ForMapper() {
        String expectedHelpText = "An AKHQ ACL mapper for Keycloak to transition from AKHQ version 0.24.x to 0.25.x and above.";
        
        assertThat(akhqAclMapper.getHelpText()).isNotBlank()
                .isEqualTo(expectedHelpText);
    }

    @Test
    @DisplayName("Should return correct id for mapper.")
    void Should_ReturnCorrectId_ForMapper() {
        String expectedId = "stevenjdh-akhq-acl-mapper";
        
        assertThat(akhqAclMapper.getId()).isNotBlank()
                .isEqualTo(expectedId);
    }

    @Test
    @DisplayName("Should return correct priority for mapper.")
    void Should_ReturnCorrectPriority_ForMapper() {
        int expectedPriority = 50;

        assertThat(akhqAclMapper.getPriority()).isNotZero()
                .isEqualTo(expectedPriority);
    }

    @Test
    @DisplayName("Should have standard properties configured for mapper.")
    void Should_HaveStandardPropertiesConfigured_ForMapper() {
        List<String> expectedConfigProperties = List.of(TOKEN_CLAIM_NAME, INCLUDE_IN_ID_TOKEN,
                INCLUDE_IN_ACCESS_TOKEN, INCLUDE_IN_LIGHTWEIGHT_ACCESS_TOKEN, INCLUDE_IN_USERINFO);

        List<String> configPropertyNames = akhqAclMapper.getConfigProperties().stream()
                .map(ProviderConfigProperty::getName)
                .toList();

        assertThat(configPropertyNames).isNotEmpty()
                .hasSameElementsAs(expectedConfigProperties);
    }

    @Test
    @DisplayName("Should add custom claim to id token when token transformation runs.")
    void Should_AddCustomClaimToIdToken_When_TokenTransformationRuns() {
        when(mockUserSession.getUser())
                .thenReturn(mockUser);

        var transformedToken = akhqAclMapper.transformIDToken(new IDToken(), PROTOCOL_MAPPER,
                null, mockUserSession, null);

        assertThat(transformedToken.getOtherClaims()).isNotEmpty()
                .containsKey(CLAIM_NAME_KEY);
    }

    @Test
    @DisplayName("Should set custom claim with ACLs when user group has attributes configured.")
    void Should_SetCustomClaimWithAcls_When_UserGroupHasAttributesConfigured() {
        var token = new IDToken();
        List<Map<String, Object>> claimEntries = List.of(
                Map.of("role", "topic-reader", "patterns", List.of("moe.*")),
                Map.of("role", "group-reader", "patterns", List.of("larry.*")),
                Map.of("role", "connect-reader", "patterns", List.of("curly.*")),
                Map.of("role", "registry-reader", "patterns", List.of("shemp.*")),
                Map.of("role", "acl-reader", "patterns", List.of("joe.*"))
        );
        Map<String, Object> expectedClaimValue = Map.of("foobar-group", claimEntries);

        when(mockGroup.getName())
                .thenReturn("foobar-group");
        when(mockGroup.getFirstAttribute("topics-filter-regexp"))
                .thenReturn("moe.*");
        when(mockGroup.getFirstAttribute("consumer-groups-filter-regexp"))
                .thenReturn("larry.*");
        when(mockGroup.getFirstAttribute("connects-filter-regexp"))
                .thenReturn("curly.*");
        when(mockGroup.getFirstAttribute("registry-filter-regexp"))
                .thenReturn("shemp.*");
        when(mockGroup.getFirstAttribute("acls-filter-regexp"))
                .thenReturn("joe.*");
        when(mockUser.getGroupsStream())
                .thenReturn(Stream.of(mockGroup));
        when(mockUserSession.getUser())
                .thenReturn(mockUser);

        akhqAclMapper.setClaim(token, PROTOCOL_MAPPER, mockUserSession, null, null);

        assertThat(token.getOtherClaims()).isNotEmpty()
                .containsEntry(CLAIM_NAME_KEY, expectedClaimValue);
    }
    
    @Test
    @DisplayName("Should set custom claim with ACLs when using inline permission definition.")
    void Should_SetCustomClaimWithAcls_When_UsingInlinePermissionDefinition() {
        var token = new IDToken();
        List<Map<String, Object>> claimEntries = List.of(
                Map.of("role", "topic-writer", "patterns", List.of("marco.*"), "clusters", List.of("foobar")),
                Map.of("role", "group-reader", "patterns", List.of("polo.*"), "clusters", List.of("foobar"))
        );
        Map<String, Object> expectedClaimValue = Map.of("foobar-group", claimEntries);

        when(mockGroup.getName())
                .thenReturn("foobar-group");
        when(mockGroup.getFirstAttribute("topics-filter-regexp"))
                .thenReturn("role:topic-writer,pattern:marco.*,cluster:foobar");
        when(mockGroup.getFirstAttribute("consumer-groups-filter-regexp"))
                .thenReturn("role:group-reader,pattern:polo.*,cluster:foobar");
        when(mockUser.getGroupsStream())
                .thenReturn(Stream.of(mockGroup));
        when(mockUserSession.getUser())
                .thenReturn(mockUser);

        akhqAclMapper.setClaim(token, PROTOCOL_MAPPER, mockUserSession, null, null);

        assertThat(token.getOtherClaims()).isNotEmpty()
                .containsEntry(CLAIM_NAME_KEY, expectedClaimValue);
    }
    
    @Test
    @DisplayName("Should set default ACL values for invalid inline permission definition entries.")
    void Should_SetDefaultAclValues_ForInvalidInlinePermissionDefinitionEntries() {
        var token = new IDToken();
        List<Map<String, Object>> claimEntries = List.of(
                Map.of("role", "topic-reader", "patterns", List.of("marco.*"), "clusters", List.of("foobar")),
                Map.of("role", "group-reader", "patterns", List.of("^$"), "clusters", List.of("foobar"))
        );
        Map<String, Object> expectedClaimValue = Map.of("foobar-group", claimEntries);

        when(mockGroup.getName())
                .thenReturn("foobar-group");
        when(mockGroup.getFirstAttribute("topics-filter-regexp"))
                .thenReturn("rolZ:topic-writer,pattern:marco.*,cluster:foobar");
        when(mockGroup.getFirstAttribute("consumer-groups-filter-regexp"))
                .thenReturn("role:group-reader,pattern:,cluster:foobar");
        when(mockUser.getGroupsStream())
                .thenReturn(Stream.of(mockGroup));
        when(mockUserSession.getUser())
                .thenReturn(mockUser);

        akhqAclMapper.setClaim(token, PROTOCOL_MAPPER, mockUserSession, null, null);

        assertThat(token.getOtherClaims()).isNotEmpty()
                .containsEntry(CLAIM_NAME_KEY, expectedClaimValue);
    }
    
    @Test
    @DisplayName("Should set custom claim without ACLs when user group attributes are not configured.")
    void Should_SetCustomClaimWithoutAcls_When_UserGroupAttributesAreNotConfigured() {
        var token = new IDToken();
        Map<String, Object> expectedClaimValue = Map.of();

        when(mockGroup.getName())
                .thenReturn("foobar-group");
        when(mockUser.getGroupsStream())
                .thenReturn(Stream.of(mockGroup));
        when(mockUserSession.getUser())
                .thenReturn(mockUser);

        akhqAclMapper.setClaim(token, PROTOCOL_MAPPER, mockUserSession, null, null);

        assertThat(token.getOtherClaims()).isNotEmpty()
                .containsEntry(CLAIM_NAME_KEY, expectedClaimValue);
    }
    
    @Test
    @DisplayName("Should skip adding an ACL for each user group attribute that is null or blank.")
    void Should_SkipAddingAnAcl_ForEachUserGroupAttributeThatIsNullOrBlank() {
        var token = new IDToken();
        Map<String, Object> expectedClaimValue = Map.of();

        when(mockGroup.getName())
                .thenReturn("foobar-group");
        when(mockGroup.getFirstAttribute("topics-filter-regexp"))
                .thenReturn("");
        when(mockGroup.getFirstAttribute("consumer-groups-filter-regexp"))
                .thenReturn(" ");
        when(mockGroup.getFirstAttribute("connects-filter-regexp"))
                .thenReturn(null);
        when(mockUser.getGroupsStream())
                .thenReturn(Stream.of(mockGroup));
        when(mockUserSession.getUser())
                .thenReturn(mockUser);

        akhqAclMapper.setClaim(token, PROTOCOL_MAPPER, mockUserSession, null, null);

        assertThat(token.getOtherClaims()).isNotEmpty()
                .containsEntry(CLAIM_NAME_KEY, expectedClaimValue);
    }
    
    @Test
    @DisplayName("Should not set custom claim when claim name is not configured.")
    void Should_NotSetCustomClaim_When_ClaimNameIsNotConfigured() {
        var token = new IDToken();
        var protocolMapper = new ProtocolMapperModel();
        protocolMapper.setConfig(Map.of(INCLUDE_IN_ID_TOKEN, Boolean.toString(true)));
        
        when(mockUser.getGroupsStream())
                .thenReturn(Stream.empty());
        when(mockUserSession.getUser())
                .thenReturn(mockUser);

        akhqAclMapper.setClaim(token, protocolMapper, mockUserSession, null, null);

        assertThat(token.getOtherClaims())
                .doesNotContainKey(CLAIM_NAME_KEY);
    }
}
