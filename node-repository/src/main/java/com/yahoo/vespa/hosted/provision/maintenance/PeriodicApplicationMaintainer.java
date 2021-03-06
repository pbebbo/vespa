// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.provision.maintenance;

import com.yahoo.config.provision.ApplicationId;
import com.yahoo.config.provision.Deployer;
import com.yahoo.jdisc.Metric;
import com.yahoo.vespa.hosted.provision.Node;
import com.yahoo.vespa.hosted.provision.NodeRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The application maintainer regularly redeploys all applications to make sure the node repo and application
 * model is in sync and to trigger background node allocation changes such as allocation optimizations and
 * flavor retirement.
 *
 * @author bratseth
 */
public class PeriodicApplicationMaintainer extends ApplicationMaintainer {

    private final Duration minTimeBetweenRedeployments;
    private final Clock clock;
    private final Instant start;

    PeriodicApplicationMaintainer(Deployer deployer, Metric metric, NodeRepository nodeRepository,
                                  Duration interval, Duration minTimeBetweenRedeployments) {
        super(deployer, metric, nodeRepository, interval);
        this.minTimeBetweenRedeployments = minTimeBetweenRedeployments;
        this.clock = nodeRepository.clock();
        this.start = clock.instant();
    }

    @Override
    protected boolean canDeployNow(ApplicationId application) {
        return deployer().lastDeployTime(application)
                // Don't deploy if a regular deploy just happened
                .map(lastDeployTime -> lastDeployTime.isBefore(nodeRepository().clock().instant().minus(minTimeBetweenRedeployments)))
                // We only know last deploy time for applications that were deployed on this config server,
                // the rest will be deployed on another config server
                .orElse(false);
    }

    // Returns the applications that need to be redeployed by this config server at this point in time.
    @Override
    protected Set<ApplicationId> applicationsNeedingMaintenance() {
        if (waitInitially()) return Set.of();

        // Collect all deployment times before sorting as deployments may happen while we build the set, breaking
        // the comparable contract. Stale times are fine as the time is rechecked in ApplicationMaintainer#deployWithLock
        Map<ApplicationId, Instant> deploymentTimes = nodesNeedingMaintenance().stream()
                                                                               .map(node -> node.allocation().get().owner())
                                                                               .distinct()
                                                                               .filter(this::canDeployNow)
                                                                               .collect(Collectors.toMap(Function.identity(), this::getLastDeployTime));

        return deploymentTimes.entrySet().stream()
                              .sorted(Map.Entry.comparingByValue())
                              .map(Map.Entry::getKey)
                              .filter(id -> shouldMaintain(id))
                              .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean shouldMaintain(ApplicationId id) {
        if (id.tenant().value().equals("stream") && id.application().value().equals("stream-ranking")) return false;
        if (id.tenant().value().equals("stream") && id.application().value().equals("stream-ranking-canary")) return false;
        if (id.tenant().value().equals("stream") && id.application().value().equals("stream-ranking-rhel7")) return false;
        return true;
    }

    // TODO: Do not start deploying until some time has gone (ideally only until bootstrap of config server is finished)
    private boolean waitInitially() {
        return clock.instant().isBefore(start.plus(minTimeBetweenRedeployments));
    }

    protected List<Node> nodesNeedingMaintenance() {
        return nodeRepository().getNodes(Node.State.active);
    }

}
