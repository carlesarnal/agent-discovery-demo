package com.carlesarnal.agents.translator;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskState;
import io.a2a.spec.TextPart;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class TranslatorAgentExecutorProducer {

    @Inject
    private TranslatorAgent translatorAgent;

    @Produces
    public AgentExecutor agentExecutor() {
        return new TranslatorAgentExecutor(translatorAgent);
    }

    private static class TranslatorAgentExecutor implements AgentExecutor {

        private final TranslatorAgent agent;

        TranslatorAgentExecutor(TranslatorAgent agent) {
            this.agent = agent;
        }

        @Override
        public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
            TaskUpdater updater = new TaskUpdater(context, eventQueue);

            if (context.getTask() == null) {
                updater.submit();
            }
            updater.startWork();

            String userMessage = extractText(context.getMessage());
            String translation = agent.translate(userMessage);

            TextPart responsePart = new TextPart(translation, null);
            updater.addArtifact(List.of(responsePart), null, null, null);
            updater.complete();
        }

        private String extractText(Message message) {
            StringBuilder sb = new StringBuilder();
            if (message.getParts() != null) {
                for (Part<?> part : message.getParts()) {
                    if (part instanceof TextPart textPart) {
                        sb.append(textPart.getText());
                    }
                }
            }
            return sb.toString();
        }

        @Override
        public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
            Task task = context.getTask();
            if (task.getStatus().state() == TaskState.CANCELED
                    || task.getStatus().state() == TaskState.COMPLETED) {
                throw new TaskNotCancelableError();
            }
            new TaskUpdater(context, eventQueue).cancel();
        }
    }
}
