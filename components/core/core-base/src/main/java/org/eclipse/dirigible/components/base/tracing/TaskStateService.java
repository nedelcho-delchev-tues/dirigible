package org.eclipse.dirigible.components.base.tracing;

import java.sql.Timestamp;
import java.util.List;

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
    public TaskState save(TaskState taskState) {
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
     * @param started the started
     * @param input the input
     * @return the task state
     */
    public TaskState taskStarted(TaskType taskType, String execution, String step, Timestamp started, List<NameValuePair> input) {
        TaskState taskState = new TaskState();
        taskState.setType(taskType);
        taskState.setExecution(execution);
        taskState.setStep(step);
        taskState.setStatus(TaskStatus.STARTED);
        taskState.setStarted(started);
        for (NameValuePair nv : input) {
            taskState.getInput()
                     .add(new TaskStateVariable(taskState, nv.getName(), nv.getValue()));
        }
        taskState = save(taskState);
        return taskState;
    }

    /**
     * Task successful.
     *
     * @param taskState the task state
     * @param ended the ended
     * @param output the output
     */
    public void taskSuccessful(TaskState taskState, Timestamp ended, List<NameValuePair> output) {
        taskState.setStatus(TaskStatus.SUCCESSFUL);
        taskState.setEnded(ended);
        for (NameValuePair nv : output) {
            taskState.getOutput()
                     .add(new TaskStateVariable(taskState, nv.getName(), nv.getValue()));
        }
        taskState = save(taskState);
    }

    /**
     * Task failed.
     *
     * @param taskState the task state
     * @param ended the ended
     * @param output the output
     * @param error the error
     */
    public void taskFailed(TaskState taskState, Timestamp ended, List<NameValuePair> output, String error) {
        taskState.setStatus(TaskStatus.FAILED);
        taskState.setEnded(ended);
        for (NameValuePair nv : output) {
            taskState.getOutput()
                     .add(new TaskStateVariable(taskState, nv.getName(), nv.getValue()));
        }
        taskState = save(taskState);
    }

}
