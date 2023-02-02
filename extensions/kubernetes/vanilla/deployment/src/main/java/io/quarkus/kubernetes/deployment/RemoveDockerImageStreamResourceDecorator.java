package io.quarkus.kubernetes.deployment;

import io.dekorate.kubernetes.decorator.Decorator;
import io.dekorate.kubernetes.decorator.ResourceProvidingDecorator;
import io.dekorate.s2i.decorator.AddDockerImageStreamResourceDecorator;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.openshift.api.model.ImageStream;

/**
 * Workaround for https://github.com/dekorateio/dekorate/issues/1148: Dekorate is always adding an image stream if we're
 * using a docker image, so we need to remove it if we're using a Deployment resource.
 */
public class RemoveDockerImageStreamResourceDecorator extends ResourceProvidingDecorator<KubernetesListBuilder> {

    private final String name;

    public RemoveDockerImageStreamResourceDecorator(String name) {
        this.name = name;
    }

    public void visit(KubernetesListBuilder list) {
        list.getItems().stream()
                .filter(i -> i.getMetadata().getName().equals(name) && i instanceof ImageStream)
                .map(ImageStream.class::cast)
                .findFirst()
                .ifPresent(list::removeFromItems);
    }

    @Override
    public Class<? extends Decorator>[] after() {
        return new Class[] { AddDockerImageStreamResourceDecorator.class };
    }
}