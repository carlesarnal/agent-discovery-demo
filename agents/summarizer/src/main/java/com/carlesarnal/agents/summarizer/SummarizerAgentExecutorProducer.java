package com.carlesarnal.agents.summarizer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.Part;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskNotCancelableError;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TextPart;

import java.util.List;

@ApplicationScoped
public class SummarizerAgentExecutorProducer {

    @Inject
    private SummarizerAgent summarizerAgent;

    @Produces
    public AgentExecutor agentExecutor() {
        return new SummarizerAgentExecutor(summarizerAgent);
    }

    private static class SummarizerAgentExecutor implements AgentExecutor {

        private final SummarizerAgent agent;

        SummarizerAgentExecutor(SummarizerAgent agent) {
            this.agent = agent;
        }

        @Override
        public void execute(RequestContext context, AgentEmitter emitter) throws A2AError {
            if (context.getTask() == null) {
                emitter.submit();
            }
            emitter.startWork();

            String userMessage = context.getUserInput();
            String summary = agent.summarize(userMessage);

            Part<?> responsePart = new TextPart(summary);
            emitter.addArtifact(List.of(responsePart), null, null, null);
            emitter.complete();
        }

        @Override
        public void cancel(RequestContext context, AgentEmitter emitter) throws A2AError {
            Task task = context.getTask();
            if (task != null && (task.status().state() == TaskState.TASK_STATE_CANCELED
                    || task.status().state() == TaskState.TASK_STATE_COMPLETED)) {
                throw new TaskNotCancelableError();
            }
            emitter.cancel();
        }
    }
}
