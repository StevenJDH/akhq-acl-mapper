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
 * Version: 0.1.0
 */

var debug = true
var ArrayList = Java.type("java.util.ArrayList");

function debugOutput(msg) {
    if (debug) print("=== akhq-acl-mapper [Debug]: " + msg);
}

function getClaimEntry(role, attribute, group) {
    var claim = { "role": role };
    claim["patterns"] = new ArrayList();
    claim["patterns"].add(group.getFirstAttribute(attribute));
    return claim
}

var groupClaims = {};
var groups = user.getGroupsStream();
var count = 0

groups.forEach(function(group) {
    var groupName = group.getName();

    // Initialize the list for the current group.
    groupClaims[groupName] = new ArrayList();

    var topicEntry = getClaimEntry("topic-reader", "topics-filter-regexp", group);
    var groupEntry = getClaimEntry("group-reader", "consumer-groups-filter-regexp", group);
    var connectEntry = getClaimEntry("connect-reader", "connects-filter-regexp", group);

    groupClaims[groupName].add(topicEntry);
    groupClaims[groupName].add(groupEntry);
    groupClaims[groupName].add(connectEntry);

    count++
});

debugOutput('Number of groups processed [' + user.getUsername() + ']: ' + count);
token.setOtherClaims("groups", groupClaims);
