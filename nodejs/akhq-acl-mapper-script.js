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

/*
 * File: akhq-acl-mapper.js
 * Version: 0.3.0
 *
 * Predefined Objects:
 * =========================
 * user - the current user.
 * realm - the current realm.
 * token - the current token.
 * userSession - the current userSession.
 * keycloakSession - the current keycloakSession.
 */

var debug = true
var ArrayList = Java.type("java.util.ArrayList");

function debugOutput(msg) {
    if (debug) print("=== akhq-acl-mapper [Debug]: " + msg);
}

function getClaimEntry(role, pattern) {
    var claim = { "role": role };

    claim["patterns"] = new ArrayList();
    claim["patterns"].add(pattern);

    return claim
}

var groupClaims = {};
var groups = user.getGroupsStream();
var count = 0

groups.forEach(function(group) {
    var groupName = group.getName();
    var claimEntries = new ArrayList();

    var topicFilter = group.getFirstAttribute("topics-filter-regexp");
    var groupFilter = group.getFirstAttribute("consumer-groups-filter-regexp");
    var connectFilter = group.getFirstAttribute("connects-filter-regexp");
    var registryFilter = group.getFirstAttribute("registry-filter-regexp");
    var aclFilter = group.getFirstAttribute("acls-filter-regexp");


    if (topicFilter) {
        claimEntries.add(getClaimEntry("topic-reader", topicFilter));
    }

    if (groupFilter) {
        claimEntries.add(getClaimEntry("group-reader", groupFilter));
    }

    if (connectFilter) {
        claimEntries.add(getClaimEntry("connect-reader", connectFilter));
    }

    if (registryFilter) {
        claimEntries.add(getClaimEntry("registry-reader", registryFilter));
    }

    if (aclFilter) {
        claimEntries.add(getClaimEntry("acl-reader", aclFilter));
    }

    // Avoids other unrelated user groups from appearing in token.
    if (!claimEntries.isEmpty()) {
        groupClaims[groupName] = claimEntries;
    }

    count++
});

debugOutput('Number of groups processed [' + user.getUsername() + ']: ' + count);
exports = groupClaims;
