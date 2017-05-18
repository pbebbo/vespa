package com.yahoo.vespa.model.admin.clustercontroller;

import com.yahoo.component.ComponentSpecification;
import com.yahoo.vespa.model.container.Container;
import com.yahoo.vespa.model.container.ContainerCluster;
import com.yahoo.vespa.model.container.ContainerClusterVerifier;
import com.yahoo.vespa.model.container.component.Component;
import com.yahoo.vespa.model.container.component.SimpleComponent;

import java.util.Collections;
import java.util.Set;

/**
 * @author baldersheim
 * Verifies that all containers added are ClusterControllerContainers and that filters away Linguistics components.
 */
public class VerifyClusterControllerCluster implements ContainerClusterVerifier {
    static final Set<ComponentSpecification> unwantedComponents = Collections.singleton(new SimpleComponent(ContainerCluster.SIMPLE_LINGUISTICS_PROVIDER).getClassId());
    @Override
    public boolean acceptComponent(Component component) {
        return ! unwantedComponents.contains(component.getClassId());
    }

    @Override
    public boolean acceptContainer(Container container) {
        return container instanceof ClusterControllerContainer;
    }
}
