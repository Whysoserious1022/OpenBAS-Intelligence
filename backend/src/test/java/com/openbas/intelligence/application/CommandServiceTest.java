package com.openbas.intelligence.application;

import com.openbas.intelligence.api.dto.CommandDto;
import com.openbas.intelligence.api.exception.BusinessValidationException;
import com.openbas.intelligence.api.exception.ResourceNotFoundException;
import com.openbas.intelligence.application.service.CommandService;
import com.openbas.intelligence.infrastructure.persistence.entity.CommandEntity;
import com.openbas.intelligence.infrastructure.persistence.repository.CommandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommandService Unit Tests")
class CommandServiceTest {

    @Mock private CommandRepository commandRepository;
    private CommandService commandService;

    @BeforeEach
    void setUp() {
        commandService = new CommandService(commandRepository);
    }

    @Test
    @DisplayName("approveCommand transitions PENDING_APPROVAL to APPROVED")
    void approveCommand_pendingApproval_transitionsToApproved() {
        UUID id = UUID.randomUUID();
        CommandEntity cmd = createCommand(id, "PENDING_APPROVAL");
        cmd.setExpiresAt(Instant.now().plusSeconds(3600));
        when(commandRepository.findById(id)).thenReturn(Optional.of(cmd));
        when(commandRepository.save(any())).thenReturn(cmd);

        CommandDto result = commandService.approveCommand(id, "operator1");

        verify(commandRepository).save(argThat(c ->
            "APPROVED".equals(c.getStatus()) && "operator1".equals(c.getApprovedBy())));
    }

    @Test
    @DisplayName("approveCommand throws when command not in PENDING_APPROVAL")
    void approveCommand_wrongStatus_throws() {
        UUID id = UUID.randomUUID();
        CommandEntity cmd = createCommand(id, "PROPOSED");
        when(commandRepository.findById(id)).thenReturn(Optional.of(cmd));

        assertThatThrownBy(() -> commandService.approveCommand(id, "operator1"))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("PENDING_APPROVAL");
    }

    @Test
    @DisplayName("approveCommand throws when command is expired")
    void approveCommand_expired_throws() {
        UUID id = UUID.randomUUID();
        CommandEntity cmd = createCommand(id, "PENDING_APPROVAL");
        cmd.setExpiresAt(Instant.now().minusSeconds(60)); // Already expired
        when(commandRepository.findById(id)).thenReturn(Optional.of(cmd));

        assertThatThrownBy(() -> commandService.approveCommand(id, "operator1"))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("rejectCommand records rejection details")
    void rejectCommand_recordsRejectionDetails() {
        UUID id = UUID.randomUUID();
        CommandEntity cmd = createCommand(id, "PENDING_APPROVAL");
        when(commandRepository.findById(id)).thenReturn(Optional.of(cmd));
        when(commandRepository.save(any())).thenReturn(cmd);

        commandService.rejectCommand(id, "Risk too high", "supervisor1");

        verify(commandRepository).save(argThat(c ->
            "REJECTED".equals(c.getStatus()) &&
            "supervisor1".equals(c.getRejectedBy()) &&
            "Risk too high".equals(c.getRejectionReason())));
    }

    @Test
    @DisplayName("cancelCommand rejects EXECUTING status")
    void cancelCommand_executing_throws() {
        UUID id = UUID.randomUUID();
        CommandEntity cmd = createCommand(id, "EXECUTING");
        when(commandRepository.findById(id)).thenReturn(Optional.of(cmd));

        assertThatThrownBy(() -> commandService.cancelCommand(id, "operator1"))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("executing");
    }

    @Test
    @DisplayName("getCommandById throws when not found")
    void getCommandById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(commandRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.getCommandById(id))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    private CommandEntity createCommand(UUID id, String status) {
        CommandEntity cmd = new CommandEntity();
        cmd.setId(id);
        cmd.setStatus(status);
        cmd.setIdempotencyKey(UUID.randomUUID().toString());
        cmd.setTargetEquipmentId("AHU-01");
        cmd.setTargetPointId("SUPPLY_AIR_TEMP_SETPOINT");
        cmd.setBuildingId("BUILDING-01");
        cmd.setCommandType("SET_SETPOINT");
        cmd.setProposedValue(14.0);
        cmd.setReason("Test command");
        cmd.setProposedBy("test-agent");
        return cmd;
    }
}
