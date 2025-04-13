package org.eclipse.dirigible.components.base.tracing;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.dirigible.commons.api.helpers.NameValuePair;
import org.eclipse.dirigible.components.base.artefact.ArtefactRepository;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class TaskStateService.
 */
@Service
@Transactional
public class TaskStateService {

    /** The repository. */
    private TaskStateRepository repository;

    /**
     * Instantiates a new task state service.
     *
     * @param repository the repository
     */
    public TaskStateService(TaskStateRepository repository) {
        this.repository = repository;
    }

    /**
     * Gets the repository.
     *
     * @return the repository
     */
    protected TaskStateRepository getRepository() {
        return repository;
    }

    /**
     * Gets the all.
     *
     * @return the all
     */
    public List<TaskState> getAll() {
        return getRepository().findAll();
    }

    /**
     * Save.
     *
     * @param taskState the taskState
     * @return the taskState
     */
    private TaskState save(TaskState taskState) {
        return getRepository().saveAndFlush(taskState);
    }

    /**
     * Delete.
     *
     * @param taskState the taskState
     */
    public void delete(TaskState taskState) {
        getRepository().delete(taskState);
    }
    
    /**
     * Delete all.
     */
    public void deleteAll() {
        getRepository().deleteAll();
    }

    /**
     * Find by id.
     *
     * @param id the id
     * @return the taskState
     */
    public TaskState findById(Long id) {
        return getRepository().findById(id)
                              .orElseThrow(() -> new IllegalArgumentException(this.getClass() + ": missing task state with [" + id + "]"));
    }

    /**
     * Find by name.
     *
     * @param execution the execution
     * @return the taskState
     */
    public List<TaskState> findByExecution(String execution) {
        TaskState example = new TaskState();
        example.setExecution(execution);
        return getRepository().findAll(Example.of(example));
    }

    /**
     * Task started.
     *
     * @param taskType the task type
     * @param execution the execution
     * @param step the step
     * @param input the input
     * @return the task state
     */
    public TaskState taskStarted(TaskType taskType, String execution, String step, Map<String, String> input) {
        TaskState taskState = new TaskState();
        taskState.setType(taskType);
        taskState.setExecution(execution);
        taskState.setStep(step);
        taskState.setStatus(TaskStatus.STARTED);
        taskState.setStarted(Timestamp.from(Instant.now()));
        taskState.getInput().putAll(input);
        taskState = save(taskState);
        return taskState;
    }

    /**
     * Task successful.
     *
     * @param taskState the task state
     * @param output the output
     */
    public void taskSuccessful(TaskState taskState, Map<String, String> output) {
        taskState.setStatus(TaskStatus.SUCCESSFUL);
        taskState.setEnded(Timestamp.from(Instant.now()));
        taskState.getOutput().putAll(output);
        taskState = save(taskState);
    }

    /**
     * Task failed.
     *
     * @param taskState the task state
     * @param output the output
     * @param error the error
     */
    public void taskFailed(TaskState taskState, Map<String, String> output, String error) {
        taskState.setStatus(TaskStatus.FAILED);
        taskState.setEnded(Timestamp.from(Instant.now()));
        taskState.getOutput().putAll(output);
        taskState.setError(error);
        taskState = save(taskState);
    }

}
