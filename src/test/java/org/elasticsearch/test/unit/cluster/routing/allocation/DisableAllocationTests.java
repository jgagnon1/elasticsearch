/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.test.unit.cluster.routing.allocation;

import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.routing.RoutingTable;
import org.elasticsearch.cluster.routing.allocation.AllocationService;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.testng.annotations.Test;

import static org.elasticsearch.cluster.ClusterState.newClusterStateBuilder;
import static org.elasticsearch.cluster.metadata.IndexMetaData.newIndexMetaDataBuilder;
import static org.elasticsearch.cluster.metadata.MetaData.newMetaDataBuilder;
import static org.elasticsearch.cluster.node.DiscoveryNodes.newNodesBuilder;
import static org.elasticsearch.cluster.routing.RoutingBuilders.routingTable;
import static org.elasticsearch.cluster.routing.ShardRoutingState.INITIALIZING;
import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.elasticsearch.test.unit.cluster.routing.allocation.RoutingAllocationTests.newNode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 */
@Test
public class DisableAllocationTests {

    private final ESLogger logger = Loggers.getLogger(DisableAllocationTests.class);

    @Test
    public void testDisableAllocation() {
        AllocationService strategy = new AllocationService(settingsBuilder()
                .put("cluster.routing.allocation.disable_new_allocation", true)
                .put("cluster.routing.allocation.disable_allocation", true)
                .build());

        logger.info("Building initial routing table");

        MetaData metaData = newMetaDataBuilder()
                .put(newIndexMetaDataBuilder("test").numberOfShards(1).numberOfReplicas(1))
                .build();

        RoutingTable routingTable = routingTable()
                .addAsNew(metaData.index("test"))
                .build();

        ClusterState clusterState = newClusterStateBuilder().metaData(metaData).routingTable(routingTable).build();

        logger.info("--> adding two nodes on same rack and do rerouting");
        clusterState = newClusterStateBuilder().state(clusterState).nodes(newNodesBuilder()
                .put(newNode("node1"))
                .put(newNode("node2"))
        ).build();
        routingTable = strategy.reroute(clusterState).routingTable();
        clusterState = newClusterStateBuilder().state(clusterState).routingTable(routingTable).build();
        assertThat(clusterState.routingNodes().shardsWithState(INITIALIZING).size(), equalTo(0));

    }

    @Test
    public void testDisableReplicaAllocation() {
        AllocationService strategy = new AllocationService(settingsBuilder()
                .put("cluster.routing.allocation.disable_replica_allocation", true)
                .build());

        logger.info("Building initial routing table");

        MetaData metaData = newMetaDataBuilder()
                .put(newIndexMetaDataBuilder("test").numberOfShards(1).numberOfReplicas(1))
                .build();

        RoutingTable routingTable = routingTable()
                .addAsNew(metaData.index("test"))
                .build();

        ClusterState clusterState = newClusterStateBuilder().metaData(metaData).routingTable(routingTable).build();

        logger.info("--> adding two nodes on same rack and do rerouting");
        clusterState = newClusterStateBuilder().state(clusterState).nodes(newNodesBuilder()
                .put(newNode("node1"))
                .put(newNode("node2"))
        ).build();
        routingTable = strategy.reroute(clusterState).routingTable();
        clusterState = newClusterStateBuilder().state(clusterState).routingTable(routingTable).build();
        assertThat(clusterState.routingNodes().shardsWithState(INITIALIZING).size(), equalTo(1));

        logger.info("--> start the shards (primaries)");
        routingTable = strategy.applyStartedShards(clusterState, clusterState.routingNodes().shardsWithState(INITIALIZING)).routingTable();
        clusterState = newClusterStateBuilder().state(clusterState).routingTable(routingTable).build();

        assertThat(clusterState.routingNodes().shardsWithState(INITIALIZING).size(), equalTo(0));
    }
}
