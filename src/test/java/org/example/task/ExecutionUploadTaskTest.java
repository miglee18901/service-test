package org.example.task;

import org.example.model.ExecutionInfo;
import org.example.repository.ExecutionInfoRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExecutionUploadTaskTest {
    @Test
    void shouldCompleteExistingExecution() {
        ExecutionInfo info = execution(1L);
        ExecutionInfoRepository repository = mock(ExecutionInfoRepository.class);
        when(repository.findById(1L)).thenReturn(Optional.of(info));

        new ExecutionUploadTask(info, repository, 10, 100, 0).run();

        assertEquals(1, info.getCurrentState());
        assertNotNull(info.getCompletedTime());
        verify(repository).save(info);
    }

    @Test
    void shouldUseZeroStateWhenThresholdIsNotPositive() {
        ExecutionInfo info = execution(1L);
        ExecutionInfoRepository repository = mock(ExecutionInfoRepository.class);
        when(repository.findById(1L)).thenReturn(Optional.of(info));
        new ExecutionUploadTask(info, repository, 0, 0, -1).run();
        assertEquals(0, info.getCurrentState());
    }

    @Test
    void shouldDoNothingWhenExecutionWasDeleted() {
        ExecutionInfo info = execution(1L);
        ExecutionInfoRepository repository = mock(ExecutionInfoRepository.class);
        when(repository.findById(1L)).thenReturn(Optional.empty());
        new ExecutionUploadTask(info, repository, 10, 0, 0).run();
        verify(repository, never()).save(any());
    }

    @Test
    void shouldRestoreInterruptFlagWhenSleepIsInterrupted() {
        ExecutionInfo info = execution(1L);
        ExecutionInfoRepository repository = mock(ExecutionInfoRepository.class);
        Thread.currentThread().interrupt();
        try {
            new ExecutionUploadTask(info, repository, 10, 100, 1).run();
            assertTrue(Thread.currentThread().isInterrupted());
            verifyNoInteractions(repository);
        } finally {
            Thread.interrupted();
        }
    }

    private ExecutionInfo execution(Long id) {
        ExecutionInfo info = new ExecutionInfo();
        info.setId(id);
        return info;
    }
}