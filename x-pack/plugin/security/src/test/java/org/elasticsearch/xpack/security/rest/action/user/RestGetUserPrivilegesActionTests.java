/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.security.rest.action.user;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.core.security.action.user.GetUserPrivilegesResponse;
import org.elasticsearch.xpack.core.security.authz.RoleDescriptor.ApplicationResourcePrivileges;
import org.elasticsearch.xpack.core.security.authz.permission.FieldPermissionsDefinition;
import org.elasticsearch.xpack.core.security.authz.privilege.ConditionalClusterPrivilege;
import org.elasticsearch.xpack.core.security.authz.privilege.ConditionalClusterPrivileges;
import org.hamcrest.Matchers;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class RestGetUserPrivilegesActionTests extends ESTestCase {

    public void testBuildResponse() throws Exception {
        final RestGetUserPrivilegesAction.RestListener listener = new RestGetUserPrivilegesAction.RestListener(null);


        final Set<String> cluster = new LinkedHashSet<>(Arrays.asList("monitor", "manage_ml", "manage_watcher"));
        final Set<ConditionalClusterPrivilege> conditionalCluster = Collections.singleton(
            new ConditionalClusterPrivileges.ManageApplicationPrivileges(new LinkedHashSet<>(Arrays.asList("app01", "app02"))));
        final Set<GetUserPrivilegesResponse.Indices> index = new LinkedHashSet<>(Arrays.asList(
            new GetUserPrivilegesResponse.Indices(Arrays.asList("index-1", "index-2", "index-3-*"), Arrays.asList("read", "write"),
                new LinkedHashSet<>(Arrays.asList(
                    new FieldPermissionsDefinition.FieldGrantExcludeGroup(new String[]{ "public.*" }, new String[0]),
                    new FieldPermissionsDefinition.FieldGrantExcludeGroup(new String[]{ "*" }, new String[]{ "private.*" })
                )),
                new LinkedHashSet<>(Arrays.asList(
                    new BytesArray("{ \"term\": { \"access\": \"public\" } }"),
                    new BytesArray("{ \"term\": { \"access\": \"standard\" } }")
                ))
            ),
            new GetUserPrivilegesResponse.Indices(Arrays.asList("index-4"), Collections.singleton("all"),
                Collections.emptySet(), Collections.emptySet()
            )
        ));
        final Set<ApplicationResourcePrivileges> application = Sets.newHashSet(
            ApplicationResourcePrivileges.builder().application("app01").privileges("read", "write").resources("*").build(),
            ApplicationResourcePrivileges.builder().application("app01").privileges("admin").resources("department/1").build(),
            ApplicationResourcePrivileges.builder().application("app02").privileges("all").resources("tenant/42", "tenant/99").build()
        );
        final Set<String> runAs = new LinkedHashSet<>(Arrays.asList("app-user-*", "backup-user"));
        final GetUserPrivilegesResponse response = new GetUserPrivilegesResponse(cluster, conditionalCluster, index, application, runAs);
        XContentBuilder builder = jsonBuilder();
        listener.buildResponse(response, builder);

        String json = Strings.toString(builder);
        assertThat(json, Matchers.equalTo("{" +
            "\"cluster\":[\"monitor\",\"manage_ml\",\"manage_watcher\"]," +
            "\"global\":[" +
            "{\"application\":{\"manage\":{\"applications\":[\"app01\",\"app02\"]}}}" +
            "]," +
            "\"indices\":[" +
            "{\"names\":[\"index-1\",\"index-2\",\"index-3-*\"]," +
            "\"privileges\":[\"read\",\"write\"]," +
            "\"field_security\":[" +
            "{\"grant\":[\"public.*\"]}," +
            "{\"grant\":[\"*\"],\"except\":[\"private.*\"]}" +
            "]," +
            "\"query\":[" +
            "\"{ \\\"term\\\": { \\\"access\\\": \\\"public\\\" } }\"," +
            "\"{ \\\"term\\\": { \\\"access\\\": \\\"standard\\\" } }\"" +
            "]}," +
            "{\"names\":[\"index-4\"],\"privileges\":[\"all\"]}" +
            "]," +
            "\"applications\":[" +
            "{\"application\":\"app01\",\"privileges\":[\"read\",\"write\"],\"resources\":[\"*\"]}," +
            "{\"application\":\"app01\",\"privileges\":[\"admin\"],\"resources\":[\"department/1\"]}," +
            "{\"application\":\"app02\",\"privileges\":[\"all\"],\"resources\":[\"tenant/42\",\"tenant/99\"]}" +
            "]," +
            "\"run_as\":[\"app-user-*\",\"backup-user\"]" +
            "}"
        ));
    }

}
