package local.aidev.config;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.WorkerFactory;
import local.aidev.activity.DevelopmentActivities;
import local.aidev.workflow.DevelopmentWorkflow;
import local.aidev.workflow.DevelopmentWorkflowImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

@Configuration
public class TemporalConfiguration {
    @Bean(destroyMethod = "shutdown")
    WorkflowServiceStubs temporalService(@Value("${app.temporal.address}") String address) {
        return WorkflowServiceStubs.newServiceStubs(WorkflowServiceStubsOptions.newBuilder().setTarget(address).build());
    }

    @Bean
    WorkflowClient workflowClient(WorkflowServiceStubs service) {
        return WorkflowClient.newInstance(service);
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    WorkerFactory workerFactory(WorkflowClient client, DevelopmentActivities activities,
                                @Value("${app.temporal.task-queue}") String queue, @Value("${app.max-revisions}") int maxRevisions) {
        WorkerFactory factory = WorkerFactory.newInstance(client);
        var worker = factory.newWorker(queue);
        worker.registerWorkflowImplementationFactory(DevelopmentWorkflow.class, () -> new DevelopmentWorkflowImpl(maxRevisions));
        worker.registerActivitiesImplementations(activities);
        return factory;
    }
}
